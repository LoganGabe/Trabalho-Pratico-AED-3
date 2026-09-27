package persistencia;

import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ArvoreBMais {
    private int ordem;
    private int maxChaves;
    private int maxFilhos;
    private RandomAccessFile arquivo;
    private long raiz; // Ponteiro (endereço) para o nó raiz no ficheiro
    
    private final int TAMANHO_CABECALHO = 8; // 8 bytes para guardar o endereço da raiz

    public ArvoreBMais(int ordem, String nomeArquivo) throws Exception {
        this.ordem = ordem;
        this.maxChaves = ordem - 1;
        this.maxFilhos = ordem;
        
        this.arquivo = new RandomAccessFile(nomeArquivo, "rw");
        
        if (this.arquivo.length() < TAMANHO_CABECALHO) {
            // Ficheiro novo: inicializa o cabeçalho e cria a raiz vazia
            inicializarArvore();
        } else {
            // Ficheiro existente: lê o endereço da raiz do cabeçalho
            this.arquivo.seek(0);
            this.raiz = this.arquivo.readLong();
        }
    }

    private void inicializarArvore() throws Exception {
        // Grava espaço para o cabeçalho (raiz)
        this.arquivo.seek(0);
        this.arquivo.writeLong(-1); 
        
        // Cria a primeira página (que é raiz e folha ao mesmo tempo)
        Pagina novaRaiz = new Pagina(true);
        this.raiz = gravarPagina(novaRaiz); // Grava no fim e obtém o endereço
        
        // Atualiza o cabeçalho com o endereço da nova raiz
        this.arquivo.seek(0);
        this.arquivo.writeLong(this.raiz);
    }

    /**
     * Classe interna para representar um Par / Chave Composta
     */
    class ChaveComposta {
        long publicadoraId;
        long jogoId;

        public ChaveComposta(long publicadoraId, long jogoId) {
            this.publicadoraId = publicadoraId;
            this.jogoId = jogoId;
        }
    }

    /**
     * Classe interna que representa um Nó/Página da Árvore B+
     */
    class Pagina {
        boolean folha;
        int quantidade; // Número de chaves atuais
        ChaveComposta[] chaves; 
        long[] ponteiros; 
        long irmaoAnterior; // Apenas para folha
        long irmaoSeguinte; // Apenas para folha

        public Pagina(boolean folha) {
            this.folha = folha;
            this.quantidade = 0;
            
            // maxChaves + 1 para suportar o overflow temporário antes do split
            this.chaves = new ChaveComposta[maxChaves + 1];
            this.ponteiros = new long[maxFilhos + 1];
            
            for (int i = 0; i < maxFilhos + 1; i++) ponteiros[i] = -1;
            this.irmaoAnterior = -1;
            this.irmaoSeguinte = -1;
        }

        // Lê a página a partir do ficheiro binário
        public void lerDoArquivo(RandomAccessFile arq) throws Exception {
            this.folha = arq.readBoolean();
            this.quantidade = arq.readInt();

            if (this.folha) {
                this.irmaoAnterior = arq.readLong();
                this.irmaoSeguinte = arq.readLong();
                for (int i = 0; i < maxChaves; i++) {
                    long pubId = arq.readLong();
                    long jgId = arq.readLong();
                    if (i < this.quantidade) {
                        this.chaves[i] = new ChaveComposta(pubId, jgId);
                    }
                }
            } else {
                this.ponteiros[0] = arq.readLong();
                for (int i = 0; i < maxChaves; i++) {
                    long pubId = arq.readLong();
                    long jgId = arq.readLong();
                    if (i < this.quantidade) {
                        this.chaves[i] = new ChaveComposta(pubId, jgId);
                    }
                    this.ponteiros[i + 1] = arq.readLong();
                }
            }
        }

        // Escreve a página no ficheiro binário
        public void escreverNoArquivo(RandomAccessFile arq) throws Exception {
            arq.writeBoolean(this.folha);
            arq.writeInt(this.quantidade);

            if (this.folha) {
                arq.writeLong(this.irmaoAnterior);
                arq.writeLong(this.irmaoSeguinte);
                for (int i = 0; i < maxChaves; i++) {
                    if (i < this.quantidade) {
                        arq.writeLong(this.chaves[i].publicadoraId);
                        arq.writeLong(this.chaves[i].jogoId);
                    } else {
                        arq.writeLong(-1); // Espaço vazio
                        arq.writeLong(-1);
                    }
                }
            } else {
                arq.writeLong(this.ponteiros[0]);
                for (int i = 0; i < maxChaves; i++) {
                    if (i < this.quantidade) {
                        arq.writeLong(this.chaves[i].publicadoraId);
                        arq.writeLong(this.chaves[i].jogoId);
                    } else {
                        arq.writeLong(-1);
                        arq.writeLong(-1);
                    }
                    arq.writeLong(this.ponteiros[i + 1]);
                }
            }
        }
    }

    // Métodos de acesso ao ficheiro
    private long gravarPagina(Pagina pagina) throws Exception {
        long endereco = this.arquivo.length();
        this.arquivo.seek(endereco);
        pagina.escreverNoArquivo(this.arquivo);
        return endereco;
    }

    private void atualizarPagina(long endereco, Pagina pagina) throws Exception {
        this.arquivo.seek(endereco);
        pagina.escreverNoArquivo(this.arquivo);
    }

    private Pagina lerPagina(long endereco) throws Exception {
        this.arquivo.seek(endereco);
        Pagina pagina = new Pagina(true); 
        pagina.lerDoArquivo(this.arquivo);
        return pagina;
    }

    // Método auxiliar de ordenação
    private int compararChaves(ChaveComposta c1, ChaveComposta c2) {
        if (c1.publicadoraId < c2.publicadoraId) return -1;
        if (c1.publicadoraId > c2.publicadoraId) return 1;
        if (c1.jogoId < c2.jogoId) return -1;
        if (c1.jogoId > c2.jogoId) return 1;
        return 0; 
    }

    /**
     * B+.read(publicadoraId) - Retorna a lista de jogos associados
     */
    public ArrayList<Long> read(long publicadoraId) throws Exception {
        ArrayList<Long> listaJogos = new ArrayList<>();
        if (this.raiz == -1) return listaJogos;

        long enderecoAtual = this.raiz;
        Pagina paginaAtual = lerPagina(enderecoAtual);

        while (!paginaAtual.folha) {
            int i = 0;
            while (i < paginaAtual.quantidade && publicadoraId > paginaAtual.chaves[i].publicadoraId) {
                i++;
            }
            enderecoAtual = paginaAtual.ponteiros[i];
            paginaAtual = lerPagina(enderecoAtual);
        }

        boolean continuar = true;
        while (continuar) {
            for (int i = 0; i < paginaAtual.quantidade; i++) {
                if (paginaAtual.chaves[i].publicadoraId == publicadoraId) {
                    listaJogos.add(paginaAtual.chaves[i].jogoId);
                } else if (paginaAtual.chaves[i].publicadoraId > publicadoraId) {
                    continuar = false; 
                    break;
                }
            }
            
            if (continuar && paginaAtual.irmaoSeguinte != -1) {
                paginaAtual = lerPagina(paginaAtual.irmaoSeguinte);
            } else {
                continuar = false;
            }
        }

        return listaJogos;
    }

    /**
     * B+.create(publicadoraId, idJogo) - Passos 1 e 2
     */
    public boolean create(long publicadoraId, long idJogo) throws Exception {
        ChaveComposta novaChave = new ChaveComposta(publicadoraId, idJogo);
        if (this.raiz == -1) return false; 

        java.util.Stack<Long> caminho = new java.util.Stack<>();
        long enderecoAtual = this.raiz;
        Pagina paginaAtual = lerPagina(enderecoAtual);

        // =========================================================
        // PASSO 1: Descer até à Folha
        // =========================================================
        
        while (!paginaAtual.folha) {
            caminho.push(enderecoAtual); 
            int i = 0;
            while (i < paginaAtual.quantidade && compararChaves(novaChave, paginaAtual.chaves[i]) >= 0) {
                i++;
            }
            enderecoAtual = paginaAtual.ponteiros[i];
            paginaAtual = lerPagina(enderecoAtual);
        }
        // =========================================================
        // PASSO 2: Inserção Ordenada na Folha
        // =========================================================
        int pos = paginaAtual.quantidade - 1;
        while (pos >= 0 && compararChaves(novaChave, paginaAtual.chaves[pos]) < 0) {
            paginaAtual.chaves[pos + 1] = paginaAtual.chaves[pos];
            pos--;
        }
        
        paginaAtual.chaves[pos + 1] = novaChave;
        paginaAtual.quantidade++;

        atualizarPagina(enderecoAtual, paginaAtual);

       // =========================================================
        // PASSO 3: O Split (Divisão) e a Promoção
        // =========================================================
        long enderecoFilho = enderecoAtual;
        Pagina paginaFilho = paginaAtual;

        // Enquanto a página atual tiver mais chaves do que o permitido
        while (paginaFilho.quantidade > maxChaves) {
            Pagina novaPagina = new Pagina(paginaFilho.folha);
            int meio = paginaFilho.quantidade / 2;
            
            // Se for folha, a chave sobe e TAMBÉM fica na direita. 
            // Se for nó interno, a chave sobe e NÃO fica na direita.
            int inicioTransferencia = paginaFilho.folha ? meio : meio + 1;
            int j = 0;
            
            // Transfere as chaves e ponteiros da metade superior
            for (int k = inicioTransferencia; k < paginaFilho.quantidade; k++) {
                novaPagina.chaves[j] = paginaFilho.chaves[k];
                novaPagina.ponteiros[j] = paginaFilho.ponteiros[k];
                j++;
            }
            // Copia o último ponteiro que sobra à direita da última chave transferida
            novaPagina.ponteiros[j] = paginaFilho.ponteiros[paginaFilho.quantidade]; 
            novaPagina.quantidade = j;
            
            // A chave que será promovida ao pai
            ChaveComposta chavePromovida = paginaFilho.chaves[meio];
            
            // A página original fica apenas com a metade inferior
            paginaFilho.quantidade = meio; 
            
            long enderecoNovaPagina = -1;
            
            if (paginaFilho.folha) {
                // Atualiza a lista ligada das folhas (essencial para o read funcionar)
                novaPagina.irmaoSeguinte = paginaFilho.irmaoSeguinte;
                novaPagina.irmaoAnterior = enderecoFilho;
                enderecoNovaPagina = gravarPagina(novaPagina);
                
                paginaFilho.irmaoSeguinte = enderecoNovaPagina;
                
                if (novaPagina.irmaoSeguinte != -1) {
                    Pagina irmao = lerPagina(novaPagina.irmaoSeguinte);
                    irmao.irmaoAnterior = enderecoNovaPagina;
                    atualizarPagina(novaPagina.irmaoSeguinte, irmao);
                }
            } else {
                enderecoNovaPagina = gravarPagina(novaPagina);
            }
            
            atualizarPagina(enderecoFilho, paginaFilho);
            
            // =========================================================
            // PASSO 4: Inserir a chave no Nó Pai
            // =========================================================
            if (caminho.isEmpty()) {
                // Se a pilha de caminhos está vazia, a raiz transbordou. A árvore cresce em altura!
                Pagina novaRaiz = new Pagina(false);
                novaRaiz.chaves[0] = chavePromovida;
                novaRaiz.ponteiros[0] = enderecoFilho;
                novaRaiz.ponteiros[1] = enderecoNovaPagina;
                novaRaiz.quantidade = 1;
                
                this.raiz = gravarPagina(novaRaiz);
                this.arquivo.seek(0);
                this.arquivo.writeLong(this.raiz);
                break; // O split resolveu-se na nova raiz
            } else {
                // Retira o pai da pilha e insere lá a chave promovida
                long enderecoPai = caminho.pop();
                Pagina paginaPai = lerPagina(enderecoPai);
                
                int posPai = paginaPai.quantidade - 1;
                // Desloca chaves e ponteiros do pai para abrir espaço
                while (posPai >= 0 && compararChaves(chavePromovida, paginaPai.chaves[posPai]) < 0) {
                    paginaPai.chaves[posPai + 1] = paginaPai.chaves[posPai];
                    paginaPai.ponteiros[posPai + 2] = paginaPai.ponteiros[posPai + 1];
                    posPai--;
                }
                
                paginaPai.chaves[posPai + 1] = chavePromovida;
                paginaPai.ponteiros[posPai + 2] = enderecoNovaPagina;
                paginaPai.quantidade++;
                
                atualizarPagina(enderecoPai, paginaPai);
                
                // O nó pai passa a ser o nó em análise para a próxima repetição do ciclo `while`
                enderecoFilho = enderecoPai;
                paginaFilho = paginaPai;
            }
        }

        return true;

    }

    public boolean delete(long publicadoraId, long idJogo) throws Exception {
        return true;
    }
}