package persistencia;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class HashExtensivel {

    private RandomAccessFile arquivoDiretorio;
    private RandomAccessFile arquivoBuckets;

    private int profundidadeGlobal;

    // Quantidade máxima de pares chave/endereço em cada bucket
    private static final int CAPACIDADE_BUCKET = 4;

    public HashExtensivel(String nome) throws Exception {

        File pasta = new File("./dados/indices");

        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        arquivoDiretorio = new RandomAccessFile(
                "./dados/indices/" + nome + ".dir",
                "rw");

        arquivoBuckets = new RandomAccessFile(
                "./dados/indices/" + nome + ".bkt",
                "rw");

        if (arquivoDiretorio.length() == 0) {
            inicializar();
        } else {
            arquivoDiretorio.seek(0);
            profundidadeGlobal = arquivoDiretorio.readInt();
        }
    }

    // =========================================================
    // INICIALIZAÇÃO
    // =========================================================

    private void inicializar() throws Exception {

        profundidadeGlobal = 0;

        Bucket primeiroBucket = new Bucket(0);

        long enderecoBucket = 0;

        escreverBucket(enderecoBucket, primeiroBucket);

        arquivoDiretorio.seek(0);

        // Profundidade global
        arquivoDiretorio.writeInt(profundidadeGlobal);

        // Com profundidade 0, existe apenas uma entrada
        arquivoDiretorio.writeLong(enderecoBucket);
    }

    // =========================================================
    // HASH
    // =========================================================

    private int hash(long chave) {

        int hash = Long.hashCode(chave);

        if (profundidadeGlobal == 0) {
            return 0;
        }

        int mascara = (1 << profundidadeGlobal) - 1;

        return hash & mascara;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public void create(long chave, long endereco) throws Exception {

        int indice = hash(chave);

        long enderecoBucket = lerEnderecoBucket(indice);

        Bucket bucket = lerBucket(enderecoBucket);

        // Caso a chave já exista, apenas atualiza o endereço
        for (Entrada entrada : bucket.entradas) {

            if (entrada.chave == chave) {
                entrada.endereco = endereco;
                escreverBucket(enderecoBucket, bucket);
                return;
            }
        }

        // Ainda existe espaço no bucket
        if (bucket.entradas.size() < CAPACIDADE_BUCKET) {

            bucket.entradas.add(
                    new Entrada(chave, endereco));

            escreverBucket(enderecoBucket, bucket);

            return;
        }

        // Bucket cheio
        dividirBucket(enderecoBucket, bucket);

        // Tenta inserir novamente após a divisão
        create(chave, endereco);
    }

    // =========================================================
    // READ
    // =========================================================

    public long read(long chave) throws Exception {

        int indice = hash(chave);

        long enderecoBucket = lerEnderecoBucket(indice);

        Bucket bucket = lerBucket(enderecoBucket);

        for (Entrada entrada : bucket.entradas) {

            if (entrada.chave == chave) {
                return entrada.endereco;
            }
        }

        return -1;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(long chave, long novoEndereco) throws Exception {

        int indice = hash(chave);

        long enderecoBucket = lerEnderecoBucket(indice);

        Bucket bucket = lerBucket(enderecoBucket);

        for (Entrada entrada : bucket.entradas) {

            if (entrada.chave == chave) {

                entrada.endereco = novoEndereco;

                escreverBucket(enderecoBucket, bucket);

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(long chave) throws Exception {

        int indice = hash(chave);

        long enderecoBucket = lerEnderecoBucket(indice);

        Bucket bucket = lerBucket(enderecoBucket);

        for (int i = 0; i < bucket.entradas.size(); i++) {

            if (bucket.entradas.get(i).chave == chave) {

                bucket.entradas.remove(i);

                escreverBucket(enderecoBucket, bucket);

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // DIVISÃO DE BUCKET
    // =========================================================

    private void dividirBucket(
            long enderecoBucketAntigo,
            Bucket bucketAntigo) throws Exception {

        int profundidadeAntiga = bucketAntigo.profundidadeLocal;

        /*
         * Se a profundidade local chegou à global,
         * precisamos dobrar o diretório.
         */
        if (profundidadeAntiga == profundidadeGlobal) {
            duplicarDiretorio();
        }

        int novaProfundidade = profundidadeAntiga + 1;

        bucketAntigo.profundidadeLocal = novaProfundidade;

        Bucket novoBucket = new Bucket(novaProfundidade);

        long enderecoNovoBucket = arquivoBuckets.length();

        escreverBucket(
                enderecoNovoBucket,
                novoBucket);

        /*
         * Atualiza as entradas do diretório.
         */
        long[] diretorio = lerDiretorio();

        int bit = 1 << profundidadeAntiga;

        for (int i = 0; i < diretorio.length; i++) {

            if (diretorio[i] == enderecoBucketAntigo) {

                if ((i & bit) != 0) {
                    diretorio[i] = enderecoNovoBucket;
                }
            }
        }

        escreverDiretorio(diretorio);

        /*
         * Guarda temporariamente as entradas antigas.
         */
        ArrayList<Entrada> entradasAntigas = new ArrayList<>(bucketAntigo.entradas);

        bucketAntigo.entradas.clear();

        escreverBucket(
                enderecoBucketAntigo,
                bucketAntigo);

        escreverBucket(
                enderecoNovoBucket,
                novoBucket);

        /*
         * Redistribui as entradas usando o novo diretório.
         */
        for (Entrada entrada : entradasAntigas) {
            create(entrada.chave, entrada.endereco);
        }
    }

    // =========================================================
    // DUPLICAÇÃO DO DIRETÓRIO
    // =========================================================

    private void duplicarDiretorio() throws Exception {

        long[] diretorioAntigo = lerDiretorio();

        profundidadeGlobal++;

        long[] novoDiretorio = new long[diretorioAntigo.length * 2];

        for (int i = 0; i < diretorioAntigo.length; i++) {

            novoDiretorio[i] = diretorioAntigo[i];

            novoDiretorio[i + diretorioAntigo.length] = diretorioAntigo[i];
        }

        escreverDiretorio(novoDiretorio);
    }

    // =========================================================
    // DIRETÓRIO
    // =========================================================

    private long[] lerDiretorio() throws Exception {

        arquivoDiretorio.seek(0);

        profundidadeGlobal = arquivoDiretorio.readInt();

        int quantidade = 1 << profundidadeGlobal;

        long[] diretorio = new long[quantidade];

        for (int i = 0; i < quantidade; i++) {
            diretorio[i] = arquivoDiretorio.readLong();
        }

        return diretorio;
    }

    private void escreverDiretorio(
            long[] diretorio) throws Exception {

        arquivoDiretorio.setLength(0);

        arquivoDiretorio.seek(0);

        arquivoDiretorio.writeInt(
                profundidadeGlobal);

        for (long endereco : diretorio) {
            arquivoDiretorio.writeLong(endereco);
        }
    }

    private long lerEnderecoBucket(
            int indice) throws Exception {

        arquivoDiretorio.seek(
                4L + (indice * 8L));

        return arquivoDiretorio.readLong();
    }

    // =========================================================
    // BUCKET
    // =========================================================

    private Bucket lerBucket(
            long endereco) throws Exception {

        arquivoBuckets.seek(endereco);

        int profundidadeLocal = arquivoBuckets.readInt();

        int quantidade = arquivoBuckets.readInt();

        Bucket bucket = new Bucket(profundidadeLocal);

        for (int i = 0; i < quantidade; i++) {

            long chave = arquivoBuckets.readLong();

            long enderecoRegistro = arquivoBuckets.readLong();

            bucket.entradas.add(
                    new Entrada(
                            chave,
                            enderecoRegistro));
        }

        return bucket;
    }

    private void escreverBucket(
            long endereco,
            Bucket bucket) throws Exception {

        arquivoBuckets.seek(endereco);

        arquivoBuckets.writeInt(
                bucket.profundidadeLocal);

        arquivoBuckets.writeInt(
                bucket.entradas.size());

        for (int i = 0; i < CAPACIDADE_BUCKET; i++) {

            if (i < bucket.entradas.size()) {

                Entrada entrada = bucket.entradas.get(i);

                arquivoBuckets.writeLong(
                        entrada.chave);

                arquivoBuckets.writeLong(
                        entrada.endereco);

            } else {

                // Espaço vazio do bucket
                arquivoBuckets.writeLong(-1);
                arquivoBuckets.writeLong(-1);
            }
        }
    }

    // =========================================================
    // CLOSE
    // =========================================================

    public void close() throws Exception {

        arquivoDiretorio.close();
        arquivoBuckets.close();
    }

    // =========================================================
    // CLASSES INTERNAS
    // =========================================================

    private static class Entrada {

        long chave;
        long endereco;

        public Entrada(
                long chave,
                long endereco) {
            this.chave = chave;
            this.endereco = endereco;
        }
    }

    private static class Bucket {

        int profundidadeLocal;

        ArrayList<Entrada> entradas;

        public Bucket(
                int profundidadeLocal) {
            this.profundidadeLocal = profundidadeLocal;

            this.entradas = new ArrayList<>();
        }
    }
}