import entidades.Jogo;
import persistencia.Arquivo;
import persistencia.ArquivoJogoDesenvolvedora;
import persistencia.Registro;
import persistencia.RelacionamentoJogoDesenvolvedora;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Executar em diretório temporário próprio. Nenhum CRUD de Desenvolvedora é implementado aqui. */
public class RelacionamentoJogoDesenvolvedoraTest {
    /** Apenas um registro persistível de teste, enquanto a entidade do colega não existe. */
    public static class RegistroTeste implements Registro {
        private long id;
        public RegistroTeste() { }
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public byte[] toByteArray() { return ByteBuffer.allocate(8).putLong(id).array(); }
        public void fromByteArray(byte[] dados) { id = ByteBuffer.wrap(dados).getLong(); }
    }

    private static void verificar(boolean valor, String mensagem) {
        if (!valor) throw new AssertionError(mensagem);
    }

    private static void recusar(RelacionamentoJogoDesenvolvedora relacionamento,
            long jogo, long desenvolvedora, String mensagem) throws Exception {
        try {
            relacionamento.associar(jogo, desenvolvedora);
            throw new AssertionError("Aceitou associação inválida");
        } catch (IllegalArgumentException esperado) {
            verificar(esperado.getMessage().contains(mensagem), "Motivo da recusa");
        }
    }

    public static void main(String[] args) throws Exception {
        Arquivo<Jogo> jogos = new Arquivo<>("jogos_teste_integracao", Jogo.class.getConstructor());
        Arquivo<RegistroTeste> desenvolvedoras = new Arquivo<>("desenvolvedoras_teste_integracao",
            RegistroTeste.class.getConstructor());
        try (ArquivoJogoDesenvolvedora vinculos = new ArquivoJogoDesenvolvedora()) {
            verificar(vinculos.readAll().isEmpty(), "Exige base temporária vazia");
            RelacionamentoJogoDesenvolvedora relacionamento =
                new RelacionamentoJogoDesenvolvedora(jogos, desenvolvedoras, vinculos);
            long jogo = jogos.create(new Jogo());
            long dev = desenvolvedoras.create(new RegistroTeste());
            recusar(relacionamento, 999, dev, "Jogo inexistente");
            recusar(relacionamento, jogo, 999, "Desenvolvedora inexistente");
            recusar(relacionamento, 0, dev, "positivos");
            verificar(vinculos.readAll().isEmpty(), "Recusas não gravam vínculos");
            verificar(relacionamento.associar(jogo, dev), "Entidades existentes");
            verificar(vinculos.read(jogo, dev).getId() == 1, "Recusas não consomem ID");
            verificar(!relacionamento.associar(jogo, dev), "Duplicata");
            verificar(relacionamento.desenvolvedorasDoJogo(jogo).equals(Arrays.asList(dev)), "Consulta por jogo");
            verificar(relacionamento.jogosDaDesenvolvedora(dev).equals(Arrays.asList(jogo)), "Consulta por desenvolvedora");
            verificar(relacionamento.desassociar(jogo, dev), "Desassociar");
            desenvolvedoras.delete(dev);
            recusar(relacionamento, jogo, dev, "Desenvolvedora inexistente");
            long outraDev = desenvolvedoras.create(new RegistroTeste());
            jogos.delete(jogo);
            recusar(relacionamento, jogo, outraDev, "Jogo inexistente");
            verificar(vinculos.readAll().isEmpty(), "Entidades excluídas não criam vínculos");
        } finally {
            try { jogos.close(); } finally { desenvolvedoras.close(); }
        }
        System.out.println("OK: integracao com arquivos, entidades ausentes/excluidas, duplicata e consultas.");
    }
}
