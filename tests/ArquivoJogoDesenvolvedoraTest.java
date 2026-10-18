import persistencia.ArquivoJogoDesenvolvedora;
import entidades.JogoDesenvolvedora;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/** Executar em diretório temporário vazio, pois cria sua própria pasta dados. */
public class ArquivoJogoDesenvolvedoraTest {
    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }

    public static void main(String[] args) throws Exception {
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            verificar(arquivo.readAll().isEmpty(), "O teste exige uma base vazia");
            verificar(arquivo.create(1, 10), "Primeiro vínculo");
            verificar(!arquivo.create(1, 10), "Recusar duplicata");
            verificar(arquivo.create(1, 20), "Um jogo com duas desenvolvedoras");
            verificar(arquivo.read(1, 20).getId() == 2, "Duplicata não consome ID");
            verificar(arquivo.create(2, 10), "Uma desenvolvedora com dois jogos");
            verificar(arquivo.create(10, 1), "A ordem dos IDs faz parte da chave");
            verificar(arquivo.read(99, 99) == null, "Par ausente");
            verificar(!arquivo.delete(99, 99), "Exclusão ausente");
            for (long[] par : new long[][] {{0, 10}, {1, -1}}) {
                try {
                    arquivo.create(par[0], par[1]);
                    throw new AssertionError("ID inválido aceito");
                } catch (IllegalArgumentException esperado) { }
            }
            verificar(arquivo.readAll().size() == 4, "Quantidade sem duplicatas ou IDs inválidos");
            verificar(arquivo.readDesenvolvedoras(1).equals(Arrays.asList(10L, 20L)), "Consulta por jogo");
            verificar(arquivo.readJogos(10).equals(Arrays.asList(1L, 2L)), "Consulta por desenvolvedora");
            verificar(arquivo.readJogos(999).isEmpty(), "Desenvolvedora sem vínculos");
            verificar(arquivo.readDesenvolvedoras(999).isEmpty(), "Jogo sem vínculos");
        }
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            verificar(arquivo.readAll().size() == 4, "Persistência após reabrir");
            verificar(!arquivo.create(1, 10), "Duplicata após reabrir");
            verificar(arquivo.delete(1, 10), "Excluir vínculo");
            verificar(arquivo.read(1, 10) == null, "Lápide respeitada");
            verificar(arquivo.read(1, 20) != null && arquivo.read(2, 10) != null,
                "Demais vínculos preservados");
        }
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            verificar(arquivo.read(1, 10) == null, "Exclusão persiste");
            verificar(arquivo.create(1, 10), "Recriar par excluído");
            verificar(arquivo.read(1, 10).getId() == 5, "Continuidade dos IDs");
            verificar(!arquivo.create(1, 10), "Recriação também impede duplicata");
            // Ordem inversa e muitos pares forçam divisões de folhas e nós internos.
            for (long jogo = 35; jogo >= 21; jogo--) {
                for (long dev = 48; dev >= 41; dev--) {
                    verificar(arquivo.create(jogo, dev), "Inserção em lote");
                }
            }
            conferirIndices(arquivo);
        }
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            conferirIndices(arquivo);
            verificar(!arquivo.create(25, 45), "Duplicata em árvore com múltiplos níveis");
            verificar(arquivo.delete(25, 45), "Exclusão em árvore com múltiplos níveis");
            conferirIndices(arquivo);
        }
        // Simula índice ausente em uma base existente (migração da etapa anterior).
        Files.delete(Paths.get("dados/indices/arvore_jogo_vinculo.db"));
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            conferirIndices(arquivo);
            verificar(arquivo.read(25, 45) == null, "Reconstrução respeita lápides");
        }
        // Simula atualização interrompida, deixando um índice incompleto e um marcador.
        Files.write(Paths.get("dados/indices/jogos_desenvolvedoras.pendente"), new byte[] {1});
        Files.write(Paths.get("dados/indices/arvore_desenvolvedora_vinculo.db"), new byte[] {0});
        try (ArquivoJogoDesenvolvedora arquivo = new ArquivoJogoDesenvolvedora()) {
            conferirIndices(arquivo);
            verificar(!arquivo.create(21, 41), "Duplicata após recuperação");
            verificar(arquivo.create(25, 45), "Recriação após recuperação");
            conferirIndices(arquivo);
        }
        verificar(!Files.exists(Paths.get("dados/indices/jogos_desenvolvedoras.pendente")), "Marcador removido");
        System.out.println("OK: arquivo intermediario, indices nos dois sentidos, splits, exclusao e recuperacao.");
    }

    private static void conferirIndices(ArquivoJogoDesenvolvedora arquivo) throws Exception {
        ArrayList<JogoDesenvolvedora> registros = arquivo.readAll();
        for (JogoDesenvolvedora vinculo : registros) {
            verificar(arquivo.read(vinculo.getJogoId(), vinculo.getDesenvolvedoraId()).getId()
                == vinculo.getId(), "Consulta do par pelo índice e Hash");
        }
        for (long entidade = 1; entidade <= 50; entidade++) {
            ArrayList<Long> jogos = new ArrayList<>();
            ArrayList<Long> desenvolvedoras = new ArrayList<>();
            for (JogoDesenvolvedora vinculo : registros) {
                if (vinculo.getJogoId() == entidade) desenvolvedoras.add(vinculo.getDesenvolvedoraId());
                if (vinculo.getDesenvolvedoraId() == entidade) jogos.add(vinculo.getJogoId());
            }
            Collections.sort(jogos);
            Collections.sort(desenvolvedoras);
            verificar(arquivo.readJogos(entidade).equals(jogos), "Índice por desenvolvedora " + entidade);
            verificar(arquivo.readDesenvolvedoras(entidade).equals(desenvolvedoras), "Índice por jogo " + entidade);
        }
    }
}
