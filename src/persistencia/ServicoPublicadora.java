package persistencia;

import entidades.Publicadora;
import java.util.ArrayList;

/** Regras compartilháveis de publicadoras, incluindo a proteção do relacionamento 1:N. */
public class ServicoPublicadora {
    private final Arquivo<Publicadora> arquivo;
    private final ServicoJogo jogos;

    public ServicoPublicadora(Arquivo<Publicadora> arquivo, ServicoJogo jogos) {
        this.arquivo = arquivo;
        this.jogos = jogos;
    }

    public ArrayList<Publicadora> listar() throws Exception { return arquivo.readAll(); }
    public Publicadora consultar(long id) throws Exception { return id > 0 ? arquivo.read(id) : null; }
    public long cadastrar(Publicadora p) throws Exception { validar(p); return arquivo.create(p); }
    public boolean atualizar(Publicadora p) throws Exception { validar(p); return arquivo.update(p); }
    public boolean excluir(long id) throws Exception {
        if (consultar(id) == null) return false;
        if (!jogos.listarPorPublicadora(id).isEmpty()) {
            throw new IllegalStateException("A publicadora possui jogos. Remova ou transfira os jogos primeiro.");
        }
        return arquivo.delete(id);
    }

    private void validar(Publicadora p) {
        if (p.getNome() == null || p.getNome().trim().isEmpty()
                || p.getPais() == null || p.getPais().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome e país são obrigatórios.");
        }
        if (p.getAnoFundacao() < 0) throw new IllegalArgumentException("Ano de fundação inválido.");
    }
}
