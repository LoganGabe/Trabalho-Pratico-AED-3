import persistencia.OrdenacaoExterna;

public class TesteOrdenacao {
    public static void main(String[] args) {
        try {
            // Vamos testar com uma RAM minúscula que só suporta 3 jogos de cada vez
            int ramSimulada = 3;
            OrdenacaoExterna ordenacao = new OrdenacaoExterna("./dados/jogos/jogos.db", ramSimulada);
            
            ordenacao.ordenarPorNome();
            ordenacao.intercalar();
            ordenacao.finalizar();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}