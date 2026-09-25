package persistencia;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class Arquivo<T extends Registro> {

    private RandomAccessFile arquivo;
    private Constructor<T> construtor;
    private HashExtensivel hash;

    private static final int TAM_CABECALHO = 16;

    // Cabeçalho:
    // 0  - 7  -> último ID utilizado
    // 8  - 15 -> endereço do primeiro registro excluído
    private static final long POS_ULTIMO_ID = 0;
    private static final long POS_LISTA_EXCLUIDOS = 8;

    // Registro:
    // 1 byte  -> lápide
    // 2 bytes -> tamanho
    // N bytes -> dados
    private static final int TAM_CABECALHO_REGISTRO = 3;

    public Arquivo(String nome, Constructor<T> construtor) throws Exception {

        this.construtor = construtor;

        // =====================================================
        // ARQUIVO DE DADOS
        // =====================================================

        File pastaDados = new File("./dados/" + nome);

        if (!pastaDados.exists()) {
            pastaDados.mkdirs();
        }

        arquivo = new RandomAccessFile(
            "./dados/" + nome + "/" + nome + ".db",
            "rw"
        );

        // Cria o cabeçalho caso o arquivo seja novo
        if (arquivo.length() == 0) {

            arquivo.seek(0);

            // Último ID utilizado
            arquivo.writeLong(0);

            // Início da lista de registros excluídos
            arquivo.writeLong(-1);
        }

        if (arquivo.length() < TAM_CABECALHO) {
            throw new IOException(
                "Arquivo de dados invalido: cabecalho incompleto."
            );
        }

        // =====================================================
        // HASH EXTENSÍVEL
        // =====================================================

        File pastaIndices = new File("./dados/indices");

        if (!pastaIndices.exists()) {
            pastaIndices.mkdirs();
        }

        File arquivoDir =
            new File("./dados/indices/" + nome + ".dir");

        File arquivoBucket =
            new File("./dados/indices/" + nome + ".bkt");

        /*
         * Se o .db já existia, mas ainda não havia índice,
         * reconstruiremos o Hash usando os registros ativos.
         */
        boolean precisaReconstruirIndice =
            !arquivoDir.exists()
            || arquivoDir.length() == 0
            || !arquivoBucket.exists()
            || arquivoBucket.length() == 0;

        /*
         * Se apenas um dos arquivos do índice existir ou algum
         * estiver vazio, descartamos os dois e reconstruímos.
         */
        if (precisaReconstruirIndice) {

            if (arquivoDir.exists()) {
                arquivoDir.delete();
            }

            if (arquivoBucket.exists()) {
                arquivoBucket.delete();
            }
        }

        hash = new HashExtensivel(nome);

        if (precisaReconstruirIndice
                && arquivo.length() > TAM_CABECALHO) {

            reconstruirIndice();
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    public long create(T obj) throws Exception {

        // Obtém o último ID
        arquivo.seek(POS_ULTIMO_ID);

        long ultimoId = arquivo.readLong();

        long novoId = ultimoId + 1;

        // Atualiza o cabeçalho
        arquivo.seek(POS_ULTIMO_ID);
        arquivo.writeLong(novoId);

        // Coloca o ID no objeto
        obj.setId(novoId);

        // Serializa
        byte[] dados = obj.toByteArray();

        verificarTamanho(dados);

        // Grava no arquivo
        long endereco = gravarRegistro(dados);

        // Adiciona ao índice
        hash.create(novoId, endereco);

        return novoId;
    }

    // =========================================================
    // READ
    // =========================================================

    public T read(long id) throws Exception {

        /*
         * Antes:
         *
         * percorríamos jogos.db inteiro.
         *
         * Agora:
         *
         * ID -> Hash -> endereço -> registro
         */

        long endereco = hash.read(id);

        if (endereco == -1) {
            return null;
        }

        if (endereco < TAM_CABECALHO
                || endereco >= arquivo.length()) {
            return null;
        }

        arquivo.seek(endereco);

        byte lapide = arquivo.readByte();

        int tamanho = arquivo.readUnsignedShort();

        if (lapide != ' ') {
            return null;
        }

        byte[] dados = new byte[tamanho];

        arquivo.readFully(dados);

        T obj = construtor.newInstance();

        obj.fromByteArray(dados);

        /*
         * Verificação adicional para garantir que o índice
         * apontou realmente para o registro desejado.
         */
        if (obj.getId() != id) {
            return null;
        }

        return obj;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public boolean update(T novoObj) throws Exception {

        long id = novoObj.getId();

        long enderecoAntigo = hash.read(id);

        if (enderecoAntigo == -1) {
            return false;
        }

        arquivo.seek(enderecoAntigo);

        byte lapide = arquivo.readByte();

        int tamanhoAntigo =
            arquivo.readUnsignedShort();

        if (lapide != ' ') {
            return false;
        }

        byte[] novosDados =
            novoObj.toByteArray();

        verificarTamanho(novosDados);

        // =====================================================
        // NOVO REGISTRO CABE NO ESPAÇO ANTIGO
        // =====================================================

        if (novosDados.length <= tamanhoAntigo) {

            arquivo.seek(
                enderecoAntigo
                + TAM_CABECALHO_REGISTRO
            );

            arquivo.write(novosDados);

            /*
             * Preenche a sobra com zeros.
             * O tamanho físico do registro permanece o mesmo.
             */
            for (int i = novosDados.length;
                    i < tamanhoAntigo;
                    i++) {

                arquivo.writeByte(0);
            }

            /*
             * O endereço não mudou, mas mantemos explicitamente
             * o índice sincronizado.
             */
            hash.update(
                id,
                enderecoAntigo
            );

            return true;
        }

        // =====================================================
        // NOVO REGISTRO NÃO CABE
        // =====================================================

        /*
         * O registro antigo é marcado como excluído.
         */
        arquivo.seek(enderecoAntigo);

        arquivo.writeByte('*');

        adicionarExcluido(
            enderecoAntigo
        );

        /*
         * Procura outro espaço ou escreve no final.
         */
        long novoEndereco =
            gravarRegistro(novosDados);

        /*
         * O ID continua o mesmo, mas seu endereço mudou.
         */
        hash.update(
            id,
            novoEndereco
        );

        return true;
    }

    // =========================================================
    // DELETE
    // =========================================================

    public boolean delete(long id) throws Exception {

        long endereco = hash.read(id);

        if (endereco == -1) {
            return false;
        }

        arquivo.seek(endereco);

        byte lapide = arquivo.readByte();

        if (lapide != ' ') {
            return false;
        }

        // Marca como excluído
        arquivo.seek(endereco);

        arquivo.writeByte('*');

        // Adiciona à lista de espaços disponíveis
        adicionarExcluido(endereco);

        // Remove do índice Hash
        hash.delete(id);

        return true;
    }

    // =========================================================
    // READ ALL
    // =========================================================

    public ArrayList<T> readAll() throws Exception {

        ArrayList<T> registros =
            new ArrayList<>();

        long endereco =
            TAM_CABECALHO;

        while (endereco < arquivo.length()) {

            arquivo.seek(endereco);

            byte lapide =
                arquivo.readByte();

            int tamanho =
                arquivo.readUnsignedShort();

            if (lapide == ' ') {

                byte[] dados =
                    new byte[tamanho];

                arquivo.readFully(dados);

                T obj =
                    construtor.newInstance();

                obj.fromByteArray(dados);

                registros.add(obj);
            }

            /*
             * Vai diretamente para o próximo registro.
             *
             * endereço
             * + 1 byte da lápide
             * + 2 bytes do tamanho
             * + tamanho do payload
             */
            endereco +=
                TAM_CABECALHO_REGISTRO
                + tamanho;
        }

        return registros;
    }

    // =========================================================
    // GRAVAÇÃO DE REGISTRO
    // =========================================================

    private long gravarRegistro(
        byte[] dados
    ) throws Exception {

        /*
         * Primeiro procura um espaço deixado por um registro
         * excluído.
         */
        long endereco =
            getExcluido(dados.length);

        // =====================================================
        // NÃO HÁ ESPAÇO REUTILIZÁVEL
        // =====================================================

        if (endereco == -1) {

            endereco =
                arquivo.length();

            arquivo.seek(endereco);

            arquivo.writeByte(' ');

            arquivo.writeShort(
                dados.length
            );

            arquivo.write(dados);

            return endereco;
        }

        // =====================================================
        // REUTILIZA ESPAÇO EXCLUÍDO
        // =====================================================

        arquivo.seek(
            endereco + 1
        );

        int tamanhoEspaco =
            arquivo.readUnsignedShort();

        arquivo.seek(endereco);

        arquivo.writeByte(' ');

        /*
         * Mantemos o tamanho físico original do espaço.
         *
         * Isso é necessário porque o próximo registro começa
         * somente depois desse espaço inteiro.
         */
        arquivo.writeShort(
            tamanhoEspaco
        );

        arquivo.write(dados);

        // Limpa o espaço que sobrou
        for (int i = dados.length;
                i < tamanhoEspaco;
                i++) {

            arquivo.writeByte(0);
        }

        return endereco;
    }

    // =========================================================
    // LISTA DE EXCLUÍDOS
    // =========================================================

    private void adicionarExcluido(
        long endereco
    ) throws Exception {

        arquivo.seek(
            endereco + 1
        );

        int tamanho =
            arquivo.readUnsignedShort();

        /*
         * Precisamos de 8 bytes do payload para armazenar
         * o ponteiro para o próximo registro excluído.
         */
        if (tamanho < Long.BYTES) {
            return;
        }

        arquivo.seek(
            POS_LISTA_EXCLUIDOS
        );

        long primeiro =
            arquivo.readLong();

        /*
         * Dentro dos primeiros 8 bytes do payload do registro
         * excluído guardamos o endereço do próximo nó.
         */
        arquivo.seek(
            endereco
            + TAM_CABECALHO_REGISTRO
        );

        arquivo.writeLong(primeiro);

        /*
         * Este registro se torna o primeiro da lista.
         */
        arquivo.seek(
            POS_LISTA_EXCLUIDOS
        );

        arquivo.writeLong(endereco);
    }

    private long getExcluido(
        int tamanhoNecessario
    ) throws Exception {

        arquivo.seek(
            POS_LISTA_EXCLUIDOS
        );

        long atual =
            arquivo.readLong();

        long anterior = -1;

        while (atual != -1) {

            arquivo.seek(atual);

            byte lapide =
                arquivo.readByte();

            int tamanho =
                arquivo.readUnsignedShort();

            arquivo.seek(
                atual
                + TAM_CABECALHO_REGISTRO
            );

            long proximo =
                arquivo.readLong();

            /*
             * O espaço serve para o novo registro.
             */
            if (lapide == '*'
                    && tamanho >= tamanhoNecessario) {

                /*
                 * Remove o registro da lista encadeada.
                 */
                if (anterior == -1) {

                    // Era o primeiro nó
                    arquivo.seek(
                        POS_LISTA_EXCLUIDOS
                    );

                    arquivo.writeLong(
                        proximo
                    );

                } else {

                    arquivo.seek(
                        anterior
                        + TAM_CABECALHO_REGISTRO
                    );

                    arquivo.writeLong(
                        proximo
                    );
                }

                return atual;
            }

            anterior = atual;

            atual = proximo;
        }

        return -1;
    }

    // =========================================================
    // RECONSTRUIR HASH
    // =========================================================

    private void reconstruirIndice()
            throws Exception {

        long endereco =
            TAM_CABECALHO;

        while (endereco < arquivo.length()) {

            arquivo.seek(endereco);

            byte lapide =
                arquivo.readByte();

            int tamanho =
                arquivo.readUnsignedShort();

            if (lapide == ' ') {

                byte[] dados =
                    new byte[tamanho];

                arquivo.readFully(dados);

                T obj =
                    construtor.newInstance();

                obj.fromByteArray(dados);

                hash.create(
                    obj.getId(),
                    endereco
                );
            }

            endereco +=
                TAM_CABECALHO_REGISTRO
                + tamanho;
        }
    }

    // =========================================================
    // VERIFICAÇÃO DO TAMANHO
    // =========================================================

    private void verificarTamanho(
        byte[] dados
    ) throws IOException {

        /*
         * O tamanho do registro é armazenado utilizando
         * 2 bytes no arquivo.
         */
        if (dados.length > 65535) {

            throw new IOException(
                "Registro muito grande. "
                + "Tamanho maximo: 65535 bytes."
            );
        }
    }

    // =========================================================
    // CLOSE
    // =========================================================

    public void close() throws Exception {

        if (hash != null) {
            hash.close();
        }

        if (arquivo != null) {
            arquivo.close();
        }
    }
}