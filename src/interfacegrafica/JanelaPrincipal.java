package interfacegrafica;

import entidades.*;
import persistencia.Registro;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Interface Swing: componentes na EDT e operações de arquivo em tarefas exclusivas. */
public class JanelaPrincipal extends JFrame {
    private final Catalogo catalogo;
    private final JLabel status = new JLabel("Pronto");
    private final JTabbedPane abas = new JTabbedPane();
    private final List<Crud<?>> cadastros = new ArrayList<>();
    private List<Publicadora> publicadorasDisponiveis = new ArrayList<>();
    private List<Desenvolvedora> desenvolvedorasDisponiveis = new ArrayList<>();
    private Map<Long, List<Long>> vinculosPorJogo = new HashMap<>();
    private boolean ocupado;

    public JanelaPrincipal(Catalogo catalogo) {
        super("Catálogo de Jogos — AED III");
        this.catalogo = catalogo;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        setSize(1120, 700);
        setLocationRelativeTo(null);
        JPanel topo = new JPanel(new BorderLayout(12, 8));
        topo.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel titulo = new JLabel("Catálogo de Jogos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 25f));
        topo.add(titulo, BorderLayout.NORTH);
        topo.add(new JLabel("Gerencie jogos, publicadoras, desenvolvedoras e seus relacionamentos."));
        add(topo, BorderLayout.NORTH);
        criarCadastros();
        add(abas);
        status.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        add(status, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!ocupado) executar(() -> { catalogo.close(); return () -> dispose(); });
            }
        });
        atualizar();
    }

    private void criarCadastros() {
        Crud<Jogo> jogos = new Crud<Jogo>("Jogos", "ID", "Nome", "Preço (R$)", "Lançamento", "Desenvolvedoras", "Publicadora", "Idiomas") {
            private List<Long> selecionadas = new ArrayList<>();
            List<Jogo> listar() throws Exception { return catalogo.jogos.listar(); }
            Jogo consultar(long id) throws Exception { return catalogo.jogos.consultar(id); }
            void salvar(Jogo j, boolean novo) throws Exception {
                for (long id : selecionadas) {
                    if (catalogo.desenvolvedoras.consultar(id) == null)
                        throw new IllegalArgumentException("Desenvolvedora não encontrada: " + id);
                }
                if (novo) catalogo.jogos.cadastrar(j); else exigir(catalogo.jogos.atualizar(j));
                List<Long> anteriores = catalogo.relacionamentos.desenvolvedorasDoJogo(j.getId());
                for (long id : selecionadas) {
                    if (!anteriores.contains(id)) catalogo.relacionamentos.associar(j.getId(), id);
                }
                for (long id : anteriores) {
                    if (!selecionadas.contains(id)) catalogo.relacionamentos.desassociar(j.getId(), id);
                }
            }
            boolean excluir(long id) throws Exception { return catalogo.jogos.excluir(id); }
            Object[] linha(Jogo j) { return linhaJogo(j); }
            Jogo formulario(Jogo j) {
                JComboBox<Opcao> publicadora = new JComboBox<>();
                for (Publicadora pub : publicadorasDisponiveis) {
                    publicadora.addItem(new Opcao(pub.getId(), pub.getNome()));
                    if (j != null && pub.getId() == j.getPublicadoraId()) publicadora.setSelectedIndex(publicadora.getItemCount() - 1);
                }
                if (publicadora.getItemCount() == 0) throw new IllegalArgumentException("Cadastre uma publicadora antes de cadastrar jogos.");
                JTextField nome = campo(j == null ? "" : j.getNome());
                JTextField preco = campo(j == null ? "0" : Float.toString(j.getPreco()));
                JTextField data = campo(j == null ? LocalDate.now().toString() : j.getDataLancamento().toString());
                JTextField idiomas = campo(j == null ? "Português" : String.join(", ", j.getIdiomas()));
                List<Long> atuais = j == null ? Collections.emptyList()
                    : vinculosPorJogo.getOrDefault(j.getId(), Collections.emptyList());
                SeletorDesenvolvedoras selecao = new SeletorDesenvolvedoras(desenvolvedorasDisponiveis, atuais);
                if (!form("Jogo", new String[]{"Nome", "Preço (ex.: 59,90)", "Lançamento (AAAA-MM-DD)", "Desenvolvedoras", "Publicadora", "Idiomas separados por vírgula"}, nome, preco, data, selecao, publicadora, idiomas)) return null;
                float valor = Float.parseFloat(texto(preco).replace(',', '.'));
                if (!Float.isFinite(valor) || valor < 0) throw new IllegalArgumentException("O preço deve ser finito e não negativo.");
                selecionadas = selecao.getSelecionadas();
                return new Jogo(j == null ? -1 : j.getId(), obrigatorio(nome), valor, LocalDate.parse(texto(data)),
                    ((Opcao) publicadora.getSelectedItem()).id, Arrays.stream(texto(idiomas).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
            }
        };
        jogos.barra.add(botao("Ordenar arquivo por nome", () -> {
            if (confirmar("Executar a ordenação externa por nome?")) executar(() -> { catalogo.ordenar(); return carregar("Arquivo ordenado e Hash reconstruído."); });
        }));
        jogos.barra.add(botao("Ver desenvolvedoras", () -> {
            Jogo selecionado = jogos.selecionado();
            executar(() -> {
                List<Desenvolvedora> lista = new ArrayList<>();
                for (long id : catalogo.relacionamentos.desenvolvedorasDoJogo(selecionado.getId())) {
                    Desenvolvedora dev = catalogo.desenvolvedoras.consultar(id);
                    if (dev != null) lista.add(dev);
                }
                return () -> mostrarDesenvolvedoras("Desenvolvedoras de " + selecionado.getNome(), lista);
            });
        }));
        adicionar(jogos);
        Crud<Publicadora> publicadoras = new Crud<Publicadora>("Publicadoras", "ID", "Nome", "País", "Fundação", "Descrição") {
            List<Publicadora> listar() throws Exception { return catalogo.publicadoras.listar(); }
            Publicadora consultar(long id) throws Exception { return catalogo.publicadoras.consultar(id); }
            void salvar(Publicadora p, boolean novo) throws Exception {
                if (novo) catalogo.publicadoras.cadastrar(p); else exigir(catalogo.publicadoras.atualizar(p));
            }
            boolean excluir(long id) throws Exception { return catalogo.publicadoras.excluir(id); }
            Object[] linha(Publicadora p) { return new Object[]{p.getId(), p.getNome(), p.getPais(), p.getAnoFundacao(), p.getDescricao()}; }
            Publicadora formulario(Publicadora p) {
                JTextField nome = campo(p == null ? "" : p.getNome()), pais = campo(p == null ? "" : p.getPais());
                JTextField ano = campo(p == null ? "" : Integer.toString(p.getAnoFundacao())), descricao = campo(p == null ? "" : p.getDescricao());
                if (!form("Publicadora", new String[]{"Nome", "País", "Ano de fundação", "Descrição"}, nome, pais, ano, descricao)) return null;
                return new Publicadora(p == null ? -1 : p.getId(), obrigatorio(nome), obrigatorio(pais), ano(ano), texto(descricao));
            }
        };
        publicadoras.barra.add(botao("Ver jogos", () -> {
            Publicadora p = publicadoras.selecionado();
            executar(() -> {
                List<Jogo> lista = catalogo.jogos.listarPorPublicadora(p.getId());
                return () -> mostrarJogos("Jogos de " + p.getNome(), lista);
            });
        }));
        adicionar(publicadoras);
        Crud<Desenvolvedora> desenvolvedoras = new Crud<Desenvolvedora>("Desenvolvedoras", "ID", "Nome", "País", "Fundação") {
            List<Desenvolvedora> listar() throws Exception { return catalogo.desenvolvedoras.listar(); }
            Desenvolvedora consultar(long id) throws Exception { return catalogo.desenvolvedoras.consultar(id); }
            void salvar(Desenvolvedora d, boolean novo) throws Exception {
                if (novo) catalogo.desenvolvedoras.cadastrar(d); else exigir(catalogo.desenvolvedoras.atualizar(d));
            }
            boolean excluir(long id) throws Exception { return catalogo.desenvolvedoras.excluir(id); }
            Object[] linha(Desenvolvedora d) { return new Object[]{d.getId(), d.getNome(), d.getPais(), d.getAnoFundacao()}; }
            Desenvolvedora formulario(Desenvolvedora d) {
                JTextField nome = campo(d == null ? "" : d.getNome()), pais = campo(d == null ? "" : d.getPais());
                JTextField ano = campo(d == null ? "" : Integer.toString(d.getAnoFundacao()));
                if (!form("Desenvolvedora", new String[]{"Nome", "País", "Ano de fundação"}, nome, pais, ano)) return null;
                return new Desenvolvedora(d == null ? -1 : d.getId(), obrigatorio(nome), obrigatorio(pais), ano(ano));
            }
        };
        desenvolvedoras.barra.add(botao("Ver jogos", () -> {
            Desenvolvedora selecionada = desenvolvedoras.selecionado();
            executar(() -> {
                List<Jogo> lista = new ArrayList<>();
                for (long id : catalogo.relacionamentos.jogosDaDesenvolvedora(selecionada.getId())) {
                    Jogo registro = catalogo.jogos.consultar(id);
                    if (registro != null) lista.add(registro);
                }
                return () -> mostrarJogos("Jogos de " + selecionada.getNome(), lista);
            });
        }));
        adicionar(desenvolvedoras);
    }

    private void adicionar(Crud<?> crud) { cadastros.add(crud); abas.addTab(crud.nome, crud); }

    private abstract class Crud<T extends Registro> extends JPanel {
        final String nome;
        final DefaultTableModel modelo;
        final JTable tabela;
        final JPanel barra = new JPanel(new GridLayout(0, 3, 6, 6));
        List<T> registros = new ArrayList<>();
        Crud(String nome, String... colunas) {
            super(new BorderLayout(8, 8)); this.nome = nome;
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            modelo = modelo(colunas); tabela = tabela(modelo);
            barra.add(botao("Novo", () -> editar(null)));
            barra.add(botao("Editar selecionado", () -> editar(selecionado())));
            barra.add(botao("Excluir selecionado", () -> {
                T item = selecionado();
                if (confirmar("Excluir o registro " + item.getId() + " de " + nome + "?")) executar(() -> { exigir(excluir(item.getId())); return JanelaPrincipal.this.carregar("Registro excluído."); });
            }));
            barra.add(botao("Consultar ID", () -> {
                String entrada = JOptionPane.showInputDialog(JanelaPrincipal.this, "ID de " + nome + ":");
                if (entrada == null) return;
                long id = Long.parseLong(entrada.trim());
                if (id <= 0) throw new IllegalArgumentException("Informe um ID positivo.");
                executar(() -> {
                    T item = consultar(id);
                    return () -> { registros = item == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(item)); preencher(); status.setText(item == null ? "Registro não encontrado." : "Registro encontrado pelo Hash."); };
                });
            }));
            barra.add(botao("Listar todos / Atualizar", () -> atualizar()));
            add(barra, BorderLayout.NORTH); add(new JScrollPane(tabela));
            add(new JLabel("Selecione uma linha para editar ou excluir. Consulte pelo ID ou liste todos os registros."), BorderLayout.SOUTH);
        }
        abstract List<T> listar() throws Exception;
        abstract T consultar(long id) throws Exception;
        abstract void salvar(T item, boolean novo) throws Exception;
        abstract boolean excluir(long id) throws Exception;
        abstract Object[] linha(T item);
        abstract T formulario(T item);
        T selecionado() {
            int linha = tabela.getSelectedRow();
            if (linha < 0) throw new IllegalArgumentException("Selecione um registro na tabela.");
            return registros.get(tabela.convertRowIndexToModel(linha));
        }
        void editar(T item) {
            T novo = formulario(item);
            if (novo != null) executar(() -> { salvar(novo, item == null); return JanelaPrincipal.this.carregar("Registro salvo."); });
        }
        void preencher() { modelo.setRowCount(0); for (T item : registros) modelo.addRow(linha(item)); }
        Runnable carregar() throws Exception {
            List<T> lista = listar();
            return () -> { registros = lista; preencher(); };
        }
    }

    private void atualizar() { executar(() -> carregar("Catálogo atualizado.")); }

    private Runnable carregar(String mensagem) throws Exception {
        List<Runnable> aplicar = new ArrayList<>();
        for (Crud<?> crud : cadastros) aplicar.add(crud.carregar());
        List<Jogo> jogos = catalogo.jogos.listar();
        List<Desenvolvedora> devs = catalogo.desenvolvedoras.listar();
        List<Publicadora> pubs = catalogo.publicadoras.listar();
        Map<Long, List<Long>> associacoes = new HashMap<>();
        for (Jogo registro : jogos) {
            associacoes.put(registro.getId(), catalogo.relacionamentos.desenvolvedorasDoJogo(registro.getId()));
        }
        return () -> {
            publicadorasDisponiveis = pubs;
            desenvolvedorasDisponiveis = devs;
            vinculosPorJogo = associacoes;
            for (Runnable r : aplicar) r.run();
            status.setText(mensagem);
        };
    }

    private void executar(Callable<Runnable> tarefa) {
        if (ocupado) return;
        ocupado = true; habilitar(abas, false); status.setText("Processando…");
        new SwingWorker<Runnable, Void>() {
            @Override protected Runnable doInBackground() throws Exception { return tarefa.call(); }
            @Override protected void done() {
                ocupado = false; habilitar(abas, true);
                try { get().run(); } catch (Exception e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause(); erro(causa);
                }
            }
        }.execute();
    }

    private Object[] linhaJogo(Jogo jogo) {
        List<String> nomes = new ArrayList<>();
        for (long id : vinculosPorJogo.getOrDefault(jogo.getId(), Collections.emptyList())) {
            String nome = "Não encontrada";
            for (Desenvolvedora dev : desenvolvedorasDisponiveis) {
                if (dev.getId() == id) { nome = dev.getNome(); break; }
            }
            nomes.add(nome);
        }
        String nomePublicadora = "Não encontrada";
        for (Publicadora publicadora : publicadorasDisponiveis) {
            if (publicadora.getId() == jogo.getPublicadoraId()) {
                nomePublicadora = publicadora.getNome();
                break;
            }
        }
        return new Object[]{jogo.getId(), jogo.getNome(), jogo.getPreco(), jogo.getDataLancamento(),
            String.join(", ", nomes),
            nomePublicadora, String.join(", ", jogo.getIdiomas())};
    }

    private void mostrarJogos(String titulo, List<Jogo> jogos) {
        DefaultTableModel m = modelo("ID", "Nome", "Preço", "Lançamento", "Desenvolvedoras", "Publicadora", "Idiomas");
        for (Jogo j : jogos) m.addRow(linhaJogo(j));
        new ListaPesquisa(this, titulo, m).setVisible(true);
        status.setText(jogos.size() + " jogo(s) encontrado(s).");
    }

    private void mostrarDesenvolvedoras(String titulo, List<Desenvolvedora> desenvolvedoras) {
        DefaultTableModel m = modelo("ID", "Nome", "País", "Fundação");
        for (Desenvolvedora dev : desenvolvedoras) {
            m.addRow(new Object[]{dev.getId(), dev.getNome(), dev.getPais(), dev.getAnoFundacao()});
        }
        new ListaPesquisa(this, titulo, m).setVisible(true);
        status.setText(desenvolvedoras.size() + " desenvolvedora(s) encontrada(s).");
    }

    private JButton botao(String titulo, Runnable acao) {
        JButton b = new JButton(titulo);
        b.addActionListener(e -> { if (!ocupado) try { acao.run(); } catch (Exception falha) { erro(falha); } });
        return b;
    }
    private void erro(Throwable e) {
        String mensagem = e instanceof NumberFormatException ? "Informe um número válido."
            : e instanceof DateTimeParseException ? "Informe uma data válida no formato AAAA-MM-DD." : e.getMessage();
        status.setText("Operação não concluída.");
        JOptionPane.showMessageDialog(this, mensagem == null ? e.toString() : mensagem, "Não foi possível concluir", JOptionPane.ERROR_MESSAGE);
    }
    private boolean form(String titulo, String[] rotulos, JComponent... campos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints posicao = new GridBagConstraints();
        posicao.insets = new Insets(5, 5, 5, 5);
        posicao.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < campos.length; i++) {
            posicao.gridy = i;
            posicao.gridx = 0;
            posicao.weightx = 0;
            posicao.fill = GridBagConstraints.NONE;
            p.add(new JLabel(rotulos[i]), posicao);
            posicao.gridx = 1;
            posicao.weightx = 1;
            posicao.fill = GridBagConstraints.HORIZONTAL;
            p.add(campos[i], posicao);
        }
        return JOptionPane.showConfirmDialog(this, p, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private boolean confirmar(String mensagem) { return JOptionPane.showConfirmDialog(this, mensagem, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private static JTextField campo(String texto) { return new JTextField(texto, 24); }
    private static String texto(JTextField campo) { return campo.getText().trim(); }
    private static String obrigatorio(JTextField campo) {
        String texto = texto(campo); if (texto.isEmpty()) throw new IllegalArgumentException("Preencha os campos obrigatórios."); return texto;
    }
    private static int ano(JTextField campo) {
        int ano = Integer.parseInt(obrigatorio(campo));
        if (ano < 0 || ano > java.time.Year.now().getValue()) throw new IllegalArgumentException("Ano de fundação inválido.");
        return ano;
    }
    private static void exigir(boolean sucesso) { if (!sucesso) throw new IllegalStateException("Registro não encontrado ou operação recusada."); }

    private static DefaultTableModel modelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }
    private static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setRowHeight(27); t.setFillsViewportHeight(true); return t;
    }
    private static void habilitar(Component c, boolean habilitado) {
        c.setEnabled(habilitado);
        if (c instanceof Container) for (Component filho : ((Container) c).getComponents()) habilitar(filho, habilitado);
    }
    private static class Opcao {
        final long id; final String nome;
        Opcao(long id, String nome) { this.id = id; this.nome = nome; }
        @Override public String toString() { return nome; }
    }
}
