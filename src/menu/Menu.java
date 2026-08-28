package menu;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

import entidades.Jogo;
import persistencia.Arquivo;

public class Menu {

    private String menuOpcoes =
        "========== CATALOGO DE JOGOS ==========\r\n" +
        "\r\n" +
        "1 - Cadastrar jogo\r\n" +
        "2 - Consultar jogo\r\n" +
        "3 - Listar jogos\r\n" +
        "4 - Atualizar jogo\r\n" +
        "5 - Excluir jogo\r\n" +
        "0 - Sair\r\n" +
        "\r\n" +
        "Escolha: ";

    private Scanner scanner;
    private Arquivo<Jogo> arquivo;

    public Menu() throws Exception {
        scanner = new Scanner(System.in);
        arquivo = new Arquivo<>(
            "jogos",
            Jogo.class.getConstructor()
        );
    }

    public void executar() throws Exception {
        int opcao;

        do {
            System.out.println(menuOpcoes);
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    cadastrarJogo();
                    break;

                case 2:
                    consultarJogo();
                    break;

                case 3:
                    listarJogos();
                    break;

                case 4:
                    atualizarJogo();
                    break;

                case 5:
                    excluirJogo();
                    break;

                case 0:
                    System.out.println("Encerrando...");
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);

        arquivo.close();
        scanner.close();
    }

    private void cadastrarJogo() throws Exception {
        System.out.println("\n========== CADASTRAR JOGO ==========");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Preco: ");
        float preco = scanner.nextFloat();
        scanner.nextLine();

        System.out.print("Data de lancamento (AAAA-MM-DD): ");
        LocalDate dataLancamento = LocalDate.parse(scanner.nextLine());

        System.out.print("ID da publicadora: ");
        long publicadoraId = scanner.nextLong();
        scanner.nextLine();

        System.out.print("Quantidade de idiomas: ");
        int quantidadeIdiomas = scanner.nextInt();
        scanner.nextLine();

        String[] idiomas = new String[quantidadeIdiomas];

        for (int i = 0; i < quantidadeIdiomas; i++) {
            System.out.print("Idioma " + (i + 1) + ": ");
            idiomas[i] = scanner.nextLine();
        }

        Jogo jogo = new Jogo(
            -1,
            nome,
            preco,
            dataLancamento,
            publicadoraId,
            idiomas
        );

        long id = arquivo.create(jogo);

        System.out.println("\nJogo cadastrado com ID: " + id);
    }

    private void consultarJogo() throws Exception {
        System.out.println("\n========== CONSULTAR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = arquivo.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
        } else {
            mostrarJogo(jogo);
        }
    }

    private void listarJogos() throws Exception {
        System.out.println("\n========== LISTAR JOGOS ==========");

        ArrayList<Jogo> jogos = arquivo.readAll();

        if (jogos.isEmpty()) {
            System.out.println("Nenhum jogo cadastrado.");
            return;
        }

        for (Jogo jogo : jogos) {
            mostrarJogo(jogo);
        }
    }

    private void atualizarJogo() throws Exception {
        System.out.println("\n========== ATUALIZAR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = arquivo.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        System.out.println("Digite os novos dados:");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Preco: ");
        float preco = scanner.nextFloat();
        scanner.nextLine();

        System.out.print("Data de lancamento (AAAA-MM-DD): ");
        LocalDate dataLancamento = LocalDate.parse(scanner.nextLine());

        System.out.print("ID da publicadora: ");
        long publicadoraId = scanner.nextLong();
        scanner.nextLine();

        System.out.print("Quantidade de idiomas: ");
        int quantidadeIdiomas = scanner.nextInt();
        scanner.nextLine();

        String[] idiomas = new String[quantidadeIdiomas];

        for (int i = 0; i < quantidadeIdiomas; i++) {
            System.out.print("Idioma " + (i + 1) + ": ");
            idiomas[i] = scanner.nextLine();
        }

        Jogo novoJogo = new Jogo(
            id,
            nome,
            preco,
            dataLancamento,
            publicadoraId,
            idiomas
        );

        if (arquivo.update(novoJogo)) {
            System.out.println("Jogo atualizado com sucesso!");
        } else {
            System.out.println("Nao foi possivel atualizar o jogo.");
        }
    }

    private void excluirJogo() throws Exception {
        System.out.println("\n========== EXCLUIR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = arquivo.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        System.out.print("Tem certeza que deseja excluir? (S/N): ");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            if (arquivo.delete(id)) {
                System.out.println("Jogo excluido com sucesso!");
            } else {
                System.out.println("Nao foi possivel excluir o jogo.");
            }
        } else {
            System.out.println("Operacao cancelada.");
        }
    }

    private void mostrarJogo(Jogo jogo) {
        System.out.println("\n---------- JOGO ----------");
        System.out.println("ID: " + jogo.getId());
        System.out.println("Nome: " + jogo.getNome());
        System.out.println("Preco: R$ " + jogo.getPreco());
        System.out.println("Data de lancamento: " + jogo.getDataLancamento());
        System.out.println("ID da publicadora: " + jogo.getPublicadoraId());

        System.out.println("Idiomas:");

        String[] idiomas = jogo.getIdiomas();

        for (String idioma : idiomas) {
            System.out.println(" - " + idioma);
        }

        System.out.println("--------------------------");
    }
}