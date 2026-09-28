import entidades.Jogo;
import entidades.Publicadora;
import persistencia.ArvoreBMais;
import persistencia.HashExtensivel;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sem dependências externas. Executar exclusivamente pelo run-tests.sh. */
public class TestSuite {
    private static int passed, failed;
    private static final String[] NAMES = {
        "Zulu", "alpha", "Mike", "Bravo", "yankee", "Delta", "echo", "Charlie",
        "Oscar", "Foxtrot", "November", "Golf", "Lima", "Hotel", "Kilo", "India"
    };
    private static final Pattern GAME = Pattern.compile(
        "---------- JOGO ----------\\R(ID: .*?)--------------------------", Pattern.DOTALL);

    @FunctionalInterface interface Test { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        test("Unidade: serialização completa de Jogo", TestSuite::jogoRoundTrip);
        test("Unidade: serialização completa de Publicadora", TestSuite::publicadoraRoundTrip);
        test("Unidade: Hash com divisões, update, delete e reabertura", TestSuite::hash);
        test("Unidade: B+ com múltiplas folhas, delete e reabertura", TestSuite::arvore);
        test("01 Persistência básica", () -> {
            Path dir = fixture();
            List<Jogo> jogos = jogos(dir);
            eq(16, jogos.size(), "quantidade persistida");
            for (int i = 0; i < NAMES.length; i++) checkGame(jogos.get(i), i + 1, NAMES[i], publisher(i), 10, new String[]{"pt-BR"});
            for (long id = 1; id <= 3; id++) {
                String out = app(dir, "2\n2\n" + id + "\n0\n0\n");
                contains(out, "Nome: Editora " + id + "\n");
                contains(out, "Pais: Brasil\n");
                contains(out, "Ano de fundacao: 2000\n");
                contains(out, "Descricao: Descricao " + id + "\n");
            }
            eq(16, blocks(app(dir, "1\n3\n0\n0\n")).size(), "listagem após reiniciar");
        });
        test("02 Hash de Jogo", () -> {
            Path dir = fixture();
            for (int i = 0; i < NAMES.length; i++) {
                List<String> found = blocks(app(dir, "1\n2\n" + (i + 1) + "\n0\n0\n"));
                eq(1, found.size(), "consulta por ID");
                contains(found.get(0), "Nome: " + NAMES[i] + "\n");
                contains(found.get(0), "ID: " + (i + 1) + "\n");
            }
            contains(app(dir, "1\n2\n999\n0\n0\n"), "Jogo nao encontrado.");
        });
        test("03 Hash de Publicadora", () -> {
            Path dir = fixture();
            for (int i = 1; i <= 3; i++) {
                String out = app(dir, "2\n2\n" + i + "\n0\n0\n");
                contains(out, "ID: " + i + "\nNome: Editora " + i + "\n");
            }
            contains(app(dir, "2\n2\n999\n0\n0\n"), "Publicadora nao encontrada.");
        });
        test("04 B+ 1:N", () -> {
            Path dir = fixture();
            relation(dir, 1, ids(1, 10)); relation(dir, 2, ids(11, 16)); relation(dir, 3, List.of());
        });
        test("05 Update com mesma publicadora (cresce e encolhe)", () -> {
            Path dir = fixture();
            update(dir, 1, "Nome expandido para realocar o registro no arquivo", 1, 25.5f, "pt-BR", "English", "日本語");
            checkGame(read(dir, 1), 1, "Nome expandido para realocar o registro no arquivo", 1, 25.5f, new String[]{"pt-BR", "English", "日本語"});
            relation(dir, 1, ids(1, 10)); relation(dir, 2, ids(11, 16));
            update(dir, 1, "A", 1, 5, "en");
            checkGame(read(dir, 1), 1, "A", 1, 5, new String[]{"en"});
            relation(dir, 1, ids(1, 10)); eq(16, jogos(dir).size(), "sem duplicar registro");
        });
        test("06 Update trocando publicadora", () -> {
            Path dir = fixture(); update(dir, 1, "Transferido", 2, 20, "en");
            checkGame(read(dir, 1), 1, "Transferido", 2, 20, new String[]{"en"});
            relation(dir, 1, ids(2, 10));
            List<Long> expected = ids(11, 16); expected.add(1L); relation(dir, 2, expected);
        });
        test("07 Delete de Jogo", () -> {
            Path dir = fixture(); delete(dir, 1);
            eq(null, read(dir, 1), "Hash remove jogo"); eq(15, jogos(dir).size(), "registro removido");
            contains(app(dir, "1\n2\n1\n0\n0\n"), "Jogo nao encontrado.");
            relation(dir, 1, ids(2, 10)); relation(dir, 2, ids(11, 16));
        });
        test("08 Integridade da FK no cadastro e update", () -> {
            Path dir = fixture();
            contains(app(dir, "1\n1\nInvalido\n10\n2020-01-02\n999\n0\n0\n"), "Publicadora nao encontrada.");
            contains(app(dir, "1\n4\n1\nInvalido\n10\n2020-01-02\n999\n0\n0\n"), "Publicadora nao encontrada.");
            eq(16, jogos(dir).size(), "FK inválida não cria registro");
            checkGame(read(dir, 1), 1, "Zulu", 1, 10, new String[]{"pt-BR"});
            relation(dir, 1, ids(1, 10)); relation(dir, 999, List.of());
        });
        test("09 Integridade da Publicadora", () -> {
            Path dir = fixture();
            contains(app(dir, "2\n5\n1\n0\n0\n"), "Nao e possivel excluir esta publicadora");
            contains(app(dir, "2\n2\n1\n0\n0\n"), "Nome: Editora 1");
            relation(dir, 1, ids(1, 10));
            contains(app(dir, "2\n5\n3\nS\n0\n0\n"), "Publicadora excluida com sucesso!");
            contains(app(dir, "2\n2\n3\n0\n0\n"), "Publicadora nao encontrada.");
        });
        test("10 Ordenação externa com múltiplas intercalações", () -> {
            Path dir = fixture(); delete(dir, 16); update(dir, 2, "ALPHA expandido", 1, 40, "en");
            List<Jogo> before = jogos(dir); sort(dir);
            List<Jogo> after = jogos(dir); eq(15, after.size(), "ordenação ignora lápides");
            List<String> expected = new ArrayList<>();
            for (Jogo j : before) expected.add(j.getNome());
            expected.sort(String.CASE_INSENSITIVE_ORDER);
            List<String> actual = new ArrayList<>(); for (Jogo j : after) actual.add(j.getNome());
            eq(expected, actual, "ordem física alfabética");
            eq(expected, names(app(dir, "1\n3\n0\n0\n")), "ordem exibida");
        });
        test("11 Hash após ordenação", () -> {
            Path dir = fixture(); sort(dir);
            for (int i = 0; i < NAMES.length; i++) checkGame(read(dir, i + 1), i + 1, NAMES[i], publisher(i), 10, new String[]{"pt-BR"});
        });
        test("12 Persistência final: alterações, exclusão, ordenação e reinício", () -> {
            Path dir = fixture(); update(dir, 1, "Transferido", 2, 33, "en", "pt-BR"); delete(dir, 16); sort(dir);
            app(dir, "0\n");
            checkGame(read(dir, 1), 1, "Transferido", 2, 33, new String[]{"en", "pt-BR"});
            eq(null, read(dir, 16), "exclusão persistida");
            for (int i = 1; i < 15; i++) checkGame(read(dir, i + 1), i + 1, NAMES[i], publisher(i), 10, new String[]{"pt-BR"});
            relation(dir, 1, ids(2, 10)); List<Long> expected = ids(11, 15); expected.add(1L); relation(dir, 2, expected);
            contains(app(dir, "1\n" + createGame("Novo", 1) + "0\n0\n"), "Jogo cadastrado com ID: 17");
            checkGame(read(dir, 17), 17, "Novo", 1, 10, new String[]{"pt-BR"});
            eq(null, read(dir, 16), "ID excluído não reutilizado");
        });
        System.out.printf("%nResultado: %d passaram; %d falharam.%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void test(String name, Test test) {
        try { test.run(); passed++; System.out.println("PASS " + name); }
        catch (Exception | AssertionError error) { failed++; System.err.println("FAIL " + name); error.printStackTrace(); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void eq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": esperado=" + expected + ", obtido=" + actual);
    }
    private static void contains(String output, String text) { check(output.contains(text), "Texto ausente: " + text + "\nSaída:\n" + output); }
    private static Jogo game(long id, String name, long pub) {
        return new Jogo(id, name, 10, LocalDate.of(2020, 1, 2), pub, new String[]{"pt-BR"});
    }
    private static void checkGame(Jogo j, long id, String name, long pub, float price, String[] languages) {
        check(j != null, "jogo " + id + " deve existir"); eq(id, j.getId(), "ID"); eq(name, j.getNome(), "nome");
        eq(pub, j.getPublicadoraId(), "publicadora"); eq(price, j.getPreco(), "preço");
        eq(LocalDate.of(2020, 1, 2), j.getDataLancamento(), "data");
        eq(Arrays.asList(languages), Arrays.asList(j.getIdiomas()), "idiomas");
    }
    private static void jogoRoundTrip() throws Exception {
        for (String[] languages : new String[][]{ {}, {"Português", "日本語", "English"} }) {
            Jogo original = game(42, "Ação e aventura 日本", 7); original.setIdiomas(languages); original.setPreco(19.99f);
            Jogo copy = new Jogo(); copy.fromByteArray(original.toByteArray());
            checkGame(copy, 42, original.getNome(), 7, 19.99f, languages);
        }
    }
    private static void publicadoraRoundTrip() throws Exception {
        Publicadora original = new Publicadora(8, "Editora 日本", "Brasil", 1999, "Descrição com ação");
        Publicadora copy = new Publicadora(); copy.fromByteArray(original.toByteArray());
        eq(8L, copy.getId(), "ID"); eq(original.getNome(), copy.getNome(), "nome"); eq(original.getPais(), copy.getPais(), "país");
        eq(1999, copy.getAnoFundacao(), "ano"); eq(original.getDescricao(), copy.getDescricao(), "descrição");
    }
    private static void hash() throws Exception {
        HashExtensivel h = new HashExtensivel("unit-hash");
        try {
            eq(-1L, h.read(999), "chave ausente");
            for (long i = 1; i <= 80; i++) h.create(i, 1000 + i);
            for (long i = 1; i <= 80; i++) eq(1000 + i, h.read(i), "divisão de buckets");
            check(h.update(5, 9999), "update existente"); check(h.delete(9), "delete existente");
            check(!h.update(999, 1), "update ausente"); check(!h.delete(9), "delete repetido");
        } finally { h.close(); }
        h = new HashExtensivel("unit-hash");
        try { for (long i = 1; i <= 80; i++) eq(i == 9 ? -1L : i == 5 ? 9999L : 1000 + i, h.read(i), "Hash persistido " + i); }
        finally { h.close(); }
    }
    private static void arvore() throws Exception {
        ArvoreBMais tree = new ArvoreBMais(5, "unit-tree.db");
        try {
            eq(List.of(), tree.read(1), "árvore vazia");
            for (long i = 60; i >= 1; i--) check(tree.create(i % 3 + 1, i), "inserção B+");
            check(tree.delete(2, 1), "delete par existente"); check(!tree.delete(2, 1), "delete par ausente");
        } finally { tree.close(); }
        tree = new ArvoreBMais(5, "unit-tree.db");
        try {
            for (long pub = 1; pub <= 3; pub++) {
                List<Long> expected = new ArrayList<>(); for (long i = 2; i <= 60; i++) if (i % 3 + 1 == pub) expected.add(i);
                eq(expected, tree.read(pub), "folhas e persistência B+ " + pub);
            }
            eq(List.of(), tree.read(999), "publicadora ausente");
        } finally { tree.close(); }
    }
    private static long publisher(int index) { return index < 10 ? 1 : 2; }
    private static List<Long> ids(long from, long to) { List<Long> list = new ArrayList<>(); for (long i = from; i <= to; i++) list.add(i); return list; }
    private static Path fixture() throws Exception {
        Path dir = Files.createTempDirectory(Path.of("."), "scenario-").toAbsolutePath().normalize();
        StringBuilder input = new StringBuilder("2\n");
        for (int i = 1; i <= 3; i++) input.append("1\nEditora ").append(i).append("\nBrasil\n2000\nDescricao ").append(i).append('\n');
        input.append("0\n1\n");
        for (int i = 0; i < NAMES.length; i++) input.append(createGame(NAMES[i], publisher(i)));
        input.append("0\n0\n");
        contains(app(dir, input.toString()), "Jogo cadastrado com ID: 16");
        return dir;
    }
    private static String createGame(String name, long pub) { return "1\n" + name + "\n10\n2020-01-02\n" + pub + "\n1\npt-BR\n"; }
    private static void update(Path dir, long id, String name, long pub, float price, String... languages) throws Exception {
        String input = "1\n4\n" + id + "\n" + name + "\n" + price + "\n2020-01-02\n" + pub + "\n" + languages.length + "\n" + String.join("\n", languages) + "\n0\n0\n";
        contains(app(dir, input), "Jogo atualizado com sucesso!");
    }
    private static void delete(Path dir, long id) throws Exception { contains(app(dir, "1\n5\n" + id + "\nS\n0\n0\n"), "Jogo excluido com sucesso!"); }
    private static void sort(Path dir) throws Exception { contains(app(dir, "1\n7\n0\n0\n"), "Sucesso: base de dados ordenada e Hash reconstruido!"); }
    private static String app(Path dir, String input) throws Exception {
        Path stdin = Files.createTempFile(dir, "stdin-", ".txt"); Path output = Files.createTempFile(dir, "stdout-", ".txt");
        Files.writeString(stdin, input, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Dfile.encoding=UTF-8", "-Duser.language=en", "-Duser.country=US", "-cp", System.getProperty("java.class.path"), "Main")
            .directory(dir.toFile()).redirectInput(stdin.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            if (!p.waitFor(20, TimeUnit.SECONDS)) { p.destroyForcibly(); p.waitFor(); throw new AssertionError("Main excedeu 20s"); }
            String text = Files.readString(output).replace("\r\n", "\n");
            eq(0, p.exitValue(), "Main terminou com erro:\n" + text);
            check(!text.contains("Exception") && !text.contains("Erro ao ordenar"), "erro na aplicação:\n" + text);
            return text;
        } finally { if (p.isAlive()) { p.destroyForcibly(); p.waitFor(); } Files.deleteIfExists(stdin); Files.deleteIfExists(output); }
    }
    private static List<String> blocks(String output) { List<String> result = new ArrayList<>(); Matcher m = GAME.matcher(output); while (m.find()) result.add(m.group(1)); return result; }
    private static List<String> names(String output) {
        List<String> result = new ArrayList<>(); for (String block : blocks(output)) for (String line : block.split("\n")) if (line.startsWith("Nome: ")) result.add(line.substring(6)); return result;
    }
    private static void relation(Path dir, long pub, List<Long> expected) throws Exception {
        ArvoreBMais tree = new ArvoreBMais(5, dir + "/dados/indices/arvore_pub_jogo.db");
        try { List<Long> sorted = new ArrayList<>(expected); Collections.sort(sorted); eq(sorted, tree.read(pub), "pares B+ da publicadora " + pub); }
        finally { tree.close(); }
        String output = app(dir, "1\n6\n" + pub + "\n0\n0\n");
        List<Long> actual = new ArrayList<>();
        for (String block : blocks(output)) {
            actual.add(Long.parseLong(block.substring(4, block.indexOf('\n'))));
            contains(block, "ID da publicadora: " + pub + "\n");
        }
        List<Long> sorted = new ArrayList<>(expected); Collections.sort(sorted); Collections.sort(actual);
        eq(sorted, actual, "listagem exata da publicadora " + pub);
    }
    // Arquivo aceita nomes simples; para inspecionar a base de outro processo usamos
    // um worker no diretório da aplicação, sem alterar user.dir ou os dados do projeto.
    private static List<Jogo> jogos(Path dir) throws Exception {
        Path snapshot = Files.createTempFile(dir, "snapshot-", ".bin");
        Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"), "Snapshot", snapshot.toString()).directory(dir.toFile()).inheritIO().start();
        try {
            if (!p.waitFor(20, TimeUnit.SECONDS)) { p.destroyForcibly(); p.waitFor(); throw new AssertionError("Snapshot excedeu 20s"); }
            eq(0, p.exitValue(), "leitura da base");
            try (java.io.DataInputStream in = new java.io.DataInputStream(Files.newInputStream(snapshot))) {
                List<Jogo> result = new ArrayList<>(); int count = in.readInt();
                for (int i = 0; i < count; i++) { Jogo j = new Jogo(); j.fromByteArray(in.readNBytes(in.readInt())); result.add(j); }
                return result;
            }
        } finally { if (p.isAlive()) { p.destroyForcibly(); p.waitFor(); } Files.deleteIfExists(snapshot); }
    }
    private static Jogo read(Path dir, long id) throws Exception { for (Jogo j : jogos(dir)) if (j.getId() == id) return j; return null; }
}
