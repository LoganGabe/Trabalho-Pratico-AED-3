package persistencia;

import entidades.Desenvolvedora;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Camada de CRUD e regras de negócio para Desenvolvedora (Itens 2.2 a 2.4 e 2.6/2.7).
 * Utiliza Arquivo<Desenvolvedora> (persistência binária + Hash Extensível por PK)
 * e pode ser consumida tanto pelo Menu de console quanto pela Interface Gráfica.
 */
public class ServicoDesenvolvedora {
    private final Arquivo<Desenvolvedora> arquivoDesenvolvedoras;
    private final ArquivoJogoDesenvolvedora arquivoVinculos;

    public ServicoDesenvolvedora(Arquivo<Desenvolvedora> arquivoDesenvolvedoras) {
        this(arquivoDesenvolvedoras, null);
    }

    public ServicoDesenvolvedora(
            Arquivo<Desenvolvedora> arquivoDesenvolvedoras,
            ArquivoJogoDesenvolvedora arquivoVinculos) {
        this.arquivoDesenvolvedoras = Objects.requireNonNull(arquivoDesenvolvedoras);
        this.arquivoVinculos = arquivoVinculos;
    }

    public long cadastrar(Desenvolvedora desenvolvedora) throws Exception {
        validarCampos(desenvolvedora);
        return arquivoDesenvolvedoras.create(desenvolvedora);
    }

    public Desenvolvedora consultar(long id) throws Exception {
        if (id <= 0) {
            return null;
        }
        return arquivoDesenvolvedoras.read(id);
    }

    public ArrayList<Desenvolvedora> listar() throws Exception {
        return arquivoDesenvolvedoras.readAll();
    }

    public boolean atualizar(Desenvolvedora desenvolvedora) throws Exception {
        Objects.requireNonNull(desenvolvedora, "Desenvolvedora nao pode ser nula.");
        if (desenvolvedora.getId() <= 0) {
            throw new IllegalArgumentException("ID invalido para atualizacao.");
        }
        validarCampos(desenvolvedora);
        if (arquivoDesenvolvedoras.read(desenvolvedora.getId()) == null) {
            return false;
        }
        return arquivoDesenvolvedoras.update(desenvolvedora);
    }

    public boolean possuiJogosAssociados(long desenvolvedoraId) throws Exception {
        if (arquivoVinculos == null || desenvolvedoraId <= 0) {
            return false;
        }
        return !arquivoVinculos.readJogos(desenvolvedoraId).isEmpty();
    }

    public boolean excluir(long id) throws Exception {
        if (id <= 0 || arquivoDesenvolvedoras.read(id) == null) {
            return false;
        }
        if (possuiJogosAssociados(id)) {
            throw new IllegalStateException(
                "Nao e possivel excluir esta desenvolvedora, pois existem jogos associados a ela."
            );
        }
        return arquivoDesenvolvedoras.delete(id);
    }

    private void validarCampos(Desenvolvedora desenvolvedora) {
        Objects.requireNonNull(desenvolvedora, "Desenvolvedora nao pode ser nula.");
        if (desenvolvedora.getNome() == null || desenvolvedora.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da desenvolvedora e obrigatorio.");
        }
        if (desenvolvedora.getPais() == null || desenvolvedora.getPais().trim().isEmpty()) {
            throw new IllegalArgumentException("O pais da desenvolvedora e obrigatorio.");
        }
    }

    public Arquivo<Desenvolvedora> getArquivo() {
        return arquivoDesenvolvedoras;
    }
}