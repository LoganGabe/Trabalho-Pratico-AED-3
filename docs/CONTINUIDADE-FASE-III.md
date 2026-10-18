# Fase III — checklist de continuidade

Estado conferido em 18/10/2026. As Partes 1, 2 e 3 estão implementadas e integradas. Os cinco enunciados oficiais foram recebidos e respondidos em [Formulário de Decisões](FORMULARIO-DECISOES-FASE-III.md).

A aplicação entregue abre somente a interface Swing por `Main`. O menu legado foi mantido como suporte aos testes de regressão, com entrada exclusiva em `tests/ConsoleTestDriver.java`; não é uma opção da interface do usuário.

## 1. Logan — Relacionamento N:N e índices

- [x] **1.1** Definir a estrutura do relacionamento Jogo ↔ Desenvolvedora.
- [x] **1.2** Criar a classe intermediária `JogoDesenvolvedora`.
- [x] **1.3** Implementar a chave composta `(jogoId, desenvolvedoraId)`.
- [x] **1.4** Implementar o armazenamento binário das associações.
- [x] **1.5** Implementar a inserção de associações.
- [x] **1.6** Impedir o cadastro de associações duplicadas.
- [x] **1.7** Implementar a exclusão de associações.
- [x] **1.8** Criar os índices persistentes para consultas nos dois sentidos.
- [x] **1.9** Implementar a busca de desenvolvedoras por jogo.
- [x] **1.10** Implementar a busca de jogos por desenvolvedora.
- [x] **1.11** Testar persistência, índices e reconstrução.

**Evidência:** `JogoDesenvolvedoraTest` verifica serialização; `ArquivoJogoDesenvolvedoraTest` verifica pares únicos, consultas nos dois sentidos, exclusão, reinício, continuidade dos IDs, divisões de páginas, índice ausente e recuperação por marcador de pendência. Os testes utilizam bases isoladas.

A chave composta é o par de campos do vínculo. O `id` técnico adicional permite reutilizar `Arquivo<T>` e o Hash; ele não substitui a unicidade do par. Os índices secundários armazenam `(jogoId, vinculoId)` e `(desenvolvedoraId, vinculoId)` em arquivos B+ distintos.

## 2. Gabriel El-dine — CRUD e integridade referencial

- [x] **2.1** Criar a entidade `Desenvolvedora`.
- [x] **2.2** Implementar o CRUD completo de Desenvolvedora.
- [x] **2.3** Integrar o CRUD à persistência binária existente.
- [x] **2.4** Garantir a indexação por chave primária de Desenvolvedora.
- [x] **2.5** Validar a existência de Jogo e Desenvolvedora antes de associá-los.
- [x] **2.6** Definir e implementar as regras de exclusão de entidades que possuem associações.
- [x] **2.7** Garantir a integridade referencial nas operações de inclusão, alteração e exclusão.
- [x] **2.8** Integrar as operações N:N às regras de negócio do sistema.
- [x] **2.9** Testar as validações e a consistência dos relacionamentos.

**Evidência:** `RelacionamentoJogoDesenvolvedoraTest` usa a entidade Desenvolvedora definitiva e testa seu CRUD pelo Hash, IDs inválidos, entidades inexistentes/excluídas, duplicatas, consultas nos dois sentidos, alterações de entidades com vínculos, troca de publicadora, rejeição de FK inválida e exclusões bloqueadas/permitidas. `CatalogoTest` complementa a verificação com reabertura e ordenação.

**Política adotada:** bloquear a exclusão de Jogo ou Desenvolvedora enquanto houver associações; o usuário remove primeiro os vínculos. Publicadora com jogos também tem exclusão bloqueada. As regras estão nos serviços usados pela interface, e não apenas no menu legado.

## 3. Kleber Rhuan — Interface gráfica e documentação

- [x] **3.1** Definir a tecnologia e a estrutura da interface gráfica: Java Swing, sem dependências externas.
- [x] **3.2** Criar a janela principal e a navegação por abas.
- [x] **3.3** Criar as telas de CRUD de Jogos.
- [x] **3.4** Criar as telas de CRUD de Publicadoras.
- [x] **3.5** Criar as telas de CRUD de Desenvolvedoras.
- [x] **3.6** Criar a interface para associar Jogo e Desenvolvedora.
- [x] **3.7** Criar a interface para remover associações.
- [x] **3.8** Criar a consulta de desenvolvedoras de um jogo.
- [x] **3.9** Criar a consulta de jogos de uma desenvolvedora.
- [x] **3.10** Integrar as funcionalidades anteriores, incluindo ordenação externa e consulta 1:N.
- [x] **3.11** Atualizar a documentação do projeto e a modelagem.
- [x] **3.12** Responder às cinco perguntas oficiais do Formulário de Decisões.
- [x] **3.13** Atualizar o README com as instruções de compilação e execução.

**Evidência:** `InterfaceGraficaTest` aciona os próprios componentes Swing para navegar, preencher formulários, consultar IDs, editar/excluir registros, criar/remover associações, recusar duplicatas, consultar o N:N nos dois sentidos, consultar o 1:N e ordenar. O teste também fecha e reabre a janela com dados e vínculos ativos, verifica a persistência, ordena e repete as consultas. Por fim, verifica que `Main` abre a interface definitiva.

A sessão `Catalogo` abre uma única instância de cada arquivo e conecta os serviços. As operações de arquivo executam em `SwingWorker`, com os controles desabilitados durante cada tarefa. A ordenação fecha o arquivo de jogos, usa o algoritmo existente, reabre o arquivo para reconstruir o Hash e reconecta os serviços. Os dados reais da raiz não são usados nos testes.

## Validação e execução

Com JDK 11 ou superior e ambiente gráfico:

```powershell
./scripts/compilar.ps1
./tests/run-tests.ps1 -Interface
```

O executor PowerShell cria bases únicas na pasta temporária do sistema e as remove ao terminar. A suíte aprovada inclui os três executáveis N:N, `CatalogoTest`, os 16 testes da `TestSuite` e `InterfaceGraficaTest`. A compilação foi feita com JDK 21 e `--release 11`. O executor Bash também foi atualizado; sua execução não foi validada neste Windows.

**Diagnóstico do código 5:** o encerramento relatado anteriormente foi reproduzido inclusive sem Swing quando a base estava dentro do repositório em `Documents`. A mesma reprodução e o teste gráfico completo terminaram com código zero em bases novas na pasta temporária do sistema. Não houve exceção Java nem confirmação do componente que encerrou o processo. O teste deixou de usar `out/` como local da base; a aplicação pode usar outra pasta de execução pelo script `executar.ps1 -Base`. Os arquivos reais não foram apagados nem migrados automaticamente.

Documentação de apoio: [interface e evidências](INTERFACE-FASE-III.md), [modelagem](MODELAGEM-FASE-III.md), [formulário oficial respondido](FORMULARIO-DECISOES-FASE-III.md) e [instruções de compilação/execução](../README.md).

## 4. Etapas finais — Todos

Os itens abaixo são separados da implementação das Partes 1–3. A validação automatizada foi concluída; a gravação e a entrega ainda dependem do grupo.

- [x] **4.1** Integrar os códigos dos três integrantes.
- [x] **4.2** Testar todos os CRUDs pela interface gráfica.
- [x] **4.3** Testar inclusão e exclusão de associações N:N.
- [x] **4.4** Testar as consultas N:N nos dois sentidos.
- [x] **4.5** Testar integridade referencial e impedir associações duplicadas.
- [x] **4.6** Testar persistência após fechar e reabrir o programa.
- [x] **4.7** Verificar as funcionalidades anteriores implementadas, com a suíte de regressão das Fases I/II.
- [ ] **4.8** Gravar o vídeo demonstrativo de até 3 minutos.
- [ ] **4.9** Publicar e conferir as alterações no GitHub.
- [ ] **4.10** Preparar o PDF único com documentação, formulário, link do GitHub e referência ao vídeo.
- [ ] **4.11** Realizar a entrega no Canvas.

O roteiro do vídeo está em `INTERFACE-FASE-III.md`. Não marcar gravação, publicação ou envio apenas pela existência de código ou roteiro. Uma revisão visual manual em diferentes resoluções também é recomendada antes da gravação.
