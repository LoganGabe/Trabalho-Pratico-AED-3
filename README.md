# Catálogo de Jogos

Trabalho Prático desenvolvido para a disciplina de **Algoritmos e Estruturas de Dados III**, do curso de Engenharia de Computação da **PUC Minas**.

## Integrantes

- Gabriel El-dine
- Gabriel Logan
- Kleber Rhuan

## Sobre o projeto

O projeto consiste no desenvolvimento de um **sistema de catálogo de jogos**, inspirado em plataformas de distribuição digital como a Steam.

O sistema permite armazenar e gerenciar informações sobre jogos e suas respectivas publicadoras, mantendo os dados de forma persistente em arquivos binários.

Ao longo do trabalho são aplicados conceitos estudados na disciplina, como estruturas de dados em memória secundária, indexação e organização de arquivos.

## Proposta

O sistema oferece funcionalidades para:

- Cadastrar, consultar, listar, atualizar e excluir jogos;
- Cadastrar, consultar, listar, atualizar e excluir publicadoras;
- Relacionar jogos às suas respectivas publicadoras;
- Consultar os jogos pertencentes a uma publicadora;
- Manter índices para acesso eficiente aos registros;
- Ordenar externamente o catálogo de jogos.

## Tecnologias

- Java
- Arquivos binários
- Hash Extensível
- Árvore B+
- Intercalação Balanceada

## Testes automatizados

Requer um **JDK 11 ou superior** (`java` e `javac` no PATH) e Bash. Não é necessário instalar bibliotecas de teste.

```bash
bash tests/run-tests.sh
```

O script compila o projeto e os testes em uma pasta temporária, executa a suíte e
remove os arquivos ao terminar. Cada cenário de integração possui sua própria base;
nenhum teste lê ou modifica a pasta `dados/` do projeto. A saída identifica cada teste
com `PASS` ou `FAIL`, mostra a causa das falhas e retorna código diferente de zero
se houver falha de compilação ou de teste.

A suíte contém quatro testes unitários (serialização de Jogo e Publicadora, Hash
Extensível e árvore B+) e doze cenários de integração correspondentes à checklist:

| Cenário | Validação automatizada |
| --- | --- |
| 1. Persistência básica | Cadastra pelo menu, encerra o processo e verifica os campos dos jogos e publicadoras após reabrir. |
| 2. Hash de Jogo | Consulta todos os jogos por ID e um ID inexistente. |
| 3. Hash de Publicadora | Consulta cada publicadora por ID e um ID inexistente. |
| 4. B+ 1:N | Compara os pares do índice e a listagem do menu com os IDs esperados, incluindo publicadora vazia. |
| 5. Update com mesma publicadora | Altera nome, preço e idiomas, crescendo e encolhendo o registro, sem duplicar ou perder associações. |
| 6. Update trocando publicadora | Confirma remoção da associação antiga, inclusão na nova e persistência dos novos dados. |
| 7. Delete de Jogo | Confirma ausência no arquivo, no Hash e na listagem da publicadora. |
| 8. Integridade da FK | Recusa cadastro e atualização com publicadora inexistente e verifica que os dados anteriores foram preservados. |
| 9. Integridade da Publicadora | Recusa exclusão com jogos e permite exclusão de publicadora vazia. |
| 10. Ordenação externa | Verifica ordem física e exibida, ignorando maiúsculas/minúsculas, com múltiplos blocos, registros realocados e excluídos. |
| 11. Hash após ordenação | Compara todos os campos e as consultas pelo Hash com a varredura física após ordenar. |
| 12. Persistência final | Combina transferência, exclusão, ordenação e reinício; verifica Hash, B+, dados e continuidade dos IDs. |

Os testes de integração iniciam o `Main` em JVMs separadas, com entradas de menu
predefinidas e limite de 20 segundos por processo. As regras de integridade são
exercitadas pelo menu, onde estão implementadas. O auxiliar `Snapshot` compara cada
registro ativo com a consulta pelo Hash e também verifica IDs excluídos e ausentes.
Os testes unitários de índices usam conjuntos maiores para exercitar divisões de
buckets e múltiplas folhas da B+, incluindo exclusão e reabertura dos arquivos.
