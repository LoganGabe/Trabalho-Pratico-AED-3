package interfacegrafica;

import entidades.*;
import persistencia.*;
import java.nio.file.*;

/** Uma sessão de arquivos e serviços. A interface serializa o acesso em uma única tarefa. */
public class Catalogo implements AutoCloseable {
    private Arquivo<Jogo> arquivoJogos;
    private Arquivo<Publicadora> arquivoPublicadoras;
    private Arquivo<Desenvolvedora> arquivoDesenvolvedoras;
    private ArvoreBMais indicePublicadora;
    private ArquivoJogoDesenvolvedora vinculos;
    public ServicoJogo jogos;
    public ServicoPublicadora publicadoras;
    public ServicoDesenvolvedora desenvolvedoras;
    public RelacionamentoJogoDesenvolvedora relacionamentos;

    public Catalogo() throws Exception {
        try {
            arquivoJogos = new Arquivo<>("jogos", Jogo.class.getConstructor());
            arquivoPublicadoras = new Arquivo<>("publicadoras", Publicadora.class.getConstructor());
            arquivoDesenvolvedoras = new Arquivo<>("desenvolvedoras", Desenvolvedora.class.getConstructor());
            Path indice = Path.of("dados/indices/arvore_pub_jogo.db");
            boolean reconstruir = !Files.exists(indice) || Files.size(indice) < 8;
            indicePublicadora = new ArvoreBMais(5, indice.toString());
            if (reconstruir) {
                for (Jogo jogo : arquivoJogos.readAll()) indicePublicadora.create(jogo.getPublicadoraId(), jogo.getId());
            }
            vinculos = new ArquivoJogoDesenvolvedora();
            conectar();
        } catch (Exception e) {
            try { close(); } catch (Exception fechamento) { e.addSuppressed(fechamento); }
            throw e;
        }
    }

    private void conectar() {
        jogos = new ServicoJogo(arquivoJogos, arquivoPublicadoras, indicePublicadora, vinculos);
        publicadoras = new ServicoPublicadora(arquivoPublicadoras, jogos);
        desenvolvedoras = new ServicoDesenvolvedora(arquivoDesenvolvedoras, vinculos);
        relacionamentos = new RelacionamentoJogoDesenvolvedora(arquivoJogos, arquivoDesenvolvedoras, vinculos);
    }

    public void ordenar() throws Exception {
        arquivoJogos.close();
        try {
            OrdenacaoExterna ordenacao = new OrdenacaoExterna("./dados/jogos/jogos.db", 3);
            ordenacao.ordenarPorNome();
            ordenacao.intercalar();
            ordenacao.finalizar();
        } finally {
            // Arquivo reconstrói o Hash; os serviços precisam receber a nova instância.
            arquivoJogos = new Arquivo<>("jogos", Jogo.class.getConstructor());
            conectar();
        }
    }

    @Override public void close() throws Exception {
        Exception falha = null;
        for (int i = 0; i < 5; i++) {
            try {
                switch (i) {
                    case 0: if (vinculos != null) vinculos.close(); break;
                    case 1: if (indicePublicadora != null) indicePublicadora.close(); break;
                    case 2: if (arquivoDesenvolvedoras != null) arquivoDesenvolvedoras.close(); break;
                    case 3: if (arquivoPublicadoras != null) arquivoPublicadoras.close(); break;
                    case 4: if (arquivoJogos != null) arquivoJogos.close(); break;
                    default: break;
                }
            } catch (Exception e) { if (falha == null) falha = e; else falha.addSuppressed(e); }
        }
        if (falha != null) throw falha;
    }
}
