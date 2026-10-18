# Interface gráfica e validação da Fase III

Responsável pela Parte 3: Kleber Rhuan. Validação local em 18/10/2026.

## Uso da interface

Execute `Main` sem argumentos para abrir o catálogo Swing. A navegação ocorre em quatro abas:

| Aba | Funcionalidades |
| --- | --- |
| Jogos | Novo, consulta pelo ID, listagem, edição, exclusão e ordenação externa por nome. |
| Publicadoras | CRUD completo e consulta dos jogos da publicadora selecionada (1:N). |
| Desenvolvedoras | CRUD completo com bloqueio de exclusão quando houver jogos associados. |
| Associações e consultas N:N | Seletores de jogo/desenvolvedora, associação, remoção e consultas nos dois sentidos. |

Cadastre uma publicadora antes do primeiro jogo. Para editar ou excluir, selecione uma linha na tabela. A consulta por ID restringe a tabela ao registro encontrado; “Listar todos / Atualizar” restaura a listagem. Os formulários apresentam todos os atributos persistidos, inclusive idiomas, data e descrição. Exclusões e remoção de vínculos pedem confirmação.

As consultas N:N mostram dados das entidades e a quantidade de resultados na barra de status. Quando não existem resultados, a tabela fica vazia e a contagem é zero. Duplicatas, registro ausente, valores inválidos e exclusões bloqueadas recebem mensagens na própria janela. A interface permanece disponível enquanto as operações de arquivo são executadas em segundo plano, com controles temporariamente desabilitados.

## Evidências por item da Parte 3

| Itens | Implementação | Validação |
| --- | --- | --- |
| 3.1–3.2 | Swing, `JanelaPrincipal` e navegação por abas; `Main` inicia a janela. | Compilação com `--release 11`; abertura, navegação e fechamento no teste Swing. |
| 3.3–3.5 | Formulários e tabelas de Jogos, Publicadoras e Desenvolvedoras. | Teste Swing executa inclusão, edição, consulta de ID existente/inexistente, listagem e exclusão de cada entidade. |
| 3.6–3.7 | Seletores com ID/nome e botões de associação/remoção. | Teste Swing cria vínculo, verifica duplicata e remove; `CatalogoTest` valida integridade com entidades reais. |
| 3.8–3.9 | Consultas N:N usando `RelacionamentoJogoDesenvolvedora` e resolução dos IDs pelos serviços. | Consultas nos dois sentidos antes/depois da remoção; testes de índices e reabertura. |
| 3.10 | Ordenação externa na aba Jogos e consulta 1:N na aba Publicadoras. | Teste Swing completo aprovado, incluindo fechamento/reabertura com vínculo ativo, ordenação e repetição das consultas. Teste de sessão verifica Hash/B+/N:N; suíte anterior verifica múltiplas intercalações. |
| 3.11 | Modelo de entidades, organização física, camadas e regras em `MODELAGEM-FASE-III.md`. | Confrontado com entidades, serviços, índices e testes atuais. |
| 3.12 | Cinco respostas oficiais em `FORMULARIO-DECISOES-FASE-III.md`. | Respostas alinhadas aos cinco enunciados recebidos e conferidas com classes, arquivos, chaves e regras existentes. |
| 3.13 | README, script PowerShell de compilação e executores de testes PowerShell/Bash. | Compilação e execução da suíte PowerShell com interface em base isolada. |

## Resultado dos testes

Foram aprovados os três executáveis N:N existentes, `CatalogoTest`, os 16 testes da `TestSuite` e o teste gráfico completo `InterfaceGraficaTest`, incluindo a ordenação. O teste de regras foi ampliado para conferir atualização de entidades com vínculos, troca de publicadora e preservação dos dados após uma FK inválida. O teste gráfico também reabre a janela com registros e associações ativos, confere os campos persistidos, ordena e consulta novamente. Ao final, verifica que `Main` abre a interface gráfica.

O encerramento com código 5 investigado anteriormente foi reproduzido sem Swing quando a base estava no repositório em `Documents`. A mesma reprodução e a suíte completa passaram com bases na pasta temporária do sistema. O executor PowerShell agora usa esse diretório; o script `scripts/executar.ps1 -Base` permite escolher outra localização para o catálogo. Não foi confirmado qual componente encerrou o Java. Não foi necessário alterar a ordenação existente nem configurações de segurança.

A compilação foi verificada com JDK 21 e `--release 11`. A suíte gráfica aciona componentes reais e confirma resultados nas tabelas; uma revisão visual manual em outras resoluções e escalas continua recomendada. O script Bash foi atualizado, mas não foi executado nesta validação Windows.

O estado inicial da checklist de continuidade descrevia uma integração ainda parcial. O código atual já contém Desenvolvedora definitiva e seus serviços, que foram reutilizados. A implementação desta etapa acrescenta a interface, sua sessão de arquivos e `ServicoPublicadora` para proteger a exclusão também fora do console. A implementação dos índices N:N foi preservada. O formulário foi reescrito para responder exatamente aos cinco enunciados oficiais, e a opção de console foi removida de `Main` para atender ao requisito da interface definitiva.

## Preparação do vídeo de até três minutos

Roteiro sugerido, a ser gravado pelo grupo:

| Tempo | Demonstração |
| --- | --- |
| 0:00–0:25 | Abrir a interface e apresentar as três entidades já cadastradas. |
| 0:25–1:10 | Associar um jogo a duas desenvolvedoras e uma delas a outro jogo; tentar repetir um par e mostrar a mensagem. |
| 1:10–1:40 | Consultar desenvolvedoras do jogo e jogos da desenvolvedora; remover um vínculo e repetir a consulta. |
| 1:40–2:10 | Mostrar bloqueio de exclusão com vínculo, consulta 1:N e ordenação externa. |
| 2:10–2:35 | Fechar/reabrir a aplicação e mostrar persistência; apresentar o arquivo intermediário e os pares dos dois índices no código. |
| 2:35–3:00 | Indicar classes do N:N, serviços e testes de consistência. |

Para demonstrar persistência, mantenha alguns registros cadastrados antes de fechar. O teste automatizado limpa sua própria base; use uma base demonstrativa separada para o vídeo. A implementação foi publicada em `origin/main`, com hash remoto conferido. A gravação, composição do PDF final e envio ao Canvas continuam como etapas finais do grupo.
