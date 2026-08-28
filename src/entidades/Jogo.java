package entidades;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import persistencia.Registro;

public class Jogo implements Registro {
    long id;
    String nome;
    float preco;
    LocalDate dataLancamento;
    long publicadoraId;
    String[] idiomas;

    public Jogo() {
        this(-1, "Sem nome", 0, LocalDate.now(), -1, new String[0]);
    }

    public Jogo(long id, String nome, float preco, LocalDate dataLancamento, long publicadoraId, String[] idiomas) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.dataLancamento = dataLancamento;
        this.publicadoraId = publicadoraId;
        this.idiomas = idiomas;
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
        
        dos.writeInt(this.idiomas.length);

        for (String idioma : this.idiomas) {
            dos.writeUTF(idioma);
        }

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

        int quantidade = dis.readInt();

        this.idiomas = new String[quantidade];

        for (int i = 0; i < quantidade; i++) {
            this.idiomas[i] = dis.readUTF();
        }
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
    public void setIdiomas(String[] idiomas) {
        this.idiomas = idiomas;
    }
    public void setIdiomas(String idioma, int pos) {
        try {
            idiomas[pos] = idioma;
        }
        catch (ArrayIndexOutOfBoundsException e){
            System.out.println("!ERRO! Posicao de idioma invalida.");
        }
    }
    public String[] getIdiomas() {
        return idiomas;
    }
    public String getIdioma(int pos) {
        try {
            return idiomas[pos];
        }
        catch (ArrayIndexOutOfBoundsException e){
            System.out.println("!ERRO! Posicao de idioma invalida.");
            return null;
        }
    }
}
