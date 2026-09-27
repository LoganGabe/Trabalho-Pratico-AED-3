import persistencia.ArvoreBMais;
import java.util.ArrayList;

public class TesteArvoreB {
    public static void main(String[] args) {
        try {
            System.out.println("A inicializar a Arvore B+ (Teste Extremo)...");
            
            // APAGA O FICHEIRO ANTIGO ANTES DE COMEÇAR
            new java.io.File("./dados/indices/teste_extremo.db").delete(); 
            
            ArvoreBMais arvore = new ArvoreBMais(5, "./dados/indices/teste_extremo.db");

            System.out.println("A inserir 10 jogos na Publicadora 10...");
            for(int i = 1; i <= 10; i++) {
                arvore.create(10, i); // Insere os jogos de ID 1 a 10
            }

            System.out.println("\n--- Teste Extremo de Leitura ---");
            ArrayList<Long> jogosPub10 = arvore.read(10);
            System.out.println("IDs dos Jogos da Publicadora 10: " + jogosPub10);
            // Deve imprimir exatamente: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

        } catch (Exception e) {
            System.out.println("Ocorreu um erro durante o teste:");
            e.printStackTrace();
        }
    }
}