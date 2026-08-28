package persistencia;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class Arquivo<T extends Registro> {

    // Tamanho do cabeçalho: 8 bytes para o último ID + 8 bytes para a lista de
    // excluídos
    private static final int TAM_CABECALHO = 16;

    private RandomAccessFile arquivo;
    private String nomeArquivo;
    private Constructor<T> construtor;

    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        // Cria o diretório principal de dados, caso não exista
        File diretorio = new File("./dados");
        if (!diretorio.exists())
            diretorio.mkdir();

        // Cria um diretório específico para a entidade
        diretorio = new File("./dados/" + nomeArquivo);
        if (!diretorio.exists())
            diretorio.mkdir();

        this.nomeArquivo = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".db";
        this.construtor = construtor;
        this.arquivo = new RandomAccessFile(this.nomeArquivo, "rw");

        // Se o arquivo ainda não possui cabeçalho, cria um novo
        if (arquivo.length() < TAM_CABECALHO) {
            arquivo.writeLong(0); // Último ID usado
            arquivo.writeLong(-1); // Lista de registros excluídos
        }
    }

    public long create(T obj) throws Exception {
        // Gera um novo ID automaticamente
        arquivo.seek(0);
        long novoID = arquivo.readLong() + 1;

        arquivo.seek(0);
        arquivo.writeLong(novoID);

        obj.setId(novoID);
        byte[] dados = obj.toByteArray();

        // Procura um espaço de registro excluído que possa ser reutilizado
        long endereco = getDeleted(dados.length);

        if (endereco == -1) {
            // Não encontrou espaço disponível: adiciona o registro no final do arquivo
            arquivo.seek(arquivo.length());
            endereco = arquivo.getFilePointer();

            arquivo.writeByte(' '); // Lápide: registro ativo
            arquivo.writeShort(dados.length);
            arquivo.write(dados);

        } else {
            // Reutiliza o espaço de um registro excluído
            arquivo.seek(endereco);
            arquivo.writeByte(' '); // Remove a lápide
            arquivo.skipBytes(2);
            arquivo.write(dados);
        }

        return obj.getId();
    }

    public T read(long id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();

            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            // Só considera registros que não foram excluídos
            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == id) {
                    return obj;
                }
            }
        }

        return null;
    }

    public ArrayList<T> readAll() throws Exception {
        ArrayList<T> registros = new ArrayList<>();

        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            // Só adiciona registros ativos
            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                registros.add(obj);
            }
        }

        return registros;
    }

    public boolean delete(long id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();

            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == id) {
                    // Marca o registro como excluído sem apagar seus dados
                    arquivo.seek(posicao);
                    arquivo.writeByte('*');

                    // Adiciona o espaço à lista de registros excluídos
                    addDeleted(tamanho, posicao);

                    return true;
                }
            }
        }

        return false;
    }

    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();

            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == novoObj.getId()) {
                    byte[] novosDados = novoObj.toByteArray();
                    short novoTam = (short) novosDados.length;

                    if (novoTam <= tamanho) {
                        // O novo registro cabe no espaço atual
                        arquivo.seek(posicao + 3);
                        arquivo.write(novosDados);

                    } else {
                        // O novo registro é maior: exclui o antigo
                        arquivo.seek(posicao);
                        arquivo.writeByte('*');
                        addDeleted(tamanho, posicao);

                        // Tenta reutilizar outro espaço excluído
                        long novoEndereco = getDeleted(novosDados.length);

                        if (novoEndereco == -1) {
                            // Não encontrou espaço: grava no final do arquivo
                            arquivo.seek(arquivo.length());
                            arquivo.writeByte(' ');
                            arquivo.writeShort(novoTam);
                            arquivo.write(novosDados);

                        } else {
                            // Reutiliza um espaço excluído
                            arquivo.seek(novoEndereco);
                            arquivo.writeByte(' ');
                            arquivo.skipBytes(2);
                            arquivo.write(novosDados);
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private void addDeleted(int tamanhoEspaco, long enderecoEspaco) throws Exception {
        // Posição no cabeçalho que guarda o primeiro registro excluído
        long posicao = 8;

        arquivo.seek(posicao);
        long endereco = arquivo.readLong();
        long proximo;

        if (endereco == -1) {
            // A lista de excluídos está vazia
            arquivo.seek(8);
            arquivo.writeLong(enderecoEspaco);

            // O novo registro excluído será o último da lista
            arquivo.seek(enderecoEspaco + 3);
            arquivo.writeLong(-1);

        } else {
            // Percorre a lista de registros excluídos
            do {
                arquivo.seek(endereco + 1);
                int tamanho = arquivo.readShort();
                proximo = arquivo.readLong();

                if (tamanho > tamanhoEspaco) {
                    // Insere o novo espaço antes do registro atual
                    if (posicao == 8)
                        arquivo.seek(posicao);
                    else
                        arquivo.seek(posicao + 3);

                    arquivo.writeLong(enderecoEspaco);

                    arquivo.seek(enderecoEspaco + 3);
                    arquivo.writeLong(endereco);

                    break;
                }

                if (proximo == -1) {
                    // Chegou ao final da lista
                    arquivo.seek(endereco + 3);
                    arquivo.writeLong(enderecoEspaco);

                    arquivo.seek(enderecoEspaco + 3);
                    arquivo.writeLong(-1);

                    break;
                }

                posicao = endereco;
                endereco = proximo;

            } while (endereco != -1);
        }
    }

    private long getDeleted(int tamanhoNecessario) throws Exception {
        // Posição no cabeçalho que guarda o primeiro registro excluído
        long posicao = 8;

        arquivo.seek(posicao);
        long endereco = arquivo.readLong();

        long proximo;
        int tamanho;

        // Percorre a lista procurando um espaço adequado
        while (endereco != -1) {
            arquivo.seek(endereco + 1);

            tamanho = arquivo.readShort();
            proximo = arquivo.readLong();

            if (tamanho > tamanhoNecessario) {
                // Remove o espaço encontrado da lista de excluídos
                if (posicao == 8)
                    arquivo.seek(posicao);
                else
                    arquivo.seek(posicao + 3);

                arquivo.writeLong(proximo);

                return endereco;
            }

            posicao = endereco;
            endereco = proximo;
        }

        // Nenhum espaço adequado foi encontrado
        return -1;
    }

    public void close() throws Exception {
        arquivo.close();
    }
}