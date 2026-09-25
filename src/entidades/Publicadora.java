package entidades;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import persistencia.Registro;

public class Publicadora implements Registro {

    long id;
    String nome;
    String pais;
    int anoFundacao;
    String descricao;

    public Publicadora() {
        this(-1, "Sem nome", "Sem país", 0, "Sem descrição");
    }

    public Publicadora(long id, String nome, String pais, int anoFundacao, String descricao) {
        this.id = id;
        this.nome = nome;
        this.pais = pais;
        this.anoFundacao = anoFundacao;
        this.descricao = descricao;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeLong(this.id);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.pais);
        dos.writeInt(this.anoFundacao);
        dos.writeUTF(this.descricao);

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
        this.descricao = dis.readUTF();
    }

    // Getters e Setters

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

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public int getAnoFundacao() {
        return anoFundacao;
    }

    public void setAnoFundacao(int anoFundacao) {
        this.anoFundacao = anoFundacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}