import interfacegrafica.Catalogo;
import entidades.*;
import java.time.LocalDate;

/** Executar em uma pasta temporária: nunca usa o catálogo da raiz. */
public class CatalogoTest {
    private static void verificar(boolean valor, String mensagem) {
        if (!valor) throw new AssertionError(mensagem);
    }
    private interface Operacao { void executar() throws Exception; }
    private static void recusar(Operacao operacao) throws Exception {
        try { operacao.executar(); throw new AssertionError("Operação deveria ser recusada"); }
        catch (IllegalArgumentException | IllegalStateException esperado) { }
    }
    public static void main(String[] args) throws Exception {
        long p, d, j;
        try (Catalogo c = new Catalogo()) {
            p = c.publicadoras.cadastrar(new Publicadora(-1, "Publicadora", "Brasil", 2000, "Descrição"));
            d = c.desenvolvedoras.cadastrar(new Desenvolvedora(-1, "Estúdio", "Brasil", 2001));
            j = c.jogos.cadastrar(new Jogo(-1, "Zulu", 10, LocalDate.of(2024, 1, 1), p, new String[]{"pt"}));
            c.jogos.cadastrar(new Jogo(-1, "Alpha", 20, LocalDate.of(2024, 1, 2), p, new String[]{"en"}));
            verificar(c.relacionamentos.associar(j, d), "Associar entidades reais");
            verificar(!c.relacionamentos.associar(j, d), "Recusar duplicata");
            recusar(() -> c.jogos.excluir(j));
            recusar(() -> c.desenvolvedoras.excluir(d));
            recusar(() -> c.publicadoras.excluir(p));
            recusar(() -> c.relacionamentos.associar(j, 999));
            recusar(() -> c.jogos.cadastrar(new Jogo(-1, "Inválido", 0, LocalDate.now(), 999, new String[0])));
            verificar(c.publicadoras.atualizar(new Publicadora(p, "Publicadora editada", "Brasil", 1999, "Nova descrição")), "Atualizar publicadora");
            verificar(c.desenvolvedoras.atualizar(new Desenvolvedora(d, "Estúdio editado", "Japão", 1998)), "Atualizar desenvolvedora");
            c.ordenar();
            verificar(c.jogos.listar().get(0).getNome().equals("Alpha"), "Ordem física");
            verificar(c.jogos.consultar(j).getNome().equals("Zulu"), "Hash reconstruído");
            verificar(c.relacionamentos.desenvolvedorasDoJogo(j).contains(d), "Serviço reconectado após ordenar");
            verificar(c.relacionamentos.jogosDaDesenvolvedora(d).contains(j), "Consulta inversa");
            verificar(c.jogos.listarPorPublicadora(p).size() == 2, "Consulta 1:N após ordenar");
        }
        try (Catalogo c = new Catalogo()) {
            verificar(c.publicadoras.consultar(p).getNome().equals("Publicadora editada"), "Persistir publicadora");
            verificar(c.desenvolvedoras.consultar(d).getNome().equals("Estúdio editado"), "Persistir desenvolvedora");
            verificar(c.relacionamentos.desassociar(j, d), "Remover associação após reinício");
            verificar(!c.relacionamentos.desassociar(j, d), "Associação ausente");
            verificar(c.relacionamentos.jogosDaDesenvolvedora(d).isEmpty(), "Remover índice inverso");
            Jogo atualizado = c.jogos.consultar(j);
            atualizado.setNome("Jogo editado"); verificar(c.jogos.atualizar(atualizado), "Atualizar jogo");
            verificar(c.jogos.excluir(j), "Excluir jogo sem vínculo");
            for (Jogo outro : c.jogos.listar()) c.jogos.excluir(outro.getId());
            verificar(c.desenvolvedoras.excluir(d), "Excluir desenvolvedora sem vínculo");
            verificar(c.publicadoras.excluir(p), "Excluir publicadora sem jogos");
            verificar(c.jogos.consultar(j) == null && c.desenvolvedoras.consultar(d) == null && c.publicadoras.consultar(p) == null, "Entidades excluídas");
        }
        System.out.println("PASS CatalogoTest: CRUDs, regras, N:N, 1:N, ordenação e reabertura");
    }
}
