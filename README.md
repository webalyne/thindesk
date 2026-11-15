# Thindesk - dashboard administrativo

Extensão do sistema fornecido pelo professor, mantendo Java, Spring Boot, Thymeleaf e MongoDB.

## Execução

Requisitos: Java 21, MongoDB 8 e Maven Wrapper incluído no projeto.

Para iniciar um MongoDB separado com Docker:

```sh
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

No Windows, substitua ./mvnw por .\mvnw.cmd. Abra http://127.0.0.1:8080 e entre com **admin / thindesk123**, credenciais de demonstração. Configure THINDESK_USER e THINDESK_PASSWORD para alterá-las.

O perfil demo usa mongodb://127.0.0.1:27028/thindesk_demo e inclui exemplos somente quando a coleção está vazia. Sem esse perfil, o padrão é mongodb://127.0.0.1:27017/thindesk. MONGO_URI permite configurar outra conexão. Não há instalação de MongoDB embutida no aplicativo.

Também é possível gerar o executável:

```sh
./mvnw package
java -jar target/thindesk-1.0.0.jar --spring.profiles.active=demo
```

A configuração limita o servidor a 127.0.0.1. PORT altera a porta.

## Recursos

- Indicadores gerais, gráfico por período, distribuição por status e tipos mais atendidos.
- Filtros por período, status, tipo, técnico, usuário e busca.
- Cadastro e edição de chamados, andamento, considerações técnicas e histórico.
- Autenticação por sessão, proteção CSRF e controle de versão nas alterações.
- Atualização manual e automática a cada 60 segundos.
- Consulta de registros antigos sem inventar datas.

Os cartões de total, abertos, andamento e a distribuição usam a **data de abertura**. Atendidos, ranking, gráfico e tempo médio usam a **data de conclusão**. Um chamado antigo atendido no período entra na análise de atendimentos, mesmo que tenha sido aberto antes.

## Documentação

- [Requisitos, modelagem e decisões](docs/PROJETO.md)
- [Endpoints, exemplos e erros](docs/API.md)
- [Evidências de testes](docs/TESTES.md)
- [Roteiro do vídeo](docs/VIDEO.md)

## Testes

```sh
./mvnw test
```

Os testes de integração exigem TEST_MONGO_URI e usam um banco temporário exclusivo. Exemplo no PowerShell:

```powershell
$env:TEST_MONGO_URI = "mongodb://127.0.0.1:27028"
.\mvnw.cmd test
```

Sem TEST_MONGO_URI, apenas os testes unitários são executados; os testes de integração ficam ignorados. O workflow configura um MongoDB para executar a suíte completa.

## Base original

O primeiro commit preserva o projeto do professor, com as configurações de conexão e login substituídas por variáveis de ambiente. O banco continua sendo MongoDB. A configuração Maven foi organizada com o parent Spring Boot e Java 21; as anotações JPA de Cliente foram removidas porque o projeto usa documentos MongoDB.

A listagem REST de chamados passou a retornar um objeto paginado. O contrato atualizado está em docs/API.md.

## Gitflow

main: versão de entrega. develop: integração. feature/api: endpoints e persistência. feature/dashboard: interface e testes. release/1.0.0: preparação da versão. Os commits usam mensagens curtas e minúsculas.

## Apresentação

O vídeo de até três minutos ainda precisa ser gravado e incluído em video/apresentacao.mp4. O roteiro está em docs/VIDEO.md.
