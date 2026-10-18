package menu;

import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDate;
import java.io.File;

import entidades.Jogo;
import entidades.Publicadora;
import entidades.Desenvolvedora;
import persistencia.Arquivo;
import persistencia.ArquivoJogoDesenvolvedora;
import persistencia.RelacionamentoJogoDesenvolvedora;
import persistencia.ServicoDesenvolvedora;
import persistencia.ServicoJogo;

public class Menu {

    private Scanner scanner;
    private Arquivo<Jogo> arquivoJogos;
    private Arquivo<Publicadora> arquivoPublicadoras;
    private Arquivo<Desenvolvedora> arquivoDesenvolvedoras;
    private persistencia.ArvoreBMais arvorePublicadoraJogo;
    private ArquivoJogoDesenvolvedora arquivoVinculos;
    private RelacionamentoJogoDesenvolvedora relacionamentoJogoDesenvolvedora;
    private ServicoDesenvolvedora servicoDesenvolvedora;
    private ServicoJogo servicoJogo;

    public Menu() throws Exception {
        scanner = new Scanner(System.in);

        arquivoJogos = new Arquivo<>(
                "jogos",
                Jogo.class.getConstructor());

        arquivoPublicadoras = new Arquivo<>(
                "publicadoras",
                Publicadora.class.getConstructor());

        arquivoDesenvolvedoras = new Arquivo<>(
                "desenvolvedoras",
                Desenvolvedora.class.getConstructor());

        File arquivoArvore = new File("./dados/indices/arvore_pub_jogo.db");

        boolean precisaReconstruirArvore = !arquivoArvore.exists() || arquivoArvore.length() < 8;

        arvorePublicadoraJogo = new persistencia.ArvoreBMais(5, "./dados/indices/arvore_pub_jogo.db");

        if (precisaReconstruirArvore) {
            for (Jogo jogo : arquivoJogos.readAll()) {
                arvorePublicadoraJogo.create(
                        jogo.getPublicadoraId(),
                        jogo.getId());
            }
        }

        arquivoVinculos = new ArquivoJogoDesenvolvedora();
        inicializarServicos();
    }

    private void inicializarServicos() {
        relacionamentoJogoDesenvolvedora = new RelacionamentoJogoDesenvolvedora(
                arquivoJogos,
                arquivoDesenvolvedoras,
                arquivoVinculos);
        servicoDesenvolvedora = new ServicoDesenvolvedora(
                arquivoDesenvolvedoras,
                arquivoVinculos);
        servicoJogo = new ServicoJogo(
                arquivoJogos,
                arquivoPublicadoras,
                arvorePublicadoraJogo,
                arquivoVinculos);
    }

    public void executar() throws Exception {
        int opcao;

        do {
            System.out.println("\n========== CATALOGO DE JOGOS ==========");
            System.out.println();
            System.out.println("1 - Jogos");
            System.out.println("2 - Publicadoras");
            System.out.println("3 - Desenvolvedoras");
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

                case 3:
                    menuDesenvolvedoras();
                    break;

                case 0:
                    System.out.println("Encerrando...");
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);

        arquivoVinculos.close();
        arquivoDesenvolvedoras.close();
        arquivoJogos.close();
        arquivoPublicadoras.close();
        arvorePublicadoraJogo.close();
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
            System.out.println("8 - Associar desenvolvedora ao jogo");
            System.out.println("9 - Remover desenvolvedora do jogo");
            System.out.println("10 - Listar desenvolvedoras do jogo");
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

                case 8:
                    associarJogoDesenvolvedora();
                    break;

                case 9:
                    desassociarJogoDesenvolvedora();
                    break;

                case 10:
                    listarDesenvolvedorasDoJogo();
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

        if (!servicoJogo.publicadoraExiste(publicadoraId)) {
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

        long id = servicoJogo.cadastrar(jogo);

        System.out.println("\nJogo cadastrado com ID: " + id);
    }

    private void consultarJogo() throws Exception {
        System.out.println("\n========== CONSULTAR JOGO ==========");

        System.out.print("ID do jogo: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = servicoJogo.consultar(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
        } else {
            mostrarJogo(jogo);
        }
    }

    private void listarJogos() throws Exception {
        System.out.println("\n========== LISTAR JOGOS ==========");

        ArrayList<Jogo> jogos = servicoJogo.listar();

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

        Jogo jogo = servicoJogo.consultar(id);

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

        if (!servicoJogo.publicadoraExiste(publicadoraId)) {
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

        Jogo novoJogo = new Jogo(
                id,
                nome,
                preco,
                dataLancamento,
                publicadoraId,
                idiomas);

        if (servicoJogo.atualizar(novoJogo)) {
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

        Jogo jogo = servicoJogo.consultar(id);

        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        if (servicoJogo.possuiDesenvolvedorasAssociadas(id)) {
            System.out.println(
                    "Nao e possivel excluir este jogo, pois existem desenvolvedoras associadas a ele.");
            return;
        }

        mostrarJogo(jogo);

        System.out.print("\nTem certeza que deseja excluir? (S/N): ");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            if (servicoJogo.excluir(id)) {
                System.out.println("Jogo excluido com sucesso!");
            } else {
                System.out.println("Nao foi possivel excluir o jogo.");
            }
        } else {
            System.out.println("Operacao cancelada.");
        }
    }

    private void mostrarJogo(Jogo jogo) throws Exception {
        System.out.println("\n---------- JOGO ----------");

        System.out.println("ID: " + jogo.getId());
        System.out.println("Nome: " + jogo.getNome());
        System.out.println("Preco: R$ " + jogo.getPreco());
        System.out.println("Data de lancamento: " + jogo.getDataLancamento());
        System.out.println("ID da publicadora: " + jogo.getPublicadoraId());

        Publicadora publicadora = arquivoPublicadoras.read(jogo.getPublicadoraId());

        if (publicadora != null) {
            System.out.println("Publicadora: " + publicadora.getNome());
        } else {
            System.out.println("Publicadora: nao encontrada");
        }

        System.out.println("Idiomas:");

        for (String idioma : jogo.getIdiomas()) {
            System.out.println(" - " + idioma);
        }

        System.out.println("--------------------------");
    }

    private void ordenarJogos() {
        boolean arquivoFechado = false;

        try {
            System.out.println("\n========== ORDENACAO EXTERNA ==========");
            System.out.println("Iniciando o processo de intercalacao balanceada...");

            arquivoJogos.close();
            arquivoFechado = true;

            persistencia.OrdenacaoExterna ordenacao = new persistencia.OrdenacaoExterna(
                    "./dados/jogos/jogos.db",
                    3);

            ordenacao.ordenarPorNome();
            ordenacao.intercalar();
            ordenacao.finalizar();

            arquivoJogos = new Arquivo<>(
                    "jogos",
                    Jogo.class.getConstructor());
            inicializarServicos();

            arquivoFechado = false;

            System.out.println("Sucesso: base de dados ordenada e Hash reconstruido!");

        } catch (Exception e) {
            System.out.println("Erro ao ordenar arquivo: " + e.getMessage());
            e.printStackTrace();

            if (arquivoFechado) {
                try {
                    arquivoJogos = new Arquivo<>(
                            "jogos",
                            Jogo.class.getConstructor());
                    inicializarServicos();
                } catch (Exception erroReabertura) {
                    System.out.println("Erro ao reabrir o arquivo de jogos: "
                            + erroReabertura.getMessage());
                }
            }
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

        ArrayList<Jogo> jogos = servicoJogo.listarPorPublicadora(publicadoraId);

        if (jogos.isEmpty()) {
            System.out.println("Nenhum jogo encontrado para a publicadora " + publicadora.getNome() + ".");
            return;
        }

        System.out.println("\nJogos encontrados (" + jogos.size() + "):");
        for (Jogo jogo : jogos) {
            mostrarJogo(jogo);
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

    // =========================================================
    // MENU DE DESENVOLVEDORAS E RELACIONAMENTO N:N
    // =========================================================

    private void menuDesenvolvedoras() throws Exception {
        int opcao;

        do {
            System.out.println("\n========== DESENVOLVEDORAS ==========");
            System.out.println();
            System.out.println("1 - Cadastrar desenvolvedora");
            System.out.println("2 - Consultar desenvolvedora");
            System.out.println("3 - Listar desenvolvedoras");
            System.out.println("4 - Atualizar desenvolvedora");
            System.out.println("5 - Excluir desenvolvedora");
            System.out.println("6 - Associar jogo a desenvolvedora");
            System.out.println("7 - Remover associacao jogo-desenvolvedora");
            System.out.println("8 - Listar jogos da desenvolvedora");
            System.out.println("0 - Voltar");
            System.out.print("\nEscolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    cadastrarDesenvolvedora();
                    break;

                case 2:
                    consultarDesenvolvedora();
                    break;

                case 3:
                    listarDesenvolvedoras();
                    break;

                case 4:
                    atualizarDesenvolvedora();
                    break;

                case 5:
                    excluirDesenvolvedora();
                    break;

                case 6:
                    associarJogoDesenvolvedora();
                    break;

                case 7:
                    desassociarJogoDesenvolvedora();
                    break;

                case 8:
                    listarJogosDaDesenvolvedora();
                    break;

                case 0:
                    break;

                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 0);
    }

    private void cadastrarDesenvolvedora() throws Exception {
        System.out.println("\n========== CADASTRAR DESENVOLVEDORA ==========");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Pais: ");
        String pais = scanner.nextLine();

        System.out.print("Ano de fundacao: ");
        int anoFundacao = scanner.nextInt();
        scanner.nextLine();

        Desenvolvedora desenvolvedora = new Desenvolvedora(
                -1,
                nome,
                pais,
                anoFundacao);

        try {
            long id = servicoDesenvolvedora.cadastrar(desenvolvedora);
            System.out.println("\nDesenvolvedora cadastrada com ID: " + id);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void consultarDesenvolvedora() throws Exception {
        System.out.println("\n========== CONSULTAR DESENVOLVEDORA ==========");

        System.out.print("ID da desenvolvedora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Desenvolvedora desenvolvedora = servicoDesenvolvedora.consultar(id);

        if (desenvolvedora == null) {
            System.out.println("Desenvolvedora nao encontrada.");
        } else {
            mostrarDesenvolvedora(desenvolvedora);
        }
    }

    private void listarDesenvolvedoras() throws Exception {
        System.out.println("\n========== LISTAR DESENVOLVEDORAS ==========");

        ArrayList<Desenvolvedora> desenvolvedoras = servicoDesenvolvedora.listar();

        if (desenvolvedoras.isEmpty()) {
            System.out.println("Nenhuma desenvolvedora cadastrada.");
            return;
        }

        for (Desenvolvedora desenvolvedora : desenvolvedoras) {
            mostrarDesenvolvedora(desenvolvedora);
        }
    }

    private void atualizarDesenvolvedora() throws Exception {
        System.out.println("\n========== ATUALIZAR DESENVOLVEDORA ==========");

        System.out.print("ID da desenvolvedora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Desenvolvedora desenvolvedora = servicoDesenvolvedora.consultar(id);

        if (desenvolvedora == null) {
            System.out.println("Desenvolvedora nao encontrada.");
            return;
        }

        System.out.println("\nDados atuais:");
        mostrarDesenvolvedora(desenvolvedora);

        System.out.println("\nDigite os novos dados:");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Pais: ");
        String pais = scanner.nextLine();

        System.out.print("Ano de fundacao: ");
        int anoFundacao = scanner.nextInt();
        scanner.nextLine();

        Desenvolvedora novaDesenvolvedora = new Desenvolvedora(
                id,
                nome,
                pais,
                anoFundacao);

        try {
            if (servicoDesenvolvedora.atualizar(novaDesenvolvedora)) {
                System.out.println("Desenvolvedora atualizada com sucesso!");
            } else {
                System.out.println("Nao foi possivel atualizar a desenvolvedora.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void excluirDesenvolvedora() throws Exception {
        System.out.println("\n========== EXCLUIR DESENVOLVEDORA ==========");

        System.out.print("ID da desenvolvedora: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        Desenvolvedora desenvolvedora = servicoDesenvolvedora.consultar(id);

        if (desenvolvedora == null) {
            System.out.println("Desenvolvedora nao encontrada.");
            return;
        }

        if (servicoDesenvolvedora.possuiJogosAssociados(id)) {
            System.out.println(
                    "Nao e possivel excluir esta desenvolvedora, pois existem jogos associados a ela.");
            return;
        }

        mostrarDesenvolvedora(desenvolvedora);

        System.out.print("\nTem certeza que deseja excluir? (S/N): ");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            if (servicoDesenvolvedora.excluir(id)) {
                System.out.println("Desenvolvedora excluida com sucesso!");
            } else {
                System.out.println("Nao foi possivel excluir a desenvolvedora.");
            }
        } else {
            System.out.println("Operacao cancelada.");
        }
    }

    private void mostrarDesenvolvedora(Desenvolvedora desenvolvedora) {
        System.out.println("\n---------- DESENVOLVEDORA ----------");
        System.out.println("ID: " + desenvolvedora.getId());
        System.out.println("Nome: " + desenvolvedora.getNome());
        System.out.println("Pais: " + desenvolvedora.getPais());
        System.out.println("Ano de fundacao: " + desenvolvedora.getAnoFundacao());
        System.out.println("------------------------------------");
    }

    private void associarJogoDesenvolvedora() throws Exception {
        System.out.println("\n========== ASSOCIAR JOGO E DESENVOLVEDORA ==========");

        System.out.print("ID do jogo: ");
        long jogoId = scanner.nextLong();
        scanner.nextLine();

        System.out.print("ID da desenvolvedora: ");
        long desenvolvedoraId = scanner.nextLong();
        scanner.nextLine();

        try {
            if (relacionamentoJogoDesenvolvedora.associar(jogoId, desenvolvedoraId)) {
                System.out.println("Associacao cadastrada com sucesso!");
            } else {
                System.out.println("Esta associacao ja existe.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro ao associar: " + e.getMessage());
        }
    }

    private void desassociarJogoDesenvolvedora() throws Exception {
        System.out.println("\n========== REMOVER ASSOCIACAO JOGO-DESENVOLVEDORA ==========");

        System.out.print("ID do jogo: ");
        long jogoId = scanner.nextLong();
        scanner.nextLine();

        System.out.print("ID da desenvolvedora: ");
        long desenvolvedoraId = scanner.nextLong();
        scanner.nextLine();

        if (relacionamentoJogoDesenvolvedora.desassociar(jogoId, desenvolvedoraId)) {
            System.out.println("Associacao removida com sucesso!");
        } else {
            System.out.println("Associacao nao encontrada.");
        }
    }

    private void listarDesenvolvedorasDoJogo() throws Exception {
        System.out.println("\n========== DESENVOLVEDORAS DO JOGO ==========");

        System.out.print("ID do jogo: ");
        long jogoId = scanner.nextLong();
        scanner.nextLine();

        Jogo jogo = servicoJogo.consultar(jogoId);
        if (jogo == null) {
            System.out.println("Jogo nao encontrado.");
            return;
        }

        ArrayList<Long> idsDesenvolvedoras = relacionamentoJogoDesenvolvedora.desenvolvedorasDoJogo(jogoId);
        if (idsDesenvolvedoras.isEmpty()) {
            System.out.println("Nenhuma desenvolvedora associada ao jogo " + jogo.getNome() + ".");
            return;
        }

        System.out.println("\nDesenvolvedoras associadas (" + idsDesenvolvedoras.size() + "):");
        for (long devId : idsDesenvolvedoras) {
            Desenvolvedora dev = servicoDesenvolvedora.consultar(devId);
            if (dev != null) {
                mostrarDesenvolvedora(dev);
            }
        }
    }

    private void listarJogosDaDesenvolvedora() throws Exception {
        System.out.println("\n========== JOGOS DA DESENVOLVEDORA ==========");

        System.out.print("ID da desenvolvedora: ");
        long desenvolvedoraId = scanner.nextLong();
        scanner.nextLine();

        Desenvolvedora dev = servicoDesenvolvedora.consultar(desenvolvedoraId);
        if (dev == null) {
            System.out.println("Desenvolvedora nao encontrada.");
            return;
        }

        ArrayList<Long> idsJogos = relacionamentoJogoDesenvolvedora.jogosDaDesenvolvedora(desenvolvedoraId);
        if (idsJogos.isEmpty()) {
            System.out.println("Nenhum jogo associado a desenvolvedora " + dev.getNome() + ".");
            return;
        }

        System.out.println("\nJogos associados (" + idsJogos.size() + "):");
        for (long idJogo : idsJogos) {
            Jogo jogo = servicoJogo.consultar(idJogo);
            if (jogo != null) {
                mostrarJogo(jogo);
            }
        }
    }
}