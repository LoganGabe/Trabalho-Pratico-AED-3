package interfacegrafica;

import entidades.Desenvolvedora;
import java.awt.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Lista de vínculos em edição; só altera a persistência quando o formulário é salvo. */
public class SeletorDesenvolvedoras extends JPanel {
    private final List<Desenvolvedora> disponiveis;
    private final Set<Long> selecionadas = new LinkedHashSet<>();
    private final JPanel linhas = new JPanel();
    private final JButton adicionar = new JButton("+");

    public SeletorDesenvolvedoras(List<Desenvolvedora> disponiveis, List<Long> atuais) {
        super(new BorderLayout(8, 0));
        this.disponiveis = new ArrayList<>(disponiveis);
        selecionadas.addAll(atuais);
        linhas.setLayout(new BoxLayout(linhas, BoxLayout.Y_AXIS));
        JScrollPane rolagem = new JScrollPane(linhas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rolagem.setPreferredSize(new Dimension(330, 104));
        rolagem.getVerticalScrollBar().setUnitIncrement(28);
        add(rolagem, BorderLayout.CENTER);
        adicionar.setToolTipText("Adicionar desenvolvedoras");
        adicionar.getAccessibleContext().setAccessibleName("Adicionar desenvolvedoras");
        adicionar.addActionListener(e -> abrirPesquisa());
        JPanel acoes = new JPanel(new BorderLayout());
        acoes.add(adicionar, BorderLayout.NORTH);
        add(acoes, BorderLayout.EAST);
        atualizarLinhas();
    }

    public List<Long> getSelecionadas() { return new ArrayList<>(selecionadas); }

    private void atualizarLinhas() {
        linhas.removeAll();
        for (long id : selecionadas) {
            String nome = "Desenvolvedora não encontrada";
            for (Desenvolvedora dev : disponiveis) {
                if (dev.getId() == id) { nome = dev.getNome(); break; }
            }
            JPanel linha = new JPanel(new BorderLayout(8, 0));
            linha.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 8));
            JButton remover = new JButton("−");
            remover.setToolTipText("Remover " + nome);
            remover.getAccessibleContext().setAccessibleName("Remover " + nome);
            remover.addActionListener(e -> { selecionadas.remove(id); atualizarLinhas(); });
            linha.add(remover, BorderLayout.WEST);
            linha.add(new JLabel(nome), BorderLayout.CENTER);
            linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, linha.getPreferredSize().height));
            linhas.add(linha);
        }
        if (selecionadas.isEmpty()) {
            JLabel vazio = new JLabel(disponiveis.isEmpty()
                ? "Cadastre uma desenvolvedora para adicioná-la."
                : "Nenhuma desenvolvedora adicionada.");
            vazio.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            linhas.add(vazio);
        }
        adicionar.setEnabled(disponiveis.stream().anyMatch(d -> !selecionadas.contains(d.getId())));
        linhas.revalidate();
        linhas.repaint();
    }

    private void abrirPesquisa() {
        JTextField pesquisa = new JTextField(28);
        pesquisa.getAccessibleContext().setAccessibleName("Pesquisar por nome");
        DefaultListModel<Desenvolvedora> modelo = new DefaultListModel<>();
        JList<Desenvolvedora> lista = new JList<>(modelo);
        lista.setVisibleRowCount(8);
        lista.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> origem, Object valor,
                    int indice, boolean selecionado, boolean foco) {
                Desenvolvedora dev = (Desenvolvedora) valor;
                return super.getListCellRendererComponent(origem,
                    dev.getNome(), indice, selecionado, foco);
            }
        });
        JLabel quantidade = new JLabel();
        Runnable filtrar = () -> {
            String termo = normalizar(pesquisa.getText().trim());
            modelo.clear();
            for (Desenvolvedora dev : disponiveis) {
                if (!selecionadas.contains(dev.getId())
                        && normalizar(dev.getNome()).contains(termo)) modelo.addElement(dev);
            }
            quantidade.setText(modelo.isEmpty() ? "Nenhuma desenvolvedora encontrada."
                : "Selecione uma ou mais (Ctrl + clique) e confirme.");
        };
        pesquisa.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar.run(); }
            public void removeUpdate(DocumentEvent e) { filtrar.run(); }
            public void changedUpdate(DocumentEvent e) { filtrar.run(); }
        });
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.add(new JLabel("Pesquisar por nome"), BorderLayout.NORTH);
        topo.add(pesquisa, BorderLayout.CENTER);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(lista), BorderLayout.CENTER);
        painel.add(quantidade, BorderLayout.SOUTH);
        filtrar.run();
        int resposta = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), painel,
            "Adicionar desenvolvedoras", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resposta == JOptionPane.OK_OPTION) {
            for (Desenvolvedora dev : lista.getSelectedValuesList()) selecionadas.add(dev.getId());
            atualizarLinhas();
        }
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
