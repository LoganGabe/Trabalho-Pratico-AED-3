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
    private static int[] selecionarDevs;
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
                            if (selecionarDevs != null) selecionarNoFormulario(p, selecionarDevs);
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
    private static void selecionarNoFormulario(JOptionPane formulario, int[] indices) {
        SeletorDesenvolvedoras seletor = componentes(formulario, SeletorDesenvolvedoras.class).get(0);
        int altura = seletor.getHeight();
        for (JTextField campo : componentes(formulario, JTextField.class)) {
            if (campo.getHeight() > 40) throw new AssertionError("Campo de texto esticado pela lista");
        }
        for (JButton botao : componentes(seletor, JButton.class)) {
            if ("−".equals(botao.getText())) botao.doClick();
        }
        if (indices.length == 0) return;
        AtomicReference<Throwable> falha = new AtomicReference<>();
        Timer pesquisa = new Timer(40, null);
        timers.add(pesquisa);
        pesquisa.addActionListener(e -> {
            for (Window w : Window.getWindows()) {
                if (!(w instanceof JDialog) || !w.isVisible()
                        || !"Adicionar desenvolvedoras".equals(((JDialog) w).getTitle())) continue;
                pesquisa.stop();
                JOptionPane painel = componentes((JDialog) w, JOptionPane.class).get(0);
                try {
                    JTextField busca = componentes(painel, JTextField.class).get(0);
                    JList<?> lista = componentes(painel, JList.class).get(0);
                    busca.setText("nenhum resultado xyz");
                    if (lista.getModel().getSize() != 0) throw new AssertionError("Pesquisa não filtrou");
                    busca.setText("ESTUDIO"); // Também deve encontrar Estúdio, sem diferenciar acentos.
                    for (int indice : indices) {
                        boolean encontrou = false;
                        for (int i = 0; i < lista.getModel().getSize(); i++) {
                            entidades.Desenvolvedora dev = (entidades.Desenvolvedora) lista.getModel().getElementAt(i);
                            if (dev.getId() == indice + 1L) {
                                lista.addSelectionInterval(i, i);
                                encontrou = true;
                            }
                        }
                        if (!encontrou) throw new AssertionError("Desenvolvedora ausente na pesquisa");
                    }
                } catch (Throwable erro) { falha.set(erro); }
                painel.setValue(JOptionPane.OK_OPTION);
                return;
            }
        });
        pesquisa.start();
        componentes(seletor, JButton.class).stream().filter(b -> "+".equals(b.getText())).findFirst().get().doClick();
        if (falha.get() != null) throw new AssertionError(falha.get());
        if (seletor.getSelecionadas().size() != indices.length) throw new AssertionError("Seleção incorreta");
        if (seletor.getHeight() != altura) throw new AssertionError("Lista expandiu o formulário");
    }

    private static void criarEditarConsultar(int indice, String[] criar, String[] editar) throws Exception {
        aba(indice); dialogo("Novo", criar); linhas(1);
        selecionar(); dialogo("Editar selecionado", editar); linhas(1);
        dialogo("Consultar ID", "1"); linhas(1);
        dialogo("Consultar ID", "999"); linhas(0);
        clicar("Listar todos / Atualizar"); linhas(1);
    }

    private static void consultarLista(String botao, int esperado, String termo) throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        AtomicReference<Boolean> concluido = new AtomicReference<>(false);
        edt(() -> {
            Timer timer = new Timer(40, null);
            timers.add(timer);
            timer.addActionListener(e -> {
                for (Window w : Window.getWindows()) {
                    if (!(w instanceof ListaPesquisa) || !w.isVisible()) continue;
                    timer.stop();
                    try {
                        JTable tabela = componentes((ListaPesquisa) w, JTable.class).get(0);
                        JTextField pesquisa = componentes((ListaPesquisa) w, JTextField.class).get(0);
                        if (tabela.getRowCount() != esperado) throw new AssertionError("Quantidade da consulta");
                        pesquisa.setText("nenhum resultado xyz [");
                        if (tabela.getRowCount() != 0) throw new AssertionError("Pesquisa sem resultados");
                        pesquisa.setText(termo);
                        if (tabela.getRowCount() != esperado) throw new AssertionError("Pesquisa por nome");
                        pesquisa.setText("");
                        if (tabela.getRowCount() != esperado) throw new AssertionError("Limpar pesquisa");
                    } catch (Throwable erro) { falha.set(erro); }
                    finally { w.dispose(); concluido.set(true); }
                    return;
                }
            });
            timer.start();
            botao(botao).doClick();
        });
        aguardar();
        long limite = System.currentTimeMillis() + 15000;
        while (!concluido.get() && System.currentTimeMillis() < limite) Thread.sleep(20);
        if (!concluido.get()) throw new AssertionError("Janela de consulta não abriu");
        if (falha.get() != null) throw new AssertionError(falha.get());
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
            edt(() -> { if (abas.getTabCount() != 3) throw new AssertionError("Aba redundante de relacionamentos"); });
            criarEditarConsultar(1, new String[]{"Publicadora", "Brasil", "2000", "Descrição"}, new String[]{"Publicadora editada", "Brasil", "2001", "Nova descrição"});
            criarEditarConsultar(2, new String[]{"Estúdio", "Brasil", "2000"}, new String[]{"Estúdio editado", "Japão", "2001"});
            selecionarDevs = new int[]{0};
            criarEditarConsultar(0, new String[]{"Jogo", "19,90", "2024-01-01", "pt, en"}, new String[]{"Jogo editado", "29.90", "2024-02-01", "pt, en, es"});
            if (catalogo.relacionamentos.desenvolvedorasDoJogo(1).size() != 1)
                throw new AssertionError("Cadastro não associou a desenvolvedora marcada");
            long segundaDev = catalogo.desenvolvedoras.cadastrar(new entidades.Desenvolvedora(-1, "Segundo estúdio", "Brasil", 2005));
            clicar("Listar todos / Atualizar"); selecionar();
            selecionarDevs = new int[]{0, 1};
            dialogo("Editar selecionado", "Jogo editado", "29.90", "2024-02-01", "pt, en, es");
            if (catalogo.relacionamentos.desenvolvedorasDoJogo(1).size() != 2)
                throw new AssertionError("Edição não salvou múltiplas desenvolvedoras");
            selecionar(); selecionarDevs = new int[]{0};
            dialogo("Editar selecionado", "Jogo editado", "29.90", "2024-02-01", "pt, en, es");
            if (catalogo.relacionamentos.jogosDaDesenvolvedora(segundaDev).size() != 0)
                throw new AssertionError("Desmarcar não removeu a associação");
            catalogo.desenvolvedoras.excluir(segundaDev);
            clicar("Listar todos / Atualizar");
            selecionarDevs = null;
            aba(0); selecionar(); consultarLista("Ver desenvolvedoras", 1, "ESTUDIO");
            aba(2); selecionar(); consultarLista("Ver jogos", 1, "Jogo editado");
            aba(1); selecionar(); consultarLista("Ver jogos", 1, "Jogo editado");
            // Reabre a janela com dados e vínculo ativos, depois ordena e consulta novamente.
            fecharJanela();
            abrirJanela();
            aba(0); linhas(1);
            edt(() -> {
                JTable tabela = componentes(painel(), JTable.class).get(0);
                if (!"Jogo editado".equals(tabela.getValueAt(0, 1))
                        || !"Estúdio editado".equals(tabela.getValueAt(0, 4))
                        || !"Publicadora editada".equals(tabela.getValueAt(0, 5))
                        || !"pt, en, es".equals(tabela.getValueAt(0, 6))) {
                    throw new AssertionError("Campos do jogo não persistiram ao reabrir a janela");
                }
            });
            dialogo("Ordenar arquivo por nome"); linhas(1);
            aba(0); selecionar(); consultarLista("Ver desenvolvedoras", 1, "ESTUDIO");
            aba(2); selecionar(); consultarLista("Ver jogos", 1, "Jogo editado");
            aba(1); selecionar(); consultarLista("Ver jogos", 1, "Jogo editado");
            aba(0); selecionar(); selecionarDevs = new int[0];
            dialogo("Editar selecionado", "Jogo editado", "29.90", "2024-02-01", "pt, en, es");
            selecionarDevs = null;
            selecionar(); consultarLista("Ver desenvolvedoras", 0, "ESTUDIO");
            aba(2); selecionar(); consultarLista("Ver jogos", 0, "Jogo editado");
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
