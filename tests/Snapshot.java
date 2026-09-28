import entidades.Jogo;
import persistencia.Arquivo;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Inspeção independente: compara varredura física e todas as buscas via Hash. */
public class Snapshot {
    public static void main(String[] args) throws Exception {
        Arquivo<Jogo> arquivo = new Arquivo<>("jogos", Jogo.class.getConstructor());
        try {
            List<Jogo> jogos = arquivo.readAll();
            Map<Long, Jogo> ativos = new HashMap<>();
            for (Jogo jogo : jogos) {
                if (ativos.put(jogo.getId(), jogo) != null) throw new AssertionError("ID duplicado: " + jogo.getId());
            }
            long ultimoId;
            try (RandomAccessFile db = new RandomAccessFile("dados/jogos/jogos.db", "r")) { ultimoId = db.readLong(); }
            for (long id = 1; id <= ultimoId + 1; id++) {
                Jogo peloHash = arquivo.read(id);
                Jogo fisico = ativos.get(id);
                if (fisico == null ? peloHash != null : peloHash == null || !Arrays.equals(fisico.toByteArray(), peloHash.toByteArray())) {
                    throw new AssertionError("Hash diverge do registro físico: " + id);
                }
            }
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(Path.of(args[0])))) {
                out.writeInt(jogos.size());
                for (Jogo jogo : jogos) { byte[] bytes = jogo.toByteArray(); out.writeInt(bytes.length); out.write(bytes); }
            }
        } finally { arquivo.close(); }
    }
}
