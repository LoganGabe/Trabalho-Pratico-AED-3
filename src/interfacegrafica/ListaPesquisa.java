package interfacegrafica;

import java.awt.*;
import java.text.Normalizer;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Consulta independente, com rolagem, ordenação e filtro local dos resultados. */
public class ListaPesquisa extends JDialog {
    public ListaPesquisa(Window origem, String titulo, DefaultTableModel modelo) {
        super(origem, titulo, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JTextField pesquisa = new JTextField(30);
        pesquisa.getAccessibleContext().setAccessibleName("Pesquisar na lista");
        JPanel topo = new JPanel(new BorderLayout(8, 0));
        topo.add(new JLabel("Pesquisar"), BorderLayout.WEST);
        topo.add(pesquisa, BorderLayout.CENTER);
        painel.add(topo, BorderLayout.NORTH);
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(27);
        tabela.setFillsViewportHeight(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> ordenador = new TableRowSorter<>(modelo);
        tabela.setRowSorter(ordenador);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        JLabel quantidade = new JLabel();
        Runnable filtrar = () -> {
            String termo = normalizar(pesquisa.getText().trim());
            ordenador.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> linha) {
                    for (int i = 0; i < linha.getValueCount(); i++) {
                        if (normalizar(linha.getStringValue(i)).contains(termo)) return true;
                    }
                    return false;
                }
            });
            quantidade.setText(modelo.getRowCount() == 0 ? "Nenhum registro associado."
                : tabela.getRowCount() == 0 ? "Nenhum resultado para esta pesquisa."
                : tabela.getRowCount() + " de " + modelo.getRowCount() + " registro(s)");
        };
        pesquisa.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar.run(); }
            public void removeUpdate(DocumentEvent e) { filtrar.run(); }
            public void changedUpdate(DocumentEvent e) { filtrar.run(); }
        });
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(quantidade, BorderLayout.WEST);
        rodape.add(fechar, BorderLayout.EAST);
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);
        setSize(900, 440);
        setMinimumSize(new Dimension(640, 320));
        setLocationRelativeTo(origem);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        filtrar.run();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
