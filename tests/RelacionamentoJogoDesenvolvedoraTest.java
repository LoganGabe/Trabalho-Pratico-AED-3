import entidades.Desenvolvedora;
import entidades.Jogo;
import entidades.Publicadora;
import persistencia.Arquivo;
import persistencia.ArquivoJogoDesenvolvedora;
import persistencia.ArvoreBMais;
import persistencia.RelacionamentoJogoDesenvolvedora;
import persistencia.ServicoDesenvolvedora;
import persistencia.ServicoJogo;
import java.time.LocalDate;
import java.util.Arrays;

/** Testa o CRUD definitivo de Desenvolvedora, integridade referencial 1:N / N:N e regras de exclusão. */
public class RelacionamentoJogoDesenvolvedoraTest {

    private static void verificar(boolean valor, String mensagem) {
        if (!valor) throw new AssertionError(mensagem);
    }

    private static void recusarAssociacao(RelacionamentoJogoDesenvolvedora relacionamento,
            long jogo, long desenvolvedora, String mensagem) throws Exception {
        try {
            relacionamento.associar(jogo, desenvolvedora);
            throw new AssertionError("Aceitou associação inválida");
        } catch (IllegalArgumentException esperado) {
            verificar(esperado.getMessage().contains(mensagem), "Motivo da recusa: " + esperado.getMessage());
        }
    }

    private static void testarSerializacaoDesenvolvedora() throws Exception {
        Desenvolvedora original = new Desenvolvedora(7L, "FromSoftware", "Japao", 1986);
        byte[] bytes = original.toByteArray();
        Desenvolvedora restaurada = new Desenvolvedora();
        restaurada.fromByteArray(bytes);

        verificar(restaurada.getId() == 7L, "ID serializado da Desenvolvedora");
        verificar("FromSoftware".equals(restaurada.getNome()), "Nome serializado da Desenvolvedora");
        verificar("Japao".equals(restaurada.getPais()), "Pais serializado da Desenvolvedora");
        verificar(restaurada.getAnoFundacao() == 1986, "Ano serializado da Desenvolvedora");
    }

    public static void main(String[] args) throws Exception {
        testarSerializacaoDesenvolvedora();

        Arquivo<Publicadora> publicadoras = new Arquivo<>("publicadoras_teste_integracao", Publicadora.class.getConstructor());
        Arquivo<Jogo> jogos = new Arquivo<>("jogos_teste_integracao", Jogo.class.getConstructor());
        Arquivo<Desenvolvedora> desenvolvedoras = new Arquivo<>("desenvolvedoras_teste_integracao", Desenvolvedora.class.getConstructor());
        ArvoreBMais arvorePubJogo = new ArvoreBMais(5, "./dados/indices/arvore_pub_jogo_teste.db");

        try (ArquivoJogoDesenvolvedora vinculos = new ArquivoJogoDesenvolvedora()) {
            verificar(vinculos.readAll().isEmpty(), "Exige base temporária vazia");

            ServicoDesenvolvedora servicoDev = new ServicoDesenvolvedora(desenvolvedoras, vinculos);
            ServicoJogo servicoJogo = new ServicoJogo(jogos, publicadoras, arvorePubJogo, vinculos);
            RelacionamentoJogoDesenvolvedora relacionamento =
                new RelacionamentoJogoDesenvolvedora(jogos, desenvolvedoras, vinculos);

            // 1. Teste de CRUD de Desenvolvedora e indexação por PK
            long pubId = publicadoras.create(new Publicadora(-1, "Bandai Namco", "Japao", 1955, "Publisher"));
            long devId = servicoDev.cadastrar(new Desenvolvedora(-1, "FromSoftware", "Japao", 1986));
            verificar(devId == 1, "Primeiro ID de Desenvolvedora");

            Desenvolvedora lida = servicoDev.consultar(devId);
            verificar(lida != null && "FromSoftware".equals(lida.getNome()), "Consulta por PK no Hash");

            verificar(servicoDev.atualizar(new Desenvolvedora(devId, "FromSoftware Inc.", "Japao", 1986)), "Atualizar Desenvolvedora");
            verificar("FromSoftware Inc.".equals(servicoDev.consultar(devId).getNome()), "Nome atualizado persistido");
            verificar(servicoDev.listar().size() == 1, "Listagem de Desenvolvedoras");

            // 2. Cadastro de Jogo validando FK de Publicadora
            long jogoId = servicoJogo.cadastrar(new Jogo(
                -1, "Elden Ring", 249.90f, LocalDate.of(2022, 2, 25), pubId, new String[]{"Portugues", "Ingles"}
            ));

            // 3. Validações de existência antes de associar (2.5)
            recusarAssociacao(relacionamento, 999, devId, "Jogo inexistente");
            recusarAssociacao(relacionamento, jogoId, 999, "Desenvolvedora inexistente");
            recusarAssociacao(relacionamento, 0, devId, "positivos");
            verificar(vinculos.readAll().isEmpty(), "Recusas não gravam vínculos");

            // 4. Associação N:N válida, duplicata e consultas nos dois sentidos (2.8)
            verificar(relacionamento.associar(jogoId, devId), "Associação com entidades reais existentes");
            verificar(vinculos.read(jogoId, devId).getId() == 1, "Recusas não consomem ID");
            verificar(!relacionamento.associar(jogoId, devId), "Rejeição de duplicata");
            verificar(relacionamento.desenvolvedorasDoJogo(jogoId).equals(Arrays.asList(devId)), "Consulta de desenvolvedoras por jogo");
            verificar(relacionamento.jogosDaDesenvolvedora(devId).equals(Arrays.asList(jogoId)), "Consulta de jogos por desenvolvedora");

            // Alterar campos e a publicadora preserva o N:N e sincroniza o 1:N.
            long novaPublicadora = publicadoras.create(new Publicadora(-1, "Outra publicadora", "Brasil", 2000, ""));
            verificar(servicoDev.atualizar(new Desenvolvedora(devId, "Estudio atualizado", "Brasil", 2001)), "Atualizar Desenvolvedora com vínculo");
            verificar(servicoJogo.atualizar(new Jogo(jogoId, "Jogo atualizado", 99f, LocalDate.of(2023, 1, 1),
                novaPublicadora, new String[]{"pt-BR"})), "Atualizar Jogo com vínculo");
            verificar(servicoJogo.listarPorPublicadora(pubId).isEmpty(), "Remover FK antiga da B+");
            verificar(servicoJogo.listarPorPublicadora(novaPublicadora).get(0).getId() == jogoId, "Incluir FK nova na B+");
            verificar(relacionamento.desenvolvedorasDoJogo(jogoId).equals(Arrays.asList(devId)), "N:N preservado após alterações");
            verificar(relacionamento.jogosDaDesenvolvedora(devId).equals(Arrays.asList(jogoId)), "Índice inverso preservado após alterações");
            try {
                servicoJogo.atualizar(new Jogo(jogoId, "Invalido", 0, LocalDate.now(), 999, new String[0]));
                throw new AssertionError("Aceitou atualização com FK inválida e vínculo N:N");
            } catch (IllegalArgumentException esperado) { }
            verificar(servicoJogo.consultar(jogoId).getPublicadoraId() == novaPublicadora, "Atualização inválida preserva FK e dados");

            // 5. Bloqueio de exclusão quando possuem associações ativas (2.6 e 2.7)
            try {
                servicoDev.excluir(devId);
                throw new AssertionError("Permitiu excluir Desenvolvedora com jogo associado");
            } catch (IllegalStateException esperado) {
                verificar(esperado.getMessage().contains("jogos associados"), "Mensagem de bloqueio de Desenvolvedora");
            }

            try {
                servicoJogo.excluir(jogoId);
                throw new AssertionError("Permitiu excluir Jogo com desenvolvedora associada");
            } catch (IllegalStateException esperado) {
                verificar(esperado.getMessage().contains("desenvolvedoras associadas"), "Mensagem de bloqueio de Jogo");
            }

            // 6. Desassociação e permissão de exclusão após remover vínculos
            verificar(relacionamento.desassociar(jogoId, devId), "Desassociar vínculo");
            verificar(!servicoDev.possuiJogosAssociados(devId), "Sem jogos após desassociar");
            verificar(!servicoJogo.possuiDesenvolvedorasAssociadas(jogoId), "Sem desenvolvedoras após desassociar");

            verificar(servicoDev.excluir(devId), "Excluir Desenvolvedora sem vínculos");
            recusarAssociacao(relacionamento, jogoId, devId, "Desenvolvedora inexistente");

            long outraDev = servicoDev.cadastrar(new Desenvolvedora(-1, "Capcom", "Japao", 1979));
            verificar(servicoJogo.excluir(jogoId), "Excluir Jogo sem vínculos");
            recusarAssociacao(relacionamento, jogoId, outraDev, "Jogo inexistente");
            verificar(vinculos.readAll().isEmpty(), "Entidades excluídas não criam vínculos");
        } finally {
            try {
                arvorePubJogo.close();
            } finally {
                try {
                    jogos.close();
                } finally {
                    try {
                        publicadoras.close();
                    } finally {
                        desenvolvedoras.close();
                    }
                }
            }
        }
        System.out.println("OK: CRUD de Desenvolvedora, integracao N:N, bloqueio de exclusao com vinculos e integridade referencial.");
    }
}
