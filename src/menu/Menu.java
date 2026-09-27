package menu;

import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDate;

import entidades.Jogo;
import entidades.Publicadora;
import persistencia.Arquivo;

public class Menu {

    private Scanner scanner;
    private Arquivo<Jogo> arquivoJogos;
    private Arquivo<Publicadora> arquivoPublicadoras;
    private persistencia.ArvoreBMais arvorePublicadoraJogo;

    public Menu() throws Exception {
        scanner = new Scanner(System.in);

        arquivoJogos = new Arquivo<>(
                "jogos",
                Jogo.class.getConstructor());

        arquivoPublicadoras = new Arquivo<>(
                "publicadoras",
                Publicadora.class.getConstructor());
        arvorePublicadoraJogo = new persistencia.ArvoreBMais(
                5, 
                "./dados/indices/arvore_pub_jogo.db");
    }

    public void executar() throws Exception {
        int opcao;

        do {
            System.out.println("\n========== CATALOGO DE JOGOS ==========");
            System.out.println();
            System.out.println("1 - Jogos");
            System.out.println("2 - Publicadoras");
            System.out.println("0 - Sair");
            System.out.print("\nEscolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    menuJogos();
                    break;

                case 2:
                    menuPublicadoras();
                    break;

                case 0:
                    System.out.println("Encerrando...");
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);

        arquivoJogos.close();
        arquivoPublicadoras.close();
        scanner.close();
    }

    // =========================================================
    // MENU DE JOGOS
    // =========================================================

    private void menuJogos() throws Exception {
        int opcao;

        do {
            System.out.println("\n========== JOGOS ==========");
            System.out.println();
            System.out.println("1 - Cadastrar jogo");
            System.out.println("2 - Consultar jogo");
            System.out.println("3 - Listar jogos");
            System.out.println("4 - Atualizar jogo");
            System.out.println("5 - Excluir jogo");
            System.out.println("6 - Listar jogos da publicadora");
            System.out.println("7 - Ordenar jogos alfabeticamente");
            System.out.println("0 - Voltar");
            System.out.print("\nEscolha: ");

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
                
                case 6:
                    listarJogosDaPublicadora();
                    break;
                    
                
                case 7:
                    ordenarJogos();
                    break;

                case 0:
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);
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

        Publicadora publicadora = arquivoPublicadoras.read(publicadoraId);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
            return;
        }

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
                idiomas);

        long id = arquivoJogos.create(jogo);

        arvorePublicadoraJogo.create(publicadoraId, id);

        System.out.println("\nJogo cadastrado com ID: " + id);
    }

    private void consultarJogo() throws Exception {
        System.out.println("\n========== CONSULTAR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = arquivoJogos.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
        } else {
            mostrarJogo(jogo);
        }
    }

    private void listarJogos() throws Exception {
        System.out.println("\n========== LISTAR JOGOS ==========");

        ArrayList<Jogo> jogos = arquivoJogos.readAll();

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

        Jogo jogo = arquivoJogos.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        System.out.println("\nDados atuais:");
        mostrarJogo(jogo);

        System.out.println("\nDigite os novos dados:");

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

        // Verifica se a publicadora existe
        Publicadora publicadora = arquivoPublicadoras.read(publicadoraId);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
            return;
        }

        System.out.print("Quantidade de idiomas: ");
        int quantidadeIdiomas = scanner.nextInt();
        scanner.nextLine();

        String[] idiomas = new String[quantidadeIdiomas];

        for (int i = 0; i < quantidadeIdiomas; i++) {
            System.out.print(
                    "Idioma " + (i + 1) + ": ");

            idiomas[i] = scanner.nextLine();
        }

        Jogo novoJogo = new Jogo(
                id, // mantém o ID!
                nome,
                preco,
                dataLancamento,
                publicadoraId,
                idiomas);

        if (arquivoJogos.update(novoJogo)) {
            System.out.println(
                    "Jogo atualizado com sucesso!");
        } else {
            System.out.println(
                    "Nao foi possivel atualizar o jogo.");
        }
    }

    private void excluirJogo() throws Exception {
        System.out.println("\n========== EXCLUIR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = arquivoJogos.read(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        mostrarJogo(jogo);

        System.out.print("\nTem certeza que deseja excluir? (S/N): ");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            if (arquivoJogos.delete(id)) {
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

    private void ordenarJogos() {
        try {
            System.out.println("\n========== ORDENACAO EXTERNA ==========");
            System.out.println("A iniciar o processo de intercalacao balanceada...");
            
            // Simula uma RAM de 3 registos para forçar a criação de blocos
            persistencia.OrdenacaoExterna ordenacao = new persistencia.OrdenacaoExterna("./dados/jogos/jogos.db", 3);
            
            ordenacao.ordenarPorNome();
            ordenacao.intercalar();
            ordenacao.finalizar();
            
            System.out.println("Sucesso: Base de dados ordenada e indices reconstruidos!");
        } catch (Exception e) {
            System.out.println("Erro ao ordenar ficheiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // MENU DE PUBLICADORAS
    // =========================================================

    private void menuPublicadoras() throws Exception {
        int opcao;

        do {
            System.out.println("\n========== PUBLICADORAS ==========");
            System.out.println();
            System.out.println("1 - Cadastrar publicadora");
            System.out.println("2 - Consultar publicadora");
            System.out.println("3 - Listar publicadoras");
            System.out.println("4 - Atualizar publicadora");
            System.out.println("5 - Excluir publicadora");
            System.out.println("6 - Listar jogos da publicadora");
            System.out.println("0 - Voltar");
            System.out.print("\nEscolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    cadastrarPublicadora();
                    break;

                case 2:
                    consultarPublicadora();
                    break;

                case 3:
                    listarPublicadoras();
                    break;

                case 4:
                    atualizarPublicadora();
                    break;

                case 5:
                    excluirPublicadora();
                    break;
                
                case 6:
                    listarJogosDaPublicadora();
                    break;

                case 0:
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);
    }

    private void cadastrarPublicadora() throws Exception {
        System.out.println("\n========== CADASTRAR PUBLICADORA ==========");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Pais: ");
        String pais = scanner.nextLine();

        System.out.print("Ano de fundacao: ");
        int anoFundacao = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Descricao: ");
        String descricao = scanner.nextLine();

        Publicadora publicadora = new Publicadora(
                -1,
                nome,
                pais,
                anoFundacao,
                descricao);

        long id = arquivoPublicadoras.create(publicadora);

        System.out.println("\nPublicadora cadastrada com ID: " + id);
    }

    private void consultarPublicadora() throws Exception {
        System.out.println("\n========== CONSULTAR PUBLICADORA ==========");

        System.out.print("ID da publicadora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Publicadora publicadora = arquivoPublicadoras.read(id);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
        } else {
            mostrarPublicadora(publicadora);
        }
    }

    private void listarPublicadoras() throws Exception {
        System.out.println("\n========== LISTAR PUBLICADORAS ==========");

        ArrayList<Publicadora> publicadoras = arquivoPublicadoras.readAll();

        if (publicadoras.isEmpty()) {
            System.out.println("Nenhuma publicadora cadastrada.");
            return;
        }

        for (Publicadora publicadora : publicadoras) {
            mostrarPublicadora(publicadora);
        }
    }

    private void atualizarPublicadora() throws Exception {
        System.out.println("\n========== ATUALIZAR PUBLICADORA ==========");

        System.out.print("ID da publicadora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Publicadora publicadora = arquivoPublicadoras.read(id);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
            return;
        }

        System.out.println("\nDados atuais:");
        mostrarPublicadora(publicadora);

        System.out.println("\nDigite os novos dados:");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Pais: ");
        String pais = scanner.nextLine();

        System.out.print("Ano de fundacao: ");
        int anoFundacao = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Descricao: ");
        String descricao = scanner.nextLine();

        Publicadora novaPublicadora = new Publicadora(
                id,
                nome,
                pais,
                anoFundacao,
                descricao);

        if (arquivoPublicadoras.update(novaPublicadora)) {
            System.out.println("Publicadora atualizada com sucesso!");
        } else {
            System.out.println("Nao foi possivel atualizar a publicadora.");
        }
    }

    private void excluirPublicadora() throws Exception {
        System.out.println("\n========== EXCLUIR PUBLICADORA ==========");

        System.out.print("ID da publicadora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Publicadora publicadora = arquivoPublicadoras.read(id);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
            return;
        }

        if (publicadoraPossuiJogos(id)) {
            System.out.println(
                    "Nao e possivel excluir esta publicadora, pois existem jogos associados a ela.");
            return;
        }

        mostrarPublicadora(publicadora);

        System.out.print("\nTem certeza que deseja excluir? (S/N): ");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            if (arquivoPublicadoras.delete(id)) {
                System.out.println("Publicadora excluida com sucesso!");
            } else {
                System.out.println("Nao foi possivel excluir a publicadora.");
            }
        } else {
            System.out.println("Operacao cancelada.");
        }
    }

    private void mostrarPublicadora(Publicadora publicadora) {
        System.out.println("\n---------- PUBLICADORA ----------");
        System.out.println("ID: " + publicadora.getId());
        System.out.println("Nome: " + publicadora.getNome());
        System.out.println("Pais: " + publicadora.getPais());
        System.out.println("Ano de fundacao: " + publicadora.getAnoFundacao());
        System.out.println("Descricao: " + publicadora.getDescricao());
        System.out.println("---------------------------------");
    }

    private void listarJogosDaPublicadora() throws Exception {
        System.out.println("\n========== JOGOS DA PUBLICADORA ==========");

        System.out.print("ID da publicadora: ");
        long publicadoraId = scanner.nextLong();
        scanner.nextLine();

        Publicadora publicadora = arquivoPublicadoras.read(publicadoraId);

        if (publicadora == null) {
            System.out.println("Publicadora nao encontrada.");
            return;
        }

        // Lê a lista de IDs de jogos associados a esta publicadora através da Árvore B+
        ArrayList<Long> idsJogos = arvorePublicadoraJogo.read(publicadoraId);

        if (idsJogos.isEmpty()) {
            System.out.println("Nenhum jogo encontrado para a publicadora " + publicadora.getNome() + ".");
            return;
        }

        System.out.println("\nJogos encontrados (" + idsJogos.size() + "):");
        for (long idJogo : idsJogos) {
            // Busca o objeto Jogo completo usando o Hash Extensível
            Jogo jogo = arquivoJogos.read(idJogo);
            if (jogo != null) {
                mostrarJogo(jogo);
            }
        }
    }

    private boolean publicadoraPossuiJogos(long publicadoraId) throws Exception {
        ArrayList<Jogo> jogos = arquivoJogos.readAll();

        for (Jogo jogo : jogos) {
            if (jogo.getPublicadoraId() == publicadoraId) {
                return true;
            }
        }

        return false;
    }
}