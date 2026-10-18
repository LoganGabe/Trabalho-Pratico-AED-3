import interfacegrafica.Catalogo;
import interfacegrafica.JanelaPrincipal;
import java.awt.GraphicsEnvironment;
import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("Uso: Main (interface gráfica)");
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("A aplicação requer um ambiente gráfico.");
        }
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        // Inicialização fora da EDT; a thread principal mantém a JVM viva até abrir a janela.
        Catalogo catalogo = new Catalogo();
        SwingUtilities.invokeLater(() -> {
            try { new JanelaPrincipal(catalogo).setVisible(true); }
            catch (Exception e) {
                try { catalogo.close(); } catch (Exception fechamento) { e.addSuppressed(fechamento); }
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erro ao abrir catálogo", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
