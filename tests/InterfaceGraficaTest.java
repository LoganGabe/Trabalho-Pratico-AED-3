import interfacegrafica.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/** Exercita ações reais dos componentes Swing; requer ambiente gráfico e base isolada. */
public class InterfaceGraficaTest {
    private static JanelaPrincipal janela;
    private static JTabbedPane abas;
    private static Catalogo catalogo;
    private static final List<Timer> timers = new ArrayList<>();
    private static void progresso(String texto) {
        if (Boolean.getBoolean("interface.debug")) System.out.println("Swing: " + texto);
    }
    private static void edt(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }
    private static <T extends Component> List<T> componentes(Container raiz, Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c)) lista.add(tipo.cast(c));
            if (c instanceof Container) lista.addAll(componentes((Container) c, tipo));
        }
        return lista;
    }
    private static Container painel() { return (Container) abas.getSelectedComponent(); }
    private static JButton botao(String texto) {
        return componentes(painel(), JButton.class).stream().filter(b -> texto.equals(b.getText())).findFirst().orElseThrow(() -> new AssertionError("Botão ausente: " + texto));
    }
    private static void clicar(String texto) throws Exception { progresso(texto); edt(() -> botao(texto).doClick()); aguardar(); }
    private static void aba(int indice) throws Exception { progresso("aba " + indice); edt(() -> abas.setSelectedIndex(indice)); progresso("aba selecionada " + indice); }
    private static void aguardar() throws Exception {
        long limite = System.currentTimeMillis() + 15000;
        AtomicReference<Boolean> pronto = new AtomicReference<>(false);
        while (System.currentTimeMillis() < limite) {
            edt(() -> {
                for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isVisible()) {
                    for (JOptionPane p : componentes((JDialog) w, JOptionPane.class)) {
                        if (p.getMessageType() == JOptionPane.ERROR_MESSAGE) {
                            Object mensagem = p.getMessage(); p.setValue(JOptionPane.OK_OPTION);
                            throw new AssertionError("Erro na interface: " + mensagem);
                        }
                    }
                }
                pronto.set(abas.isEnabled());
            });
            if (pronto.get()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Operação Swing excedeu 15 segundos");
    }
    private static void linhas(int esperado) throws Exception {
        progresso("verificar linhas " + esperado);
        edt(() -> {
            int atual = componentes(painel(), JTable.class).get(0).getRowCount();
            if (atual != esperado) throw new AssertionError("Linhas esperadas: " + esperado + "; obtidas: " + atual);
        });
        progresso("linhas verificadas");
    }
    private static void selecionar() throws Exception { edt(() -> componentes(painel(), JTable.class).get(0).setRowSelectionInterval(0, 0)); }
    private static void abrirJanela() throws Exception {
        catalogo = new Catalogo();
        edt(() -> {
            janela = new JanelaPrincipal(catalogo);
            abas = componentes(janela.getContentPane(), JTabbedPane.class).get(0);
            janela.setVisible(true);
        });
        aguardar();
    }
    private static void fecharJanela() throws Exception {
        edt(() -> janela.dispatchEvent(new WindowEvent(janela, WindowEvent.WINDOW_CLOSING)));
        aguardar();
        edt(() -> { if (janela.isDisplayable()) throw new AssertionError("Janela permaneceu aberta"); });
    }
    private static void dialogo(String botao, String... valores) throws Exception {
        progresso(botao);
        AtomicReference<Throwable> falha = new AtomicReference<>();
        edt(() -> {
            Timer timer = new Timer(40, null);
            timers.add(timer);
            timer.addActionListener(e -> {
                for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isVisible()) {
                    List<JOptionPane> paineis = componentes((JDialog) w, JOptionPane.class);
                    if (paineis.isEmpty()) continue;
                    timer.stop();
                    JOptionPane p = paineis.get(0);
                    try {
                        if (p.getWantsInput()) p.setInputValue(valores[0]);
                        else {
                            List<JTextField> campos = componentes(p, JTextField.class);
                            if (campos.size() != valores.length) throw new AssertionError("Quantidade de campos de " + botao + ": " + campos.size());
                            for (int i = 0; i < valores.length; i++) campos.get(i).setText(valores[i]);
                        }
                    } catch (Throwable erro) { falha.set(erro); }
                    p.setValue(JOptionPane.OK_OPTION);
                    return;
                }
            });
            timer.start();
            botao(botao).doClick();
        });
        if (falha.get() != null) throw new AssertionError(falha.get());
        aguardar();
    }
    private static void criarEditarConsultar(int indice, String[] criar, String[] editar) throws Exception {
        aba(indice); dialogo("Novo", criar); linhas(1);
        selecionar(); dialogo("Editar selecionado", editar); linhas(1);
        dialogo("Consultar ID", "1"); linhas(1);
        dialogo("Consultar ID", "999"); linhas(0);
        clicar("Listar todos / Atualizar"); linhas(1);
    }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Teste Swing exige ambiente gráfico.");
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        java.util.Timer limite = new java.util.Timer(true);
        limite.schedule(new java.util.TimerTask() {
            @Override public void run() {
                System.err.println("FAIL InterfaceGraficaTest: limite de 60 segundos excedido");
                for (java.util.Map.Entry<Thread, StackTraceElement[]> entrada : Thread.getAllStackTraces().entrySet()) {
                    System.err.println(entrada.getKey().getName());
                    for (StackTraceElement linha : entrada.getValue()) System.err.println("  " + linha);
                }
                System.exit(1);
            }
        }, 60000);
        try {
            abrirJanela();
            criarEditarConsultar(1, new String[]{"Publicadora", "Brasil", "2000", "Descrição"}, new String[]{"Publicadora editada", "Brasil", "2001", "Nova descrição"});
            criarEditarConsultar(2, new String[]{"Estúdio", "Brasil", "2000"}, new String[]{"Estúdio editado", "Japão", "2001"});
            criarEditarConsultar(0, new String[]{"Jogo", "19,90", "2024-01-01", "pt, en"}, new String[]{"Jogo editado", "29.90", "2024-02-01", "pt, en, es"});
            aba(3); clicar("Associar");
            clicar("Associar");
            edt(() -> {
                if (componentes(janela.getContentPane(), JLabel.class).stream().noneMatch(l -> l.getText().contains("já existe"))) throw new AssertionError("Duplicata sem retorno na tela");
            });
            clicar("Desenvolvedoras do jogo"); linhas(1);
            clicar("Jogos da desenvolvedora"); linhas(1);
            // Reabre a janela com dados e vínculo ativos, depois ordena e consulta novamente.
            fecharJanela();
            abrirJanela();
            aba(0); linhas(1);
            edt(() -> {
                JTable tabela = componentes(painel(), JTable.class).get(0);
                if (!"Jogo editado".equals(tabela.getValueAt(0, 1))
                        || !"pt, en, es".equals(tabela.getValueAt(0, 5))) {
                    throw new AssertionError("Campos do jogo não persistiram ao reabrir a janela");
                }
            });
            dialogo("Ordenar arquivo por nome"); linhas(1);
            aba(3); clicar("Desenvolvedoras do jogo"); linhas(1);
            clicar("Jogos da desenvolvedora"); linhas(1);
            aba(1); selecionar(); dialogo("Jogos da publicadora (1:N)");
            aba(3); dialogo("Remover associação");
            clicar("Desenvolvedoras do jogo"); linhas(0);
            clicar("Jogos da desenvolvedora"); linhas(0);
            for (int indice : new int[]{0, 2, 1}) {
                aba(indice); selecionar(); dialogo("Excluir selecionado"); linhas(0);
            }
            fecharJanela();
            try (Catalogo reaberto = new Catalogo()) {
                if (!reaberto.jogos.listar().isEmpty() || !reaberto.publicadoras.listar().isEmpty() || !reaberto.desenvolvedoras.listar().isEmpty()) throw new AssertionError("Exclusões não persistiram");
            }
            // Confere também a entrada real entregue ao usuário, sem driver de console.
            Main.main(new String[0]);
            edt(() -> {
                janela = null;
                for (Window w : Window.getWindows()) {
                    if (w instanceof JanelaPrincipal && w.isDisplayable()) janela = (JanelaPrincipal) w;
                }
                if (janela == null) throw new AssertionError("Main não abriu a interface gráfica");
                abas = componentes(janela.getContentPane(), JTabbedPane.class).get(0);
            });
            aguardar(); linhas(0); fecharJanela();
            System.out.println("PASS InterfaceGraficaTest: navegação, CRUDs, consulta ID, N:N, 1:N, ordenação e fechamento");
        } catch (Throwable falha) {
            falha.printStackTrace();
            throw falha;
        } finally {
            limite.cancel();
            edt(() -> { for (Timer timer : timers) timer.stop(); for (Window w : Window.getWindows()) w.dispose(); });
            if (catalogo != null) catalogo.close();
        }
    }
}
