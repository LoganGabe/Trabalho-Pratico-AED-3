package entidades;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import persistencia.Registro;

/** Registro intermediário do relacionamento N:N entre Jogo e Desenvolvedora. */
public class JogoDesenvolvedora implements Registro {
    private long id;
    private long jogoId;
    private long desenvolvedoraId;

    public JogoDesenvolvedora() { this(-1, -1, -1); }

    public JogoDesenvolvedora(long jogoId, long desenvolvedoraId) {
        this(-1, jogoId, desenvolvedoraId);
    }

    public JogoDesenvolvedora(long id, long jogoId, long desenvolvedoraId) {
        this.id = id;
        this.jogoId = jogoId;
        this.desenvolvedoraId = desenvolvedoraId;
    }

    // ID técnico exigido por Arquivo; a identidade lógica é o par (jogoId, desenvolvedoraId).
    @Override
    public long getId() { return id; }
    @Override
    public void setId(long id) { this.id = id; }
    public long getJogoId() { return jogoId; }
    public void setJogoId(long jogoId) { this.jogoId = jogoId; }
    public long getDesenvolvedoraId() { return desenvolvedoraId; }
    public void setDesenvolvedoraId(long desenvolvedoraId) { this.desenvolvedoraId = desenvolvedoraId; }

    /** Payload de 24 bytes: id técnico, jogoId, desenvolvedoraId (três longs). */
    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream saida = new DataOutputStream(buffer);
        saida.writeLong(id);
        saida.writeLong(jogoId);
        saida.writeLong(desenvolvedoraId);
        return buffer.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] dados) throws IOException {
        DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(dados));
        long novoId = entrada.readLong();
        long novoJogoId = entrada.readLong();
        long novaDesenvolvedoraId = entrada.readLong();
        id = novoId;
        jogoId = novoJogoId;
        desenvolvedoraId = novaDesenvolvedoraId;
    }
}

