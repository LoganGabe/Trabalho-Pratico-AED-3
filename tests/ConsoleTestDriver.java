import menu.Menu;

/** Acesso ao menu legado exclusivo da suíte de regressão das Fases I/II. */
public final class ConsoleTestDriver {
    private ConsoleTestDriver() { }
    public static void main(String[] args) throws Exception {
        new Menu().executar();
    }
}
