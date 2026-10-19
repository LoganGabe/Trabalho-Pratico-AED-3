# Catálogo de Jogos

Trabalho Prático de **Algoritmos e Estruturas de Dados III**, do curso de Engenharia de Computação da **PUC Minas**.

**Integrantes:** Gabriel El-dine, Gabriel Logan e Kleber Rhuan.

## Sobre o projeto

O trabalho é um catálogo de jogos inspirado em plataformas de distribuição digital, como a Steam. Sua proposta é organizar informações sobre jogos, publicadoras e desenvolvedoras, permitindo cadastrar, consultar, editar e excluir registros e relacionar cada jogo aos estúdios responsáveis pelo seu desenvolvimento.

A aplicação possui interface gráfica em Java Swing e permite consultar os relacionamentos e ordenar o catálogo. O trabalho aplica conceitos da disciplina, como persistência em arquivos binários, Hash Extensível, Árvore B+ e ordenação externa.

## Requisitos

- **JDK 11 ou superior**, com `java` e `javac` da mesma instalação disponíveis no PATH.
- Ambiente gráfico para abrir a aplicação.

Não é necessário instalar bibliotecas externas. Os dados ficam na pasta `dados/` do diretório de execução e são preservados entre execuções. Use uma única instância da aplicação por base de dados.

## Como executar

1. Abra a pasta do projeto no **VS Code**, com o **Extension Pack for Java** instalado.
2. Abra o arquivo `src/Main.java`.
3. Clique em **Run** acima do método `main` para abrir a interface gráfica.
