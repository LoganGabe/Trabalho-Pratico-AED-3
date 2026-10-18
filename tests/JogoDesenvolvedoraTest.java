import entidades.JogoDesenvolvedora;
import java.io.EOFException;

/** Teste independente: não abre nem altera os arquivos de dados do catálogo. */
public class JogoDesenvolvedoraTest {
    private static void verificar(boolean condicao) {
        if (!condicao) throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        JogoDesenvolvedora original = new JogoDesenvolvedora(7, 12, 34);
        byte[] bytes = original.toByteArray();
        verificar(bytes.length == 24);
        JogoDesenvolvedora copia = new JogoDesenvolvedora();
        copia.fromByteArray(bytes);
        verificar(copia.getId() == 7 && copia.getJogoId() == 12 && copia.getDesenvolvedoraId() == 34);
        try {
            original.fromByteArray(new byte[16]);
            throw new AssertionError("Payload truncado foi aceito");
        } catch (EOFException esperado) {
            verificar(original.getId() == 7 && original.getJogoId() == 12);
        }
        System.out.println("OK: serializacao dos IDs do vinculo e rejeicao de payload truncado.");
    }
}

