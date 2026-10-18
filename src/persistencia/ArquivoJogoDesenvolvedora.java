package persistencia;

import entidades.JogoDesenvolvedora;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/** Gerencia os vínculos N:N. Usar uma única instância aberta por vez. */
public class ArquivoJogoDesenvolvedora implements AutoCloseable {
    private final Arquivo<JogoDesenvolvedora> arquivo;
    private ArvoreBMais porJogo;
    private ArvoreBMais porDesenvolvedora;
    private boolean inconsistente;
    private final Path indiceJogo = Paths.get("dados/indices/arvore_jogo_vinculo.db");
    private final Path indiceDesenvolvedora = Paths.get("dados/indices/arvore_desenvolvedora_vinculo.db");
    private final Path pendencia = Paths.get("dados/indices/jogos_desenvolvedoras.pendente");

    public ArquivoJogoDesenvolvedora() throws Exception {
        arquivo = new Arquivo<>("jogos_desenvolvedoras", JogoDesenvolvedora.class.getConstructor());
        try {
            if (Files.exists(pendencia) || !Files.exists(indiceJogo)
                    || !Files.exists(indiceDesenvolvedora)
                    || Files.size(indiceJogo) == 0 || Files.size(indiceDesenvolvedora) == 0) {
                reconstruirIndices();
            } else {
                abrirIndices();
            }
        } catch (Exception erro) {
            try { close(); } catch (Exception fechamento) { erro.addSuppressed(fechamento); }
            throw erro;
        }
    }

    private void abrirIndices() throws Exception {
        // A árvore armazena pares de longs: (ID da entidade, ID técnico do vínculo).
        porJogo = new ArvoreBMais(5, indiceJogo.toString());
        porDesenvolvedora = new ArvoreBMais(5, indiceDesenvolvedora.toString());
    }

    private void reconstruirIndices() throws Exception {
        iniciarAlteracao();
        Files.deleteIfExists(indiceJogo);
        Files.deleteIfExists(indiceDesenvolvedora);
        abrirIndices();
        for (JogoDesenvolvedora vinculo : arquivo.readAll()) {
            porJogo.create(vinculo.getJogoId(), vinculo.getId());
            porDesenvolvedora.create(vinculo.getDesenvolvedoraId(), vinculo.getId());
        }
        concluirAlteracao();
    }

    /** Retorna false se o par já existir, sem consumir outro ID técnico. */
    public boolean create(long jogoId, long desenvolvedoraId) throws Exception {
        if (read(jogoId, desenvolvedoraId) != null) return false;
        iniciarAlteracao();
        JogoDesenvolvedora vinculo = new JogoDesenvolvedora(jogoId, desenvolvedoraId);
        arquivo.create(vinculo);
        porJogo.create(jogoId, vinculo.getId());
        porDesenvolvedora.create(desenvolvedoraId, vinculo.getId());
        concluirAlteracao();
        return true;
    }

    /** Examina apenas os vínculos indexados para este jogo, consultados pelo Hash. */
    public JogoDesenvolvedora read(long jogoId, long desenvolvedoraId) throws Exception {
        validarId(jogoId);
        validarId(desenvolvedoraId);
        verificarEstado();
        for (long id : porJogo.read(jogoId)) {
            JogoDesenvolvedora vinculo = lerVinculo(id);
            if (vinculo.getJogoId() == jogoId && vinculo.getDesenvolvedoraId() == desenvolvedoraId) {
                return vinculo;
            }
        }
        return null;
    }

    public ArrayList<Long> readDesenvolvedoras(long jogoId) throws Exception {
        validarId(jogoId);
        verificarEstado();
        ArrayList<Long> ids = new ArrayList<>();
        for (long id : porJogo.read(jogoId)) {
            ids.add(lerVinculo(id).getDesenvolvedoraId());
        }
        ids.sort(Long::compare);
        return ids;
    }

    public ArrayList<Long> readJogos(long desenvolvedoraId) throws Exception {
        validarId(desenvolvedoraId);
        verificarEstado();
        ArrayList<Long> ids = new ArrayList<>();
        for (long id : porDesenvolvedora.read(desenvolvedoraId)) {
            ids.add(lerVinculo(id).getJogoId());
        }
        ids.sort(Long::compare);
        return ids;
    }

    private JogoDesenvolvedora lerVinculo(long id) throws Exception {
        JogoDesenvolvedora vinculo = arquivo.read(id);
        if (vinculo == null) throw new IOException("Indice aponta para vinculo inexistente: " + id);
        return vinculo;
    }

    public boolean delete(long jogoId, long desenvolvedoraId) throws Exception {
        JogoDesenvolvedora vinculo = read(jogoId, desenvolvedoraId);
        if (vinculo == null) return false;
        iniciarAlteracao();
        if (!arquivo.delete(vinculo.getId())
                || !porJogo.delete(jogoId, vinculo.getId())
                || !porDesenvolvedora.delete(desenvolvedoraId, vinculo.getId())) {
            throw new IOException("Falha ao excluir vinculo; feche e reabra para reconstruir os indices.");
        }
        concluirAlteracao();
        return true;
    }

    public ArrayList<JogoDesenvolvedora> readAll() throws Exception {
        verificarEstado();
        return arquivo.readAll();
    }

    private void validarId(long id) {
        if (id <= 0) throw new IllegalArgumentException("Os IDs devem ser positivos.");
    }

    private void verificarEstado() throws IOException {
        if (inconsistente) throw new IOException("Operacao anterior incompleta; feche e reabra o arquivo.");
    }

    private void iniciarAlteracao() throws IOException {
        Files.write(pendencia, new byte[] {1});
        inconsistente = true;
    }

    private void concluirAlteracao() throws IOException {
        Files.delete(pendencia);
        inconsistente = false;
    }

    @Override
    public void close() throws Exception {
        try {
            if (porJogo != null) porJogo.close();
        } finally {
            try {
                if (porDesenvolvedora != null) porDesenvolvedora.close();
            } finally {
                arquivo.close();
            }
        }
    }
}
