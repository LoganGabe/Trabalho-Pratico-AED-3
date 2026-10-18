# Formulário de Decisões de Projeto da Fase III

Autores: Gabriel El-dine, Gabriel Logan e Kleber Rhuan.

Respostas aos cinco enunciados oficiais fornecidos para a Fase III. O relacionamento N:N está integrado ao catálogo de jogos, com persistência binária, índices em memória secundária e interface gráfica definitiva.

## 1. Qual relacionamento N:N foi implementado e quais entidades estão envolvidas?

Foi implementado o relacionamento **Jogo ↔ Desenvolvedora**. Um jogo pode ser desenvolvido por vários estúdios e uma desenvolvedora pode participar de vários jogos. As entidades envolvidas são `Jogo` e `Desenvolvedora`; a associação é representada por `JogoDesenvolvedora`.

Os vínculos são persistidos em `dados/jogos_desenvolvedoras/jogos_desenvolvedoras.db`, usando o mesmo `Arquivo<T>` das demais entidades. Cada payload contém três valores `long`: `id` técnico, `jogoId` e `desenvolvedoraId`, totalizando 24 bytes, além do cabeçalho de registro do arquivo genérico. O relacionamento anterior **Publicadora → Jogo (1:N)** também foi preservado.

## 2. Como foi representada a chave composta do arquivo intermediário e como o sistema garante que uma mesma associação não seja cadastrada mais de uma vez?

A chave composta lógica é o par **`(jogoId, desenvolvedoraId)`**. Os dois campos são armazenados explicitamente no registro intermediário; sua combinação identifica a associação. O campo `id` adicional é apenas uma chave técnica exigida pelo contrato `Registro` e pelo índice primário de `Arquivo<T>`.

Antes de incluir um vínculo, `ArquivoJogoDesenvolvedora.create` chama `read(jogoId, desenvolvedoraId)`. Essa consulta usa a B+ de vínculos do jogo e recupera as associações pelo Hash, comparando os dois IDs do par. Se o vínculo já existe, a inclusão retorna `false`, sem gravar outro registro nem consumir um novo ID técnico. A interface informa que a associação já existe.

O sistema compara os campos reais; não usa um hash do par como prova de unicidade. Assim, uma colisão de hash não é confundida com uma associação repetida. A mesma validação permanece após fechar e reabrir a aplicação. IDs inválidos e entidades inexistentes também são recusados antes da inclusão. O projeto pressupõe uma única instância da aplicação por base de dados.

## 3. Quais índices foram criados para o arquivo intermediário? Para cada índice, informe a chave utilizada, a estrutura de indexação escolhida e a consulta que o índice permite realizar eficientemente.

| Índice e arquivo | Chave utilizada | Estrutura | Consulta atendida |
| --- | --- | --- | --- |
| Primário: `dados/indices/jogos_desenvolvedoras.dir` e `.bkt` | `id` técnico do vínculo → endereço físico do registro | Hash Extensível persistido | Recuperar diretamente uma associação pelo seu ID técnico, sem varrer o arquivo intermediário. |
| Por jogo: `dados/indices/arvore_jogo_vinculo.db` | Par ordenado `(jogoId, vinculoId)` | Árvore B+ persistida | Recuperar todos os vínculos de um jogo e, por eles, suas desenvolvedoras. |
| Por desenvolvedora: `dados/indices/arvore_desenvolvedora_vinculo.db` | Par ordenado `(desenvolvedoraId, vinculoId)` | Árvore B+ persistida | Recuperar todos os vínculos de uma desenvolvedora e, por eles, seus jogos. |

Nas duas B+, o primeiro componente agrupa as associações de uma entidade; o segundo distingue os vínculos dentro desse grupo. As árvores armazenam IDs técnicos dos vínculos, não endereços físicos das entidades. O Hash resolve esses IDs para posições no arquivo intermediário. Os IDs de Jogo e Desenvolvedora encontrados no vínculo são consultados nos índices primários dos respectivos arquivos.

Essa organização reutiliza `HashExtensivel`, `ArvoreBMais` e `Arquivo<T>`, preservando a indexação das fases anteriores. Os nomes internos dos campos da classe B+ foram mantidos por compatibilidade, mas as duas novas instâncias usam os pares descritos na tabela.

## 4. Como são realizadas as consultas nos dois sentidos do relacionamento N:N?

**Desenvolvedoras de um jogo:** `RelacionamentoJogoDesenvolvedora.desenvolvedorasDoJogo(jogoId)` delega a `ArquivoJogoDesenvolvedora.readDesenvolvedoras`. A B+ `arvore_jogo_vinculo.db` encontra os IDs dos vínculos daquele jogo. Cada registro intermediário é lido pelo Hash e dele é extraído o `desenvolvedoraId`. A interface consulta cada ID pelo `ServicoDesenvolvedora` e exibe os dados completos das desenvolvedoras.

**Jogos de uma desenvolvedora:** `RelacionamentoJogoDesenvolvedora.jogosDaDesenvolvedora(desenvolvedoraId)` delega a `ArquivoJogoDesenvolvedora.readJogos`. A B+ `arvore_desenvolvedora_vinculo.db` encontra os IDs dos vínculos daquela desenvolvedora. Cada associação é lida pelo Hash, obtendo-se seu `jogoId`. A interface consulta esses IDs pelo `ServicoJogo` e apresenta os jogos, incluindo preço, lançamento, publicadora e idiomas.

A busca em cada B+ desce até a folha correspondente e percorre as folhas encadeadas enquanto houver chaves daquele grupo. Portanto, as consultas não dependem de percorrer todos os registros do arquivo intermediário. Se não houver associações, retornam uma lista vazia e a interface mostra zero resultados. Os índices e os vínculos são mantidos após reiniciar o sistema.

## 5. Como o sistema mantém a integridade referencial entre o arquivo intermediário e as entidades relacionadas nas operações de inclusão, alteração e exclusão?

**Inclusão:** `RelacionamentoJogoDesenvolvedora.associar` exige IDs positivos e consulta os arquivos de Jogo e Desenvolvedora para confirmar que ambos existem e estão ativos. Somente depois chama a inclusão do vínculo, que verifica a duplicidade do par e atualiza o arquivo intermediário, o Hash e as duas B+. Cadastro e atualização de jogo também exigem publicadora existente.

**Alteração:** as operações dos serviços preservam o ID das entidades. Mudar nome, país, preço ou outros atributos não altera as chaves estrangeiras dos vínculos N:N. Não há edição direta de um par pela interface: uma troca de associação é feita removendo o vínculo antigo e incluindo o novo, com nova validação. Quando um jogo muda de publicadora, `ServicoJogo.atualizar` remove o par antigo da B+ 1:N e insere o novo. Uma FK inválida é recusada antes de alterar o jogo.

**Exclusão:** a política adotada é o **bloqueio de exclusão enquanto houver vínculos**. `ServicoJogo.excluir` recusa remover um jogo com desenvolvedoras associadas; `ServicoDesenvolvedora.excluir` recusa remover uma desenvolvedora com jogos associados. O usuário deve remover as associações explicitamente pela interface antes de excluir a entidade. Uma publicadora com jogos também não pode ser excluída. Ao remover um vínculo, o gerenciador exclui o registro intermediário e suas entradas nos dois índices secundários; o CRUD genérico mantém seu Hash.

As alterações N:N usam um marcador `dados/indices/jogos_desenvolvedoras.pendente`. Se uma operação fica incompleta, o gerenciador bloqueia novas operações naquela sessão e reconstrói os índices ao reabrir. Índices N:N ausentes também são reconstruídos com os registros ativos. Esse mecanismo não equivale a transações ACID nem garante recuperação de qualquer corrupção dos dados.

As regras são verificadas por testes com as entidades definitivas: IDs inexistentes ou excluídos, pares duplicados, atualização de entidades com vínculos, troca e validação de publicadora, bloqueios de exclusão, remoção de associação, reinício e reconstrução dos índices. A interface usa esses serviços, sem contornar as validações.
