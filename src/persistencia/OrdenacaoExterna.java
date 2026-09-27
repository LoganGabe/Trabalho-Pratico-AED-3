package persistencia;

import entidades.Jogo;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;

public class OrdenacaoExterna {
    private String caminhoArquivo;
    private int capacidadeMemoria; // Quantos jogos cabem na RAM simulada

    public OrdenacaoExterna(String caminhoArquivo, int capacidadeMemoria) {
        this.caminhoArquivo = caminhoArquivo;
        this.capacidadeMemoria = capacidadeMemoria;
    }

    public void ordenarPorNome() throws Exception {
        System.out.println("A iniciar a Fase 1: Distribuicao de blocos...");
        
        RandomAccessFile arqOrigem = new RandomAccessFile(caminhoArquivo, "r");
        
        // Cria ficheiros temporários na mesma pasta dos jogos
        RandomAccessFile temp1 = new RandomAccessFile("./dados/jogos/temp1.db", "rw");
        RandomAccessFile temp2 = new RandomAccessFile("./dados/jogos/temp2.db", "rw");
        
        // Limpa os ficheiros temporários se já existirem de testes anteriores
        temp1.setLength(0);
        temp2.setLength(0);

        // Pula o cabeçalho de 16 bytes padrao da sua classe Arquivo<T>
        arqOrigem.seek(16); 

        ArrayList<Jogo> bloco = new ArrayList<>();
        boolean alternarArquivo = true; // Para alternar as escritas entre temp1 e temp2

        // Percorre o ficheiro original sequencialmente
        while (arqOrigem.getFilePointer() < arqOrigem.length()) {
            byte lapide = arqOrigem.readByte();
            short tamanho = arqOrigem.readShort();
            byte[] bytes = new byte[tamanho];
            arqOrigem.read(bytes);

            // Só processa se o registo não estiver excluído
            if (lapide != '*') {
                Jogo jogo = new Jogo();
                jogo.fromByteArray(bytes);
                bloco.add(jogo);
            }

            // Se o nosso limite de memória encheu OU se chegámos ao fim do ficheiro
            if (bloco.size() == capacidadeMemoria || arqOrigem.getFilePointer() == arqOrigem.length()) {
                if (!bloco.isEmpty()) {
                    // Ordena o bloco em memória pelo NOME (Ordem Alfabética)
                    bloco.sort(Comparator.comparing(Jogo::getNome));

                    // Decide para qual ficheiro temporário vai escrever
                    RandomAccessFile tempDestino = alternarArquivo ? temp1 : temp2;
                    
                    for (Jogo j : bloco) {
                        byte[] jBytes = j.toByteArray();
                        tempDestino.writeByte(' '); // Grava a lápide ativa
                        tempDestino.writeShort(jBytes.length); // Grava o tamanho
                        tempDestino.write(jBytes); // Grava o payload
                    }
                    
                    bloco.clear(); // Esvazia a RAM
                    alternarArquivo = !alternarArquivo; // Alterna o ficheiro para o próximo bloco
                }
            }
        }

        arqOrigem.close();
        temp1.close();
        temp2.close();
        
        System.out.println("Fase 1 (Distribuicao) concluida com sucesso! Blocos gerados em temp1.db e temp2.db");
    }

    public void intercalar() throws Exception {
        System.out.println("A iniciar a Fase 2: Intercalacao...");
        
        RandomAccessFile temp1 = new RandomAccessFile("./dados/jogos/temp1.db", "r");
        RandomAccessFile temp2 = new RandomAccessFile("./dados/jogos/temp2.db", "r");
        
        // Ficheiro final que vai receber a junção ordenada
        RandomAccessFile temp3 = new RandomAccessFile("./dados/jogos/temp3.db", "rw"); 
        temp3.setLength(0); // Limpa o ficheiro se já existir

        // Lê o primeiro jogo de cada ficheiro
        Jogo j1 = lerProximoJogo(temp1);
        Jogo j2 = lerProximoJogo(temp2);

        // Enquanto houver jogos em AMBOS os ficheiros, compara-os
        while (j1 != null && j2 != null) {
            // Se j1 for alfabeticamente menor ou igual a j2
            if (j1.getNome().compareToIgnoreCase(j2.getNome()) <= 0) {
                escreverJogo(temp3, j1);
                j1 = lerProximoJogo(temp1); // Puxa o próximo do temp1
            } else {
                escreverJogo(temp3, j2);
                j2 = lerProximoJogo(temp2); // Puxa o próximo do temp2
            }
        }

        // Se o temp2 acabou, mas ainda há jogos no temp1, descarrega o resto
        while (j1 != null) {
            escreverJogo(temp3, j1);
            j1 = lerProximoJogo(temp1);
        }

        // Se o temp1 acabou, mas ainda há jogos no temp2, descarrega o resto
        while (j2 != null) {
            escreverJogo(temp3, j2);
            j2 = lerProximoJogo(temp2);
        }

        temp1.close();
        temp2.close();
        temp3.close();
        
        System.out.println("Fase 2 (Intercalacao) concluida! Arquivo temp3.db gerado com os dados unificados e ordenados.");
    }

    // Método auxiliar para ler um registo (ignorando os excluídos)
    private Jogo lerProximoJogo(RandomAccessFile arq) throws Exception {
        if (arq.getFilePointer() == arq.length()) return null; // Fim do ficheiro
        
        byte lapide = arq.readByte();
        short tamanho = arq.readShort();
        byte[] bytes = new byte[tamanho];
        arq.read(bytes);
        
        if (lapide != '*') {
            Jogo j = new Jogo();
            j.fromByteArray(bytes);
            return j;
        }
        return lerProximoJogo(arq); // Se for lápide excluída, chama-se a si próprio para pular para o próximo
    }

    // Método auxiliar para escrever um registo
    private void escreverJogo(RandomAccessFile arq, Jogo j) throws Exception {
        byte[] bytes = j.toByteArray();
        arq.writeByte(' ');
        arq.writeShort(bytes.length);
        arq.write(bytes);
    }

    public void finalizar() throws Exception {
        System.out.println("A iniciar a Fase 3: Substituicao do arquivo e limpeza...");
        
        // 1. Ler o último ID do arquivo original para não perdermos a contagem
        RandomAccessFile arqOrigem = new RandomAccessFile(caminhoArquivo, "r");
        long ultimoId = arqOrigem.readLong();
        arqOrigem.close();

        // 2. Apagar o arquivo original
        java.io.File original = new java.io.File(caminhoArquivo);
        original.delete();

        // 3. Criar o novo arquivo original e escrever o cabeçalho
        RandomAccessFile novoArq = new RandomAccessFile(caminhoArquivo, "rw");
        novoArq.writeLong(ultimoId); // Mantém o gerador de IDs intacto
        novoArq.writeLong(-1);       // Zera a lista de excluídos (já limpámos tudo no merge)

        // 4. Copiar os dados ordenados do temp3 para o novo arquivo original
        RandomAccessFile temp3 = new RandomAccessFile("./dados/jogos/temp3.db", "r");
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = temp3.read(buffer)) != -1) {
            novoArq.write(buffer, 0, lidos);
        }
        
        temp3.close();
        novoArq.close();

        // 5. Apagar arquivos temporários
        new java.io.File("./dados/jogos/temp1.db").delete();
        new java.io.File("./dados/jogos/temp2.db").delete();
        new java.io.File("./dados/jogos/temp3.db").delete();

        // 6. Apagar os índices do Hash para forçar a reconstrução automática
        new java.io.File("./dados/indices/jogos.dir").delete();
        new java.io.File("./dados/indices/jogos.bkt").delete();

        System.out.println("Fase 3 concluida! Base de dados oficial atualizada e ordenada.");
    }
}