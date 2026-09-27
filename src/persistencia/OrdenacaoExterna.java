package persistencia;

import entidades.Jogo;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;

public class OrdenacaoExterna {

    private final String caminhoArquivo;
    private final int capacidadeMemoria;

    private final String tempA =
        "./dados/jogos/tempA.db";

    private final String tempB =
        "./dados/jogos/tempB.db";

    private final String tempC =
        "./dados/jogos/tempC.db";

    private final String tempD =
        "./dados/jogos/tempD.db";

    private int totalRegistros;
    private int tamanhoRun;

    private String arquivoFinalTemporario;

    public OrdenacaoExterna(
        String caminhoArquivo,
        int capacidadeMemoria
    ) {

        if (capacidadeMemoria <= 0) {
            throw new IllegalArgumentException(
                "A capacidade de memoria deve ser maior que zero."
            );
        }

        this.caminhoArquivo =
            caminhoArquivo;

        this.capacidadeMemoria =
            capacidadeMemoria;

        this.totalRegistros = 0;

        this.tamanhoRun =
            capacidadeMemoria;

        this.arquivoFinalTemporario =
            null;
    }

    // =========================================================
    // FASE 1 - DISTRIBUIÇÃO
    // =========================================================

    public void ordenarPorNome()
        throws Exception {

        System.out.println(
            "Iniciando Fase 1: distribuicao de blocos..."
        );

        totalRegistros = 0;

        tamanhoRun =
            capacidadeMemoria;

        arquivoFinalTemporario =
            null;

        try (
            RandomAccessFile origem =
                new RandomAccessFile(
                    caminhoArquivo,
                    "r"
                );

            RandomAccessFile a =
                new RandomAccessFile(
                    tempA,
                    "rw"
                );

            RandomAccessFile b =
                new RandomAccessFile(
                    tempB,
                    "rw"
                )
        ) {

            a.setLength(0);
            b.setLength(0);

            // Pula o cabeçalho do Arquivo<T>
            origem.seek(16);

            boolean usarA = true;

            ArrayList<Jogo> bloco =
                new ArrayList<>();

            while (
                origem.getFilePointer()
                < origem.length()
            ) {

                Jogo jogo =
                    lerRegistroDoArquivoDeDados(
                        origem
                    );

                if (jogo != null) {

                    bloco.add(jogo);

                    totalRegistros++;
                }

                if (
                    bloco.size()
                        == capacidadeMemoria
                    ||
                    (
                        origem.getFilePointer()
                            >= origem.length()
                        &&
                        !bloco.isEmpty()
                    )
                ) {

                    bloco.sort(
                        Comparator.comparing(
                            Jogo::getNome,
                            String.CASE_INSENSITIVE_ORDER
                        )
                    );

                    RandomAccessFile destino =
                        usarA ? a : b;

                    for (Jogo j : bloco) {

                        escreverJogo(
                            destino,
                            j
                        );
                    }

                    bloco.clear();

                    usarA = !usarA;
                }
            }
        }

        if (
            totalRegistros
            <= capacidadeMemoria
        ) {

            arquivoFinalTemporario =
                tempA;
        }

        System.out.println(
            "Fase 1 concluida. "
            + totalRegistros
            + " registros ativos distribuidos."
        );
    }

    // =========================================================
    // FASE 2 - INTERCALAÇÃO
    // =========================================================

    public void intercalar()
        throws Exception {

        System.out.println(
            "Iniciando Fase 2: intercalacao balanceada..."
        );

        if (totalRegistros == 0) {

            arquivoFinalTemporario =
                tempA;

            return;
        }

        /*
         * Se coube tudo em um único bloco,
         * já está ordenado.
         */
        if (
            arquivoFinalTemporario
            != null
        ) {

            System.out.println(
                "Apenas um bloco foi gerado."
            );

            return;
        }

        String entrada1 =
            tempA;

        String entrada2 =
            tempB;

        String saida1 =
            tempC;

        String saida2 =
            tempD;

        int runAtual =
            tamanhoRun;

        while (
            runAtual < totalRegistros
        ) {

            System.out.println(
                "Intercalando blocos de tamanho "
                + runAtual
                + "..."
            );

            try (
                RandomAccessFile in1 =
                    new RandomAccessFile(
                        entrada1,
                        "r"
                    );

                RandomAccessFile in2 =
                    new RandomAccessFile(
                        entrada2,
                        "r"
                    );

                RandomAccessFile out1 =
                    new RandomAccessFile(
                        saida1,
                        "rw"
                    );

                RandomAccessFile out2 =
                    new RandomAccessFile(
                        saida2,
                        "rw"
                    )
            ) {

                out1.setLength(0);
                out2.setLength(0);

                boolean usarSaida1 =
                    true;

                while (
                    in1.getFilePointer()
                        < in1.length()
                    ||
                    in2.getFilePointer()
                        < in2.length()
                ) {

                    RandomAccessFile destino =
                        usarSaida1
                            ? out1
                            : out2;

                    intercalarUmParDeRuns(
                        in1,
                        in2,
                        destino,
                        runAtual
                    );

                    usarSaida1 =
                        !usarSaida1;
                }
            }

            /*
             * Depois de uma passada,
             * cada run dobra de tamanho.
             */
            runAtual *= 2;

            /*
             * Troca os arquivos:
             *
             * saídas viram entradas
             * e entradas antigas viram
             * próximas saídas.
             */
            String antigaEntrada1 =
                entrada1;

            String antigaEntrada2 =
                entrada2;

            entrada1 =
                saida1;

            entrada2 =
                saida2;

            saida1 =
                antigaEntrada1;

            saida2 =
                antigaEntrada2;
        }

        arquivoFinalTemporario =
            entrada1;

        tamanhoRun =
            runAtual;

        System.out.println(
            "Fase 2 concluida."
        );
    }

    private void intercalarUmParDeRuns(
        RandomAccessFile in1,
        RandomAccessFile in2,
        RandomAccessFile destino,
        int limiteRun
    ) throws Exception {

        int lidos1 = 0;
        int lidos2 = 0;

        Jogo jogo1 = null;
        Jogo jogo2 = null;

        if (
            lidos1 < limiteRun
            &&
            in1.getFilePointer()
                < in1.length()
        ) {

            jogo1 =
                lerProximoJogoTemporario(
                    in1
                );

            if (jogo1 != null) {
                lidos1++;
            }
        }

        if (
            lidos2 < limiteRun
            &&
            in2.getFilePointer()
                < in2.length()
        ) {

            jogo2 =
                lerProximoJogoTemporario(
                    in2
                );

            if (jogo2 != null) {
                lidos2++;
            }
        }

        while (
            jogo1 != null
            ||
            jogo2 != null
        ) {

            if (
                jogo2 == null
                ||
                (
                    jogo1 != null
                    &&
                    jogo1.getNome()
                        .compareToIgnoreCase(
                            jogo2.getNome()
                        ) <= 0
                )
            ) {

                escreverJogo(
                    destino,
                    jogo1
                );

                if (
                    lidos1 < limiteRun
                    &&
                    in1.getFilePointer()
                        < in1.length()
                ) {

                    jogo1 =
                        lerProximoJogoTemporario(
                            in1
                        );

                    if (jogo1 != null) {
                        lidos1++;
                    }

                } else {

                    jogo1 = null;
                }

            } else {

                escreverJogo(
                    destino,
                    jogo2
                );

                if (
                    lidos2 < limiteRun
                    &&
                    in2.getFilePointer()
                        < in2.length()
                ) {

                    jogo2 =
                        lerProximoJogoTemporario(
                            in2
                        );

                    if (jogo2 != null) {
                        lidos2++;
                    }

                } else {

                    jogo2 = null;
                }
            }
        }
    }

    // =========================================================
    // FASE 3 - SUBSTITUIÇÃO DO ARQUIVO
    // =========================================================

    public void finalizar()
        throws Exception {

        System.out.println(
            "Iniciando Fase 3: substituicao do arquivo..."
        );

        if (
            arquivoFinalTemporario
            == null
        ) {

            throw new IllegalStateException(
                "Execute a distribuicao e a intercalacao antes."
            );
        }

        /*
         * Guarda o último ID do arquivo original.
         */
        long ultimoId;

        try (
            RandomAccessFile origem =
                new RandomAccessFile(
                    caminhoArquivo,
                    "r"
                )
        ) {

            ultimoId =
                origem.readLong();
        }

        Path original =
            Path.of(
                caminhoArquivo
            );

        Path novo =
            Path.of(
                caminhoArquivo
                    + ".novo"
            );

        try (
            RandomAccessFile novoArquivo =
                new RandomAccessFile(
                    novo.toFile(),
                    "rw"
                );

            RandomAccessFile ordenado =
                new RandomAccessFile(
                    arquivoFinalTemporario,
                    "r"
                )
        ) {

            novoArquivo.setLength(0);

            /*
             * Cabeçalho do Arquivo<T>.
             */
            novoArquivo.writeLong(
                ultimoId
            );

            // Lista de excluídos vazia
            novoArquivo.writeLong(
                -1
            );

            byte[] buffer =
                new byte[4096];

            int quantidadeLida;

            while (
                (
                    quantidadeLida =
                        ordenado.read(buffer)
                ) != -1
            ) {

                novoArquivo.write(
                    buffer,
                    0,
                    quantidadeLida
                );
            }
        }

        Files.move(
            novo,
            original,
            StandardCopyOption.REPLACE_EXISTING
        );

        /*
         * Os registros mudaram de endereço.
         * Portanto o Hash antigo ficou inválido.
         */
        Files.deleteIfExists(
            Path.of(
                "./dados/indices/jogos.dir"
            )
        );

        Files.deleteIfExists(
            Path.of(
                "./dados/indices/jogos.bkt"
            )
        );

        limparTemporarios();

        System.out.println(
            "Fase 3 concluida."
        );
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private Jogo
        lerRegistroDoArquivoDeDados(
            RandomAccessFile arquivo
        ) throws Exception {

        byte lapide =
            arquivo.readByte();

        int tamanho =
            arquivo.readUnsignedShort();

        byte[] dados =
            new byte[tamanho];

        arquivo.readFully(dados);

        if (lapide == '*') {
            return null;
        }

        Jogo jogo =
            new Jogo();

        jogo.fromByteArray(
            dados
        );

        return jogo;
    }

    private Jogo
        lerProximoJogoTemporario(
            RandomAccessFile arquivo
        ) throws Exception {

        if (
            arquivo.getFilePointer()
            >= arquivo.length()
        ) {

            return null;
        }

        byte lapide =
            arquivo.readByte();

        int tamanho =
            arquivo.readUnsignedShort();

        byte[] dados =
            new byte[tamanho];

        arquivo.readFully(dados);

        if (lapide == '*') {

            return
                lerProximoJogoTemporario(
                    arquivo
                );
        }

        Jogo jogo =
            new Jogo();

        jogo.fromByteArray(
            dados
        );

        return jogo;
    }

    private void escreverJogo(
        RandomAccessFile arquivo,
        Jogo jogo
    ) throws Exception {

        byte[] dados =
            jogo.toByteArray();

        arquivo.writeByte(' ');

        arquivo.writeShort(
            dados.length
        );

        arquivo.write(
            dados
        );
    }

    private void limparTemporarios()
        throws Exception {

        Files.deleteIfExists(
            Path.of(tempA)
        );

        Files.deleteIfExists(
            Path.of(tempB)
        );

        Files.deleteIfExists(
            Path.of(tempC)
        );

        Files.deleteIfExists(
            Path.of(tempD)
        );
    }
}