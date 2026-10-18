package persistencia;

import entidades.Jogo;
import entidades.Publicadora;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Camada de CRUD e regras de negócio para Jogo (Itens 2.6, 2.7 e 2.8).
 * Centraliza a validação de FK (Publicadora), sincronização da Árvore B+ (1:N)
 * e bloqueio de exclusão caso o jogo possua desenvolvedoras associadas (N:N).
 */
public class ServicoJogo {
    private final Arquivo<Jogo> arquivoJogos;
    private final Arquivo<Publicadora> arquivoPublicadoras;
    private final ArvoreBMais arvorePublicadoraJogo;
    private final ArquivoJogoDesenvolvedora arquivoVinculos;

    public ServicoJogo(
            Arquivo<Jogo> arquivoJogos,
            Arquivo<Publicadora> arquivoPublicadoras,
            ArvoreBMais arvorePublicadoraJogo,
            ArquivoJogoDesenvolvedora arquivoVinculos) {
        this.arquivoJogos = Objects.requireNonNull(arquivoJogos);
        this.arquivoPublicadoras = Objects.requireNonNull(arquivoPublicadoras);
        this.arvorePublicadoraJogo = Objects.requireNonNull(arvorePublicadoraJogo);
        this.arquivoVinculos = arquivoVinculos;
    }

    public boolean publicadoraExiste(long publicadoraId) throws Exception {
        if (publicadoraId <= 0) {
            return false;
        }
        return arquivoPublicadoras.read(publicadoraId) != null;
    }

    public long cadastrar(Jogo jogo) throws Exception {
        Objects.requireNonNull(jogo, "Jogo nao pode ser nulo.");
        if (!publicadoraExiste(jogo.getPublicadoraId())) {
            throw new IllegalArgumentException("Publicadora nao encontrada.");
        }
        long id = arquivoJogos.create(jogo);
        arvorePublicadoraJogo.create(jogo.getPublicadoraId(), id);
        return id;
    }

    public Jogo consultar(long id) throws Exception {
        if (id <= 0) {
            return null;
        }
        return arquivoJogos.read(id);
    }

    public ArrayList<Jogo> listar() throws Exception {
        return arquivoJogos.readAll();
    }

    public boolean atualizar(Jogo novoJogo) throws Exception {
        Objects.requireNonNull(novoJogo, "Jogo nao pode ser nulo.");
        long id = novoJogo.getId();
        if (id <= 0) {
            throw new IllegalArgumentException("ID do jogo invalido.");
        }
        Jogo jogoAntigo = arquivoJogos.read(id);
        if (jogoAntigo == null) {
            return false;
        }
        if (!publicadoraExiste(novoJogo.getPublicadoraId())) {
            throw new IllegalArgumentException("Publicadora nao encontrada.");
        }
        long publicadoraAntigaId = jogoAntigo.getPublicadoraId();
        long publicadoraNovaId = novoJogo.getPublicadoraId();

        if (arquivoJogos.update(novoJogo)) {
            if (publicadoraAntigaId != publicadoraNovaId) {
                arvorePublicadoraJogo.delete(publicadoraAntigaId, id);
                arvorePublicadoraJogo.create(publicadoraNovaId, id);
            }
            return true;
        }
        return false;
    }

    public boolean possuiDesenvolvedorasAssociadas(long jogoId) throws Exception {
        if (arquivoVinculos == null || jogoId <= 0) {
            return false;
        }
        return !arquivoVinculos.readDesenvolvedoras(jogoId).isEmpty();
    }

    public boolean excluir(long id) throws Exception {
        if (id <= 0) {
            return false;
        }
        Jogo jogo = arquivoJogos.read(id);
        if (jogo == null) {
            return false;
        }
        if (possuiDesenvolvedorasAssociadas(id)) {
            throw new IllegalStateException(
                "Nao e possivel excluir este jogo, pois existem desenvolvedoras associadas a ele."
            );
        }
        long publicadoraId = jogo.getPublicadoraId();
        if (arquivoJogos.delete(id)) {
            arvorePublicadoraJogo.delete(publicadoraId, id);
            return true;
        }
        return false;
    }

    public ArrayList<Jogo> listarPorPublicadora(long publicadoraId) throws Exception {
        ArrayList<Jogo> resultado = new ArrayList<>();
        ArrayList<Long> idsJogos = arvorePublicadoraJogo.read(publicadoraId);
        for (long idJogo : idsJogos) {
            Jogo jogo = arquivoJogos.read(idJogo);
            if (jogo != null) {
                resultado.add(jogo);
            }
        }
        return resultado;
    }
}