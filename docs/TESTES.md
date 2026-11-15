# Testes do Thindesk

Execução em 15/11/2026, com Java 21.0.13, Spring Boot 3.4.13 e MongoDB 8.0.28.

| Suíte | Testes | Falhas | Erros | Ignorados |
| --- | --- | --- | --- | --- |
| DashboardIntegrationTest | 20 | 0 | 0 | 0 |
| FiltroChamadoTest | 4 | 0 | 0 | 0 |
| Total | 24 | 0 | 0 | 0 |

Os relatórios originais estão em evidencias/com.pfc.thindesk.DashboardIntegrationTest.txt e evidencias/com.pfc.thindesk.FiltroChamadoTest.txt.

## Integração

Os testes usam o MongoDB real e bancos temporários com prefixo thindesk_test_ e identificador aleatório. Cada teste começa com seus próprios registros. O banco temporário é removido ao final; o banco de demonstração não é alterado pela suíte.

Foram verificados: autenticação, CSRF, indicadores, datas de abertura e conclusão, ranking, filtros combinados, registros sem data, períodos zerados, parâmetros inválidos, cadastro e validação, controle de versão, fluxo dos status, considerações com autor autenticado, pedido de chamado inexistente e renderização da página.

O cenário de datas confirma que um chamado aberto antes do período e concluído dentro dele entra na quantidade de atendimentos. O cenário de registros antigos confirma que a data permanece ausente.

## Interface

Foi verificado no navegador o login, o carregamento dos indicadores, o cadastro de um chamado, o início do atendimento, o registro de uma consideração e a conclusão. O chamado permaneceu no banco após reiniciar a aplicação.

A verificação identificou uma falha na segunda atualização da tabela, causada pela substituição de um elemento usado na contagem. A referência foi corrigida e a atualização manual foi repetida com a lista carregada e sem aviso de erro.

![Dashboard em execução](evidencias/dashboard.jpg)

Não foi realizado teste de carga, auditoria completa de acessibilidade ou teste em múltiplos navegadores. O workflow de CI está configurado, mas sua execução remota depende da publicação.

## Reprodução

Inicie o MongoDB e configure TEST_MONGO_URI, sem o nome do banco:

```powershell
$env:TEST_MONGO_URI = "mongodb://127.0.0.1:27028"
.\mvnw.cmd test
```

Sem a variável, os 20 testes de integração ficam ignorados e apenas os quatro testes unitários são executados.

## Horários e navegação

As páginas Dashboard, Chamados, Clientes e Horários são renderizadas pela suíte de integração. O formulário de horários recebe o objeto esperado pelo Thymeleaf e usa as mesmas rotas do controller. O cadastro valida o dia e exige um horário final posterior ao inicial, no mesmo dia. Exclusões usam POST com CSRF. Os testes cobrem listagem, gravação, intervalo inválido, proteção CSRF e exclusão; GET não altera os registros. A página de clientes mantém a consulta dos dados cadastrados, sem links para ações inexistentes.

No navegador, o ícone de horários abriu a página, um registro de teste foi cadastrado e apareceu na listagem, e sua exclusão foi confirmada. A página de clientes também foi aberta sem erro. O registro de teste foi removido depois da conferência.

![Cadastro de horário conferido no navegador](evidencias/horarios.jpg)
