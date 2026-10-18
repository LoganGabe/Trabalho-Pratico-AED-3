package persistencia;

import entidades.Jogo;
import java.util.ArrayList;
import java.util.Objects;

/** Entrada para os CRUDs: valida as entidades antes de criar o vínculo.
 * Os arquivos pertencem ao chamador, que deve fechá-los e manter uma única instância.
 */
public class RelacionamentoJogoDesenvolvedora {
    private final Arquivo<Jogo> jogos;
    private final Arquivo<? extends Registro> desenvolvedoras;
    private final ArquivoJogoDesenvolvedora vinculos;

    public RelacionamentoJogoDesenvolvedora(Arquivo<Jogo> jogos,
            Arquivo<? extends Registro> desenvolvedoras, ArquivoJogoDesenvolvedora vinculos) {
        this.jogos = Objects.requireNonNull(jogos);
        this.desenvolvedoras = Objects.requireNonNull(desenvolvedoras);
        this.vinculos = Objects.requireNonNull(vinculos);
    }

    /** Retorna false para vínculo duplicado; entidade ausente gera IllegalArgumentException. */
    public boolean associar(long jogoId, long desenvolvedoraId) throws Exception {
        if (jogoId <= 0 || desenvolvedoraId <= 0) {
            throw new IllegalArgumentException("Os IDs devem ser positivos.");
        }
        if (jogos.read(jogoId) == null) {
            throw new IllegalArgumentException("Jogo inexistente: " + jogoId);
        }
        if (desenvolvedoras.read(desenvolvedoraId) == null) {
            throw new IllegalArgumentException("Desenvolvedora inexistente: " + desenvolvedoraId);
        }
        return vinculos.create(jogoId, desenvolvedoraId);
    }

    /** Permite limpar um vínculo mesmo se alguma entidade tiver sido removida por fora. */
    public boolean desassociar(long jogoId, long desenvolvedoraId) throws Exception {
        return vinculos.delete(jogoId, desenvolvedoraId);
    }

    public ArrayList<Long> desenvolvedorasDoJogo(long jogoId) throws Exception {
        return vinculos.readDesenvolvedoras(jogoId);
    }

    public ArrayList<Long> jogosDaDesenvolvedora(long desenvolvedoraId) throws Exception {
        return vinculos.readJogos(desenvolvedoraId);
    }
}
