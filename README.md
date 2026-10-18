# Catálogo de Jogos

Trabalho Prático de **Algoritmos e Estruturas de Dados III**, Engenharia de Computação — **PUC Minas**.

Integrantes: Gabriel El-dine, Gabriel Logan e Kleber Rhuan.

## Funcionalidades da Fase III

- Interface gráfica Java Swing com abas de Jogos, Publicadoras, Desenvolvedoras e Associações.
- Cadastro, consulta por ID, listagem, edição e exclusão das três entidades.
- Relacionamento Publicadora → Jogo (1:N), com consulta pela Árvore B+.
- Relacionamento Jogo ↔ Desenvolvedora (N:N), com arquivo intermediário binário, par único e índices persistentes nos dois sentidos.
- Inclusão e remoção de associações; consultas dos jogos de uma desenvolvedora e das desenvolvedoras de um jogo.
- Integridade referencial: validação das entidades e bloqueio de exclusão enquanto houver dependências.
- Ordenação externa dos jogos por nome, por intercalação balanceada, com reconstrução do Hash e reconexão dos serviços.
- Persistência binária, Hash Extensível por chave primária e árvores B+ como índices secundários.

Todas as operações do usuário estão acessíveis pela janela. `Main` inicia exclusivamente a interface gráfica. O menu legado é utilizado somente pelo driver dos testes de regressão; não é uma alternativa de interface da aplicação entregue.

## Requisitos

**JDK 11 ou superior**, com `java` e `javac` no PATH. Um JRE isolado não permite compilar. Para abrir a janela, é necessário um ambiente gráfico. Não há dependências externas de Java, Maven ou Gradle.

Confira as ferramentas:

```text
java -version
javac -version
```

Se houver várias versões instaladas, aponte `JAVA_HOME` para o JDK e coloque sua pasta `bin` no início do PATH. A compilação foi validada com JDK 21, usando `--release 11`.

## Compilar e executar no Windows

Na raiz do repositório, em PowerShell:

```powershell
./scripts/compilar.ps1
java -cp out Main
```

O script cria `out/` e compila os fontes em UTF-8. Alternativa sem script PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$fontesCatalogo = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[IO.File]::WriteAllLines((Join-Path (Get-Location) 'out/fontes.txt'), [string[]]$fontesCatalogo)
javac --release 11 -encoding UTF-8 -d out '@out/fontes.txt'
java -cp out Main
```

## Compilar e executar com Bash

Na raiz do repositório:

```bash
mkdir -p out
find src -name '*.java' -print > out/fontes.txt
javac --release 11 -encoding UTF-8 -d out @out/fontes.txt
java -cp out Main
```

## Escolher a pasta do catálogo no Windows

O programa utiliza a pasta `dados/` dentro do diretório de execução. Para usar o catálogo da raiz do projeto, execute os comandos anteriores ou `./scripts/executar.ps1`.

Para usar uma base em outra localização, mantendo o código neste repositório:

```powershell
./scripts/executar.ps1 -Base "$env:LOCALAPPDATA/CatalogoJogosAED3"
```

O script compila os fontes e abre a interface com o classpath absoluto. Uma pasta diferente inicia uma base separada; nenhuma migração nem exclusão dos dados da raiz é feita automaticamente. Para usar dados existentes na nova pasta, copie a pasta `dados/` com a aplicação fechada.

Nesta máquina, uma reprodução mínima da ordenação encerrou o Java com código 5 quando a base estava em `Documents`, inclusive sem interface. A mesma reprodução e a suíte gráfica completa passaram usando a pasta temporária do sistema. Não foi identificado o componente que encerrou o processo. Use outra localização para a base se esse comportamento ocorrer; não é necessário alterar o algoritmo nem as configurações de segurança para executar os testes validados.

## Usar a interface

1. Em **Publicadoras**, clique em **Novo** e cadastre uma publicadora.
2. Em **Desenvolvedoras**, cadastre os estúdios.
3. Em **Jogos**, cadastre um jogo selecionando sua publicadora. Use data no formato `AAAA-MM-DD`, preço como `59,90` ou `59.90` e idiomas separados por vírgula.
4. Em **Associações e consultas N:N**, selecione jogo e desenvolvedora e clique em **Associar**. As consultas exibem os dados completos das entidades relacionadas.
5. Para desfazer um vínculo, selecione o mesmo par e clique em **Remover associação**. Para editar ou excluir uma entidade, selecione sua linha na aba correspondente.
6. Em **Publicadoras**, selecione uma linha e clique em **Jogos da publicadora (1:N)**. Em **Jogos**, use **Ordenar arquivo por nome** para reorganizar fisicamente o arquivo.

**Consultar ID** mostra somente o registro encontrado; **Listar todos / Atualizar** restaura a tabela. Exclusões com dependências são bloqueadas: remova primeiro os vínculos N:N ou transfira/exclua os jogos de uma publicadora. Confirmações e erros aparecem na própria interface.

Os arquivos e índices são criados em `dados/`, relativos ao diretório de execução. Execute a partir da raiz para usar o catálogo do projeto. Fechar a janela encerra os recursos de persistência. Use uma única instância por base e mantenha uma cópia de segurança antes de substituir dados ou índices manualmente.

## Estrutura

```text
src/Main.java                  Entrada exclusiva da interface gráfica
src/interfacegrafica/          Janela Swing e sessão de arquivos/serviços
src/entidades/                 Jogo, Publicadora, Desenvolvedora e vínculo
src/persistencia/              CRUD, serviços, Hash, B+ e ordenação externa
src/menu/                      Menu legado usado apenas pela regressão
tests/                         Suítes de persistência, integração e interface
scripts/compilar.ps1            Compilação no Windows
scripts/executar.ps1            Execução gráfica com pasta de base opcional
docs/                          Modelagem, decisões e registro de validação
```

## Testes

Em PowerShell:

```powershell
./tests/run-tests.ps1
./tests/run-tests.ps1 -Interface
```

Ou com JDK e Bash:

```bash
bash tests/run-tests.sh
bash tests/run-tests.sh --interface
```

A opção de interface requer ambiente gráfico e abre uma janela e diálogos durante o teste automatizado. Cada cenário usa uma base isolada e não lê nem modifica `dados/` da raiz. Os executores PowerShell e Bash usam pastas temporárias do sistema com nomes únicos e as removem ao terminar.

A validação de 18/10/2026 passou nos três executáveis N:N existentes, em `CatalogoTest`, nos 16 testes da `TestSuite` e no teste gráfico completo `InterfaceGraficaTest`, incluindo ordenação. Os cenários cobrem serialização, Hash/B+, CRUDs, integridade, duplicatas, N:N nos dois sentidos, 1:N, ordenação, atualização de entidades com vínculos, reinício, reconstrução dos índices e ações dos componentes Swing.

A suíte gráfica fecha e reabre a janela com dados e associações ativos, ordena e repete as consultas; também verifica a abertura pela entrada real `Main`. A suíte anterior usa `ConsoleTestDriver`, exclusivo de `tests/`, em processos separados. Os comandos PowerShell foram executados com JDK 21 e compatibilidade Java 11. Os comandos Bash foram atualizados, mas não executados nesta validação Windows. Os testes automatizados não substituem uma revisão visual manual em outras resoluções ou escalas.

## Documentação

- [Interface, evidências por item e roteiro do vídeo](docs/INTERFACE-FASE-III.md)
- [Modelagem e arquitetura da Fase III](docs/MODELAGEM-FASE-III.md)
- [Respostas às cinco perguntas oficiais do Formulário de Decisões](docs/FORMULARIO-DECISOES-FASE-III.md)
- [Checklist de continuidade e responsabilidades](docs/CONTINUIDADE-FASE-III.md)
- [Repositório do grupo](https://github.com/LoganGabe/Trabalho-Pratico-AED-3)

As Partes 1, 2 e 3 estão implementadas, integradas e com validação automatizada aprovada. Gravação do vídeo, publicação das alterações no GitHub, montagem do PDF único e envio ao Canvas continuam como etapas finais do grupo.
