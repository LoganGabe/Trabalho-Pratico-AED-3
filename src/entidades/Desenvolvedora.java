package entidades;

import persistencia.Registro;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Desenvolvedora implements Registro {
    private long id;
    private String nome;
    private String pais;
    private int anoFundacao;

    public Desenvolvedora() {
        this.id = -1;
        this.nome = "";
        this.pais = "";
        this.anoFundacao = 0;
    }

    public Desenvolvedora(long id, String nome, String pais, int anoFundacao) {
        this.id = id;
        this.nome = nome != null ? nome : "";
        this.pais = pais != null ? pais : "";
        this.anoFundacao = anoFundacao;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome != null ? nome : "";
    }

    public String getPais() {
        return this.pais;
    }

    public void setPais(String pais) {
        this.pais = pais != null ? pais : "";
    }

    public int getAnoFundacao() {
        return this.anoFundacao;
    }

    public void setAnoFundacao(int anoFundacao) {
        this.anoFundacao = anoFundacao;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeLong(this.id);
        dos.writeUTF(this.nome != null ? this.nome : "");
        dos.writeUTF(this.pais != null ? this.pais : "");
        dos.writeInt(this.anoFundacao);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readLong();
        this.nome = dis.readUTF();
        this.pais = dis.readUTF();
        this.anoFundacao = dis.readInt();
    }

    @Override
    public String toString() {
        return "Desenvolvedora{id=" + id + ", nome='" + nome + "', pais='" + pais + "', anoFundacao=" + anoFundacao + "}";
    }
}