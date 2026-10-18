# Fase III — checklist de continuidade

## Regras para continuar

1. Seguir exatamente as tarefas abaixo, na ordem de cada seção. Não acrescentar funcionalidades fora da lista.
2. Trabalhar em uma etapa por vez. Implementar, compilar, testar e conferir o resultado antes de marcar como concluída.
3. **Só avançar quando a etapa atual estiver corretamente implementada e testada. Se houver erro ou dependência pendente, corrigir ou resolver antes de continuar.**
4. Código preparado ou teste com entidade auxiliar não significa integração concluída. Conferir o funcionamento com as entidades e arquivos definitivos.
5. Respeitar a divisão de responsabilidades. A interface gráfica pertence ao Kleber e está separada do trabalho de El-dine.
6. Preservar as funcionalidades das Fases I e II e não apagar os dados do catálogo para fazer testes. Usar bases temporárias.
7. Registrar brevemente como cada etapa foi testada. Marcar `[x]` somente após a validação.
8. **Manter sempre em mente o estado completo do repositório:** o que já foi construído, o que foi feito nesta etapa, o que continua inacabado e o que deve ser concluído por outro integrante. Antes de começar cada tarefa, conferir o código existente e a checklist para não duplicar trabalho, sobrescrever implementações ou tratar uma preparação parcial como funcionalidade pronta.
9. Ao terminar cada etapa, atualizar esse contexto: informar o que mudou, o que está funcionando e testado, quais pendências permanecem e quem deve concluí-las. Se a continuação depender de outro integrante, indicar claramente a dependência e o ponto de integração, sem assumir a tarefa dele nem marcar o item como concluído.

## Estado inicial

A parte de Logan está implementada e testada localmente. As alterações ainda precisam ser incluídas no GitHub. Antes de continuar em outro computador, conferir se todos os arquivos foram recebidos.

Arquivos preparados por Logan (documentação reunida em `docs/`):

- `src/entidades/JogoDesenvolvedora.java`
- `src/persistencia/ArquivoJogoDesenvolvedora.java`
- `src/persistencia/RelacionamentoJogoDesenvolvedora.java`
- `tests/JogoDesenvolvedoraTest.java`
- `tests/ArquivoJogoDesenvolvedoraTest.java`
- `tests/RelacionamentoJogoDesenvolvedoraTest.java`
- `tests/run-tests.sh` atualizado
- `docs/PROGRESSO-FASE-III.md`

`RelacionamentoJogoDesenvolvedora` já prepara parte da validação de existência das entidades, mas ainda não está conectado ao CRUD definitivo de Desenvolvedora nem à interface. O teste atual usa um registro auxiliar no lugar dessa entidade. Isso não conclui os itens de integração do El-dine.

## O que foi feito no computador de Logan

Estado conferido em 18/10/2026, na cópia local `C:\Faculdade\AED3\TP\Trabalho-Pratico-AED-3`. Os arquivos novos ainda estão sem commit, e `tests/run-tests.sh` está modificado. Não presumir que essas mudanças já estão disponíveis no GitHub.

| Arquivo | O que foi construído |
| --- | --- |
| `src/entidades/JogoDesenvolvedora.java` | Registro intermediário com `id` técnico, `jogoId`, `desenvolvedoraId` e serialização binária de 24 bytes. |
| `src/persistencia/ArquivoJogoDesenvolvedora.java` | Inclusão sem duplicatas, consulta do par, exclusão, listagem, consultas nos dois sentidos e reconstrução dos índices quando necessário. |
| `src/persistencia/RelacionamentoJogoDesenvolvedora.java` | Preparação da integração: recebe os três arquivos abertos, verifica existência das entidades antes de associar e oferece desassociação e consultas. |
| `tests/JogoDesenvolvedoraTest.java` | Testa serialização e rejeição de dados truncados. |
| `tests/ArquivoJogoDesenvolvedoraTest.java` | Testa N:N, duplicatas, exclusões, reabertura, divisões da árvore e reconstrução dos índices. |
| `tests/RelacionamentoJogoDesenvolvedoraTest.java` | Testa a validação com Jogo real e `RegistroTeste` auxiliar no lugar de Desenvolvedora. |
| `tests/run-tests.sh` | Inclui os três testes novos, além da suíte anterior, em bases temporárias. |
| `docs/PROGRESSO-FASE-III.md` | Registra as decisões, limitações e operações implementadas por Logan. |

Na última execução, o teste novo de integração e os 16 testes anteriores passaram. Os testes de serialização e dos índices também passaram nas etapas anteriores. Reexecutar após a integração; esses resultados não validam a interface definitiva.

### Estruturas existentes que devem ser reutilizadas

Antes de cada tarefa, ler o código atual destas estruturas. **Não reimplementar algo que o repositório já oferece.**

| Estrutura/arquivo | Papel e orientação |
| --- | --- |
| `src/persistencia/Registro.java` | Contrato das entidades persistidas. Desenvolvedora deve implementá-lo, seguindo o padrão de Jogo e Publicadora. |
| `src/persistencia/Arquivo.java` | CRUD binário genérico e atribuição do ID técnico. Reutilizar para Desenvolvedora; não criar outro formato de armazenamento sem necessidade da checklist. |
| `src/persistencia/HashExtensivel.java` | Índice primário usado por Arquivo. Não usar um hash do par como identificador único: colisões não representam unicidade. |
| `src/persistencia/ArvoreBMais.java` | Reutilizada em dois arquivos exclusivos do N:N. Os nomes internos são de Publicadora/Jogo, mas as operações recebem pares de longs. Não é necessário mudar essa estrutura para integrar os CRUDs. |
| `src/entidades/Jogo.java` e `src/entidades/Publicadora.java` | Entidades existentes que devem continuar funcionando. O relacionamento anterior com Publicadora permanece. |
| `src/persistencia/OrdenacaoExterna.java` | Ordenação existente a ser preservada e posteriormente acessada pela interface definitiva. |
| `src/menu/Menu.java` | Menu de console atual. Contém chamadas de CRUD e regras anteriores; deve ser lido como referência para a integração. Ainda não chama o relacionamento N. |
| `src/Main.java` | Atualmente inicia o menu de console. A mudança para iniciar a interface definitiva cabe ao Kleber na etapa da interface. |

### Arquivos binários e índices do relacionamento

Criados automaticamente pelo gerenciador, relativos à pasta de execução:

- `dados/jogos_desenvolvedoras/jogos_desenvolvedoras.db`: associações serializadas.
- `dados/indices/jogos_desenvolvedoras.dir` e `.bkt`: Hash do ID técnico para o endereço do registro.
- `dados/indices/arvore_jogo_vinculo.db`: pares `(jogoId, id do vínculo)`.
- `dados/indices/arvore_desenvolvedora_vinculo.db`: pares `(desenvolvedoraId, id do vínculo)`.
- `dados/indices/jogos_desenvolvedoras.pendente`: marcador temporário para recuperação de atualização incompleta.

Não editar esses arquivos manualmente. Usar as operações do gerenciador. Os arquivos de dados utilizados nos testes anteriores foram criados em bases temporárias; isso não significa que o catálogo real já tenha associações cadastradas. A reconstrução dos índices não é uma transação nem uma garantia de recuperação de corrupção dos dados.

## O que está inacabado e onde continuar

Os caminhos novos abaixo são destinos sugeridos para arquivos ainda inexistentes. As mudanças devem atender somente aos itens correspondentes da checklist, sem redesenhar a persistência pronta.

| Responsável / itens | Arquivos ou estruturas a criar, alterar ou conectar | O que falta concluir |
| --- | --- | --- |
| El-dine — 2.1 | Criar `src/entidades/Desenvolvedora.java`. | Implementar Registro, construtor vazio, campos da entidade e serialização, compatíveis com Arquivo. |
| El-dine — 2.2–2.4 | Criar a camada de CRUD/regras de Desenvolvedora e usar `Arquivo<Desenvolvedora>`. O nome dessa nova classe pode seguir a organização escolhida pelo grupo. | CRUD completo, armazenamento binário e consulta pelo índice primário existente. Não é necessário alterar HashExtensivel ou Arquivo apenas para adicionar uma entidade. |
| El-dine — 2.5 e 2.8 | Conectar `src/persistencia/RelacionamentoJogoDesenvolvedora.java` aos arquivos reais de Jogos, Desenvolvedoras e vínculos. | Substituir o uso apenas demonstrativo pela utilização nas regras de negócio. A estrutura atual aceita `Arquivo<Desenvolvedora>` por meio de `Arquivo<? extends Registro>`; não exige alteração obrigatória do gerenciador N. |
| El-dine — 2.6–2.7 | Alterar as operações de exclusão/alteração nas camadas de regras dos CRUDs de Jogo e Desenvolvedora; consultar `ArquivoJogoDesenvolvedora` para verificar associações. | Definir com o grupo se exclusão com vínculos será bloqueada ou removerá associações. Aplicar a regra em todos os caminhos, sem deixar vínculos órfãos. A exclusão de Jogo hoje está em `src/menu/Menu.java`: a regra deverá ser disponibilizada na camada de negócio usada pela interface, não ficar exclusiva do console. |
| El-dine — 2.9 | Complementar `tests/RelacionamentoJogoDesenvolvedoraTest.java`, ou adicionar testes equivalentes com a entidade definitiva; atualizar `tests/run-tests.sh` se adicionar executáveis de teste. | Testar Desenvolvedora real, entidade ausente/excluída, duplicatas e a política de exclusão escolhida. `RegistroTeste` não substitui esses testes finais. |
| Kleber — 3.1–3.5 | Criar os arquivos da interface gráfica em um pacote próprio, por exemplo `src/interfacegrafica/`; conectar as telas aos CRUDs e regras. | Tecnologia, navegação e telas definitivas. O pacote é sugestão, não uma estrutura já existente. |
| Kleber — 3.6–3.9 | Conectar as telas de associações a `RelacionamentoJogoDesenvolvedora`. | Usar `associar`, `desassociar`, `desenvolvedorasDoJogo` e `jogosDaDesenvolvedora`. Resolver os IDs retornados nos respectivos CRUDs para exibir os dados completos. |
| Kleber — 3.10 | Integrar `src/Main.java`, a interface, os CRUDs e `src/persistencia/OrdenacaoExterna.java`; consultar `src/menu/Menu.java` como referência. | Iniciar a interface definitiva e disponibilizar as funcionalidades anteriores. Reutilizar regras de negócio, sem duplicá-las dentro das telas. |
| Kleber — 3.11–3.13 | Atualizar `README.md`, documentação/modelagem e o documento do Formulário de Decisões. | Documentação da Fase III, respostas às cinco perguntas e instruções verificadas de compilação/execução do programa final. O caminho do formulário deve ser definido pelo grupo. |
| Todos — 4.1–4.11 | Código integrado, testes, GitHub e artefatos finais. | Conferir o recebimento dos arquivos locais de Logan, validar o programa completo e concluir os materiais de entrega conforme a seção 4. |

**El-dine deve concluir as regras e sua integração sem implementar as telas do Kleber. Kleber deve usar as regras disponibilizadas pelo El-dine, sem contorná-las chamando diretamente a inclusão de vínculos. Qualquer mudança necessária no gerenciador N deve ser coordenada com Logan e preservar os testes existentes.**

## 1. Logan — Relacionamento N e índices

Responsável pela persistência e pelas estruturas de indexação.

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
- [x] **1.11** Testar persistência, índices e reconstrução, caso implementada.

**Entrega:** relacionamento N persistido e indexado, com operações de inclusão, exclusão e consulta funcionando na camada de persistência.

A chave composta é o par de campos da associação; não há uma classe separada para ela. O `id` adicional é técnico, exigido pela persistência existente. As consultas retornam os IDs das entidades relacionadas.

## 2. Gabriel El-dine — CRUD e integridade referencial

Responsável pelas entidades e pelas regras de negócio. **Esta seção não inclui implementar a interface gráfica.**

Executar na ordem. **Validar cada item antes de iniciar o seguinte.** Reutilizar a persistência e as operações N já implementadas por Logan.

- [ ] **2.1** Criar a entidade `Desenvolvedora`.
- [ ] **2.2** Implementar o CRUD completo de Desenvolvedora.
- [ ] **2.3** Integrar o CRUD à persistência binária existente.
- [ ] **2.4** Garantir a indexação por chave primária de Desenvolvedora.
- [ ] **2.5** Validar a existência de Jogo e Desenvolvedora antes de associá-los.
- [ ] **2.6** Definir e implementar as regras de exclusão de entidades que possuem associações.
- [ ] **2.7** Garantir a integridade referencial nas operações de inclusão, alteração e exclusão.
- [ ] **2.8** Integrar as operações N às regras de negócio do sistema.
- [ ] **2.9** Testar as validações e a consistência dos relacionamentos.

**Entrega:** CRUD de Desenvolvedora e regras de integridade referencial funcionando.

Para concluir 2.5 e 2.8, conectar e testar a camada `RelacionamentoJogoDesenvolvedora` com os arquivos reais. Para 2.6, combinar com o grupo a regra de exclusão e implementá-la antes de continuar. Não considerar o teste com registro auxiliar como validação da entidade definitiva.

## 3. Kleber Rhuan — Interface gráfica e documentação

Responsável pela interface definitiva e pela organização da entrega. **Este trabalho permanece separado do CRUD e das regras de negócio de El-dine.**

Executar na ordem. **Cada tela ou funcionalidade precisa funcionar corretamente antes de seguir para a próxima.** Se depender de uma operação ainda não entregue, concluir essa integração antes de marcar o item.

- [ ] **3.1** Definir a tecnologia e a estrutura da interface gráfica (sugestão: Java Swing).
- [ ] **3.2** Criar a janela principal e a navegação.
- [ ] **3.3** Criar as telas de CRUD de Jogos.
- [ ] **3.4** Criar as telas de CRUD de Publicadoras.
- [ ] **3.5** Criar as telas de CRUD de Desenvolvedoras.
- [ ] **3.6** Criar a interface para associar Jogo e Desenvolvedora.
- [ ] **3.7** Criar a interface para remover associações.
- [ ] **3.8** Criar a consulta de desenvolvedoras de um jogo.
- [ ] **3.9** Criar a consulta de jogos de uma desenvolvedora.
- [ ] **3.10** Integrar à interface as funcionalidades anteriores, incluindo ordenação externa e consulta 1.
- [ ] **3.11** Atualizar a documentação do projeto e a modelagem, se necessário.
- [ ] **3.12** Responder às cinco perguntas do Formulário de Decisões.
- [ ] **3.13** Atualizar o README com as instruções de compilação e execução.

**Entrega:** interface gráfica definitiva e documentação atualizada.

As telas devem chamar as operações de persistência e as regras de negócio existentes. Uma tela pronta visualmente, mas sem operações funcionando, não conclui o item. As instruções de testes já existentes no README não substituem as instruções de execução do programa final.

## 4. Etapas finais — Todos

Realizar após a conclusão das três partes. **Não avançar para vídeo e entrega enquanto os testes anteriores apresentarem falhas.**

- [ ] **4.1** Integrar os códigos dos três integrantes.
- [ ] **4.2** Testar todos os CRUDs pela interface gráfica.
- [ ] **4.3** Testar inclusão e exclusão de associações N.
- [ ] **4.4** Testar as consultas N nos dois sentidos.
- [ ] **4.5** Testar integridade referencial e impedir associações duplicadas.
- [ ] **4.6** Testar a persistência após fechar e reabrir o programa.
- [ ] **4.7** Verificar se todas as funcionalidades das Fases I e II continuam funcionando.
- [ ] **4.8** Gravar o vídeo demonstrativo de até 3 minutos, mostrando a interface, as operações N e os principais elementos do código e dos índices.
- [ ] **4.9** Conferir e atualizar o repositório GitHub.
- [ ] **4.10** Preparar o PDF único com documentação, Formulário de Decisões e link do GitHub.
- [ ] **4.11** Realizar a entrega no Canvas.

Os testes isolados já realizados ajudam na integração, mas não concluem os testes do programa final. Conferir novamente os itens 4.2 a 4.7 com as três partes integradas.

## Como validar e registrar o progresso

Com JDK 11 ou superior e Bash disponíveis, executar na raiz do repositório:

```bash
bash tests/run-tests.sh
```

Os testes usam bases temporárias. A suíte atual cobre a persistência existente, as associações N e a preparação da integração; ainda não cobre a interface gráfica definitiva.

Para cada item concluído, registrar:

```text
Item:
O que foi implementado:
Como foi testado:
Resultado:
O que foi feito nesta etapa e como se conecta ao código existente:
O que continua inacabado:
O que depende de outro integrante, quem é o responsável e onde deve integrar:
```

**Regra final: implementar → compilar → testar → corrigir → conferir → marcar como concluído → avançar. Nunca marcar uma etapa apenas porque o código foi escrito.**

