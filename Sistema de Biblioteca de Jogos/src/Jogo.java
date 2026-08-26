import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;

public class Jogo implements Registro {
    long id;
    String nome;
    float preco;
    LocalDate dataLancamento;
    long publicadoraId;
    boolean ativo; // Lapide para marcar o arquivo como "deletado" ou nao

    public Jogo() {
        this(-1, "Sem nome", 0, LocalDate.now(), -1, true);
    }

    public Jogo(long id, String nome, float preco, LocalDate dataLancamento, long publicadoraId, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.dataLancamento = dataLancamento;
        this.publicadoraId = publicadoraId;
        this.ativo = ativo;
    }
    
    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(this.id);
        dos.writeUTF(this.nome);
        dos.writeFloat(preco);
        dos.writeLong(publicadoraId);
        dos.writeLong(this.dataLancamento.toEpochDay());

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);
        this.id = dis.readLong();
        this.nome = dis.readUTF();
        this.preco = dis.readFloat();
        this.publicadoraId = dis.readLong();
        this.dataLancamento = LocalDate.ofEpochDay(dis.readLong());
    }

    //Getters e Setters
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
    public float getPreco() {
        return preco;
    }
    public void setPreco(float preco) {
        this.preco = preco;
    }
    public LocalDate getDataLancamento() {
        return dataLancamento;
    }
    public void setDataLancamento(LocalDate dataLancamento) {
        this.dataLancamento = dataLancamento;
    }
    public long getPublicadoraId() {
        return publicadoraId;
    }
    public void setPublicadoraId(long publicadoraId) {
        this.publicadoraId = publicadoraId;
    }
    public boolean isAtivo() {
        return ativo;
    }
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
