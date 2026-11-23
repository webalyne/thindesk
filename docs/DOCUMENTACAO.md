# Thindesk - documentação técnica


---

# Thindesk - análise e projeto

## Escopo

O dashboard administrativo acompanha os chamados do Thindesk. A interface consome endpoints implementados no mesmo projeto Spring Boot e usa a coleção chamados do MongoDB.

## Requisitos funcionais

| Código | Requisito | Implementação |
| --- | --- | --- |
| RF01 | Indicadores gerais de chamados técnicos | Total aberto no período, abertos, em andamento, atendidos e tempo médio. |
| RF02 | Chamados atendidos por período | Gráfico pela data de conclusão, agrupado por dia, semana ou mês, com tabela de valores. |
| RF03 | Situação dos chamados | Distribuição por status e situação individual na lista e no detalhe. |
| RF04 | Tipos de chamados mais atendidos | Ranking dos cinco tipos com maior quantidade de conclusões. |
| RF05 | Filtros | Período, status, tipo, técnico, usuário e busca por descrição ou usuário. |
| RF06 | Integração com a API | Consultas e gravações por HTTP, com dados persistidos no MongoDB. |
| RF07 | Atualização e tratamento de dados | Atualização manual e automática, carregamento, erro, nova tentativa e consulta vazia. |

Recursos complementares: cadastro, edição, início e conclusão do atendimento, considerações técnicas e histórico.

## Requisitos não funcionais

- Java 21 e Maven Wrapper para execução reproduzível.
- Login por sessão; endpoints autenticados e proteção CSRF nas escritas.
- Validação dos campos e parâmetros, limite de 100 registros por página e intervalo máximo de 366 dias.
- Interface responsiva, rótulos, navegação por teclado e valores dos gráficos disponíveis em tabela.
- Valores de data armazenados em UTC e apresentados em America/Sao_Paulo.
- Separação entre controllers, validações, serviços e acesso a dados.
- Alterações atômicas por documento com versão para impedir sobrescritas concorrentes.
- Dados de demonstração separados do banco padrão.

Não foi realizado teste de carga nem auditoria completa de acessibilidade. O projeto é destinado à atividade e à execução local.

## Modelo de dados

A coleção chamados contém id, descricao, status, tipo, tecnico e usuario, além dos campos de acompanhamento descritos abaixo.

| Campo | Finalidade |
| --- | --- |
| dataAbertura | Instante de criação do chamado. |
| dataConclusao | Instante em que o atendimento é concluído. |
| atualizadoEm | Última atualização. |
| versao | Controle de concorrência. |
| consideracoes | Lista de texto, autor autenticado e data de cada consideração. |
| historico | Lista de status, descrição e data dos eventos. |

Considerações e histórico são documentos embutidos no chamado. A atualização do status ou a inclusão de consideração incrementa a versão no mesmo comando atômico do MongoDB. Há índices em status, dataAbertura e dataConclusao.

As coleções clientes e horariosAtendimento continuam no projeto.

## Regras

1. Novos chamados recebem ABERTO e a data do servidor.
2. O fluxo é ABERTO, EM_ANDAMENTO e CONCLUIDO. Não é permitido pular o atendimento nem reabrir um chamado concluído.
3. A conclusão registra dataConclusao. Uma edição de chamado já concluído não muda essa data.
4. A versão atual é obrigatória nas alterações. Uma versão antiga retorna HTTP 409.
5. Considerações podem ser acrescentadas depois da conclusão e sempre registram o usuário autenticado.
6. Total, abertos, em andamento, distribuição e lista usam a data de abertura.
7. Atendidos, série, ranking e tempo médio usam a data de conclusão. Portanto, atendidos pode ser maior que o total aberto no período.
8. O tempo médio usa somente chamados concluídos com abertura e conclusão conhecidas e duração não negativa.
9. Sem atendimentos, o tempo médio retorna null. Dias sem conclusões aparecem com zero.
10. As semanas começam na segunda-feira. O primeiro rótulo semanal pode anteceder o filtro, mas os valores respeitam o intervalo.
11. Registros antigos sem datas não entram nos indicadores por período. A listagem semData=true permite consultá-los.
12. A migração normaliza as grafias antigas de status e o campo Tipo. O status anterior é preservado em statusAnteriorMigracao. Nenhuma data histórica é criada.
13. Os filtros textuais são aplicados por igualdade. A busca por descrição e usuário escapa os caracteres de expressão regular.
14. A atualização automática é suspensa com aba oculta, diálogo aberto ou consulta em andamento.

## Decisões de implementação

A arquitetura do Thindesk utiliza Java, Spring Boot, Thymeleaf e MongoDB. Os serviços do dashboard calculam os indicadores no banco, sem valores fixos na interface.

O dashboard usa HTML, CSS e JavaScript com gráficos SVG. A autenticação usa sessão, as escritas exigem token CSRF e a API retorna erros padronizados.

O POM usa Spring Boot 3.4.13, gerenciamento central de dependências e Java 21.

## Referências

- Spring Boot: https://docs.spring.io/spring-boot/3.4/
- Spring Data MongoDB: https://docs.spring.io/spring-data/mongodb/reference/
- MongoDB: https://www.mongodb.com/docs/manual/

---

# API do Thindesk

Base: http://127.0.0.1:8080/api. As respostas são JSON. Os endpoints exigem sessão autenticada. Escritas exigem Content-Type: application/json e o token CSRF obtido na página, no cabeçalho X-CSRF-TOKEN. Datas são ISO 8601 em UTC.

## Filtros

| Parâmetro | Valores |
| --- | --- |
| inicio, fim | AAAA-MM-DD, inclusivos em São Paulo. Padrão: últimos 30 dias. Máximo: 366 dias. |
| status | ABERTO, EM_ANDAMENTO, CONCLUIDO. |
| tipo, tecnico, usuario | Texto exato; opções obtidas em /dashboard/filtros. |
| busca | Até 100 caracteres; pesquisa na descrição e no usuário. |
| agrupamento | dia, semana ou mes; padrão dia. |
| pagina | Apenas /chamados; inteiro positivo; padrão 1. |
| limite | Apenas /chamados; 1 a 100; padrão 8. |
| semData | Apenas /chamados; true consulta somente registros sem abertura. Padrão false. |

Parâmetros desconhecidos ou repetidos retornam 400. semData=true substitui o critério de data de abertura na lista. Os demais filtros continuam aplicados.

## Consultas

| Método e endpoint | Retorno |
| --- | --- |
| GET /dashboard | indicadores, serie, situacao, ranking, atualizadoEm, inicio e fim. |
| GET /dashboard/indicadores | total, abertos, emAndamento, atendidos, tempoMedioMinutos e semData. |
| GET /dashboard/serie | Lista de data e quantidade de conclusões, preenchida com zeros. |
| GET /dashboard/status | Lista de status e quantidade, pela abertura. |
| GET /dashboard/tipos | Até cinco objetos com tipo e quantidade de conclusões. |
| GET /dashboard/filtros | tipos, tecnicos, usuarios e status disponíveis. |
| GET /chamados | chamados, pagina, limite, total e paginas. |
| GET /chamados/{id} | Chamado com datas, versão, histórico e considerações. |

Exemplo de requisição:

```http
GET /api/dashboard?inicio=2026-11-01&fim=2026-11-15&tipo=Rede
```

Formato de resposta ilustrativo:

```json
{
  "indicadores":{"total":3,"abertos":1,"emAndamento":1,"atendidos":2,"tempoMedioMinutos":60.0,"semData":0},
  "serie":[{"data":"2026-11-01","quantidade":2}],
  "situacao":[{"status":"ABERTO","quantidade":1},{"status":"EM_ANDAMENTO","quantidade":1},{"status":"CONCLUIDO","quantidade":1}],
  "ranking":[{"tipo":"Rede","quantidade":2}],
  "atualizadoEm":"2026-11-15T15:00:00Z",
  "inicio":"2026-11-01",
  "fim":"2026-11-15"
}
```

A série real inclui todos os dias do intervalo. Os endpoints individuais retornam diretamente o objeto ou a lista indicada na tabela, sem envelope adicional.

```http
GET /api/chamados?inicio=2026-11-01&fim=2026-11-15&status=ABERTO&pagina=1&limite=8
GET /api/chamados?semData=true
GET /api/chamados/675b1fa1d64af05ea59ee7d3
```

Exemplo de listagem vazia:

```json
{"chamados":[],"pagina":1,"limite":8,"total":0,"paginas":0}
```

## POST /chamados

Campos obrigatórios: descricao (1 a 1.000 caracteres), tipo (1 a 80), tecnico (1 a 100) e usuario (1 a 100). Espaços isolados não são aceitos.

```json
{"descricao":"Sem acesso à rede do setor","tipo":"Rede","tecnico":"Ana Lima","usuario":"Mariana Alves"}
```

Retorna 201, cabeçalho Location e o chamado completo. O servidor define status ABERTO, dataAbertura, atualizadoEm, versao 0 e histórico inicial. Não aceita datas de abertura fornecidas pelo cliente.

## PUT /chamados/{id}

Atualiza os dados do chamado. Todos os campos abaixo são obrigatórios:

```json
{"descricao":"Falha na conexão do setor","status":"ABERTO","tipo":"Rede","tecnico":"Ana Lima","usuario":"Mariana Alves","versao":0}
```

Retorna 200 com o chamado completo e versão incrementada. A alteração de status segue o mesmo fluxo permitido pelo PATCH.

## PATCH /chamados/{id}/status

```json
{"status":"EM_ANDAMENTO","versao":0}
```

Retorna 200 com o chamado atualizado, histórico e nova versão. A próxima alteração deve usar a versão retornada. CONCLUIDO preenche dataConclusao.

## POST /chamados/{id}/consideracoes

```json
{"texto":"Cabo substituído e conexão validada com o usuário.","versao":1}
```

Texto obrigatório de 1 a 2.000 caracteres. Retorna 201 com o chamado completo. O autor é o usuário da sessão, não um campo enviado pelo cliente.

Formato dos campos acrescentados ao chamado:

```json
{
  "dataAbertura":"2026-11-15T12:00:00Z",
  "dataConclusao":null,
  "atualizadoEm":"2026-11-15T13:00:00Z",
  "versao":2,
  "consideracoes":[{"texto":"Cabo substituído.","autor":"admin","data":"2026-11-15T13:00:00Z"}],
  "historico":[{"status":"ABERTO","descricao":"Chamado registrado.","data":"2026-11-15T12:00:00Z"}]
}
```

## Erros

```json
{"codigo":"VERSAO_DESATUALIZADA","mensagem":"O chamado foi alterado. Atualize os detalhes antes de salvar."}
```

| HTTP | Código / situação |
| --- | --- |
| 400 | DADOS_INVALIDOS: campos, JSON ou filtros inválidos. |
| 401 | NAO_AUTENTICADO: consulta sem sessão. |
| 403 | ACESSO_NEGADO: token CSRF ou sessão inválidos na escrita. |
| 404 | NAO_ENCONTRADO: chamado inexistente. |
| 405 | METODO_INVALIDO: método não permitido. |
| 409 | TRANSICAO_INVALIDA ou VERSAO_DESATUALIZADA. |
| 503 | BANCO_INDISPONIVEL: falha no acesso ao MongoDB. |

Consultas filtradas podem retornar 400; detalhes podem retornar 404; gravações podem retornar 400, 403 e 409, além de 404 quando há identificador. Todas as consultas dependem do banco e da autenticação.

GET /chamados retorna uma lista paginada de documentos da coleção chamados.

## Formulários de horários

As rotas de horários são MVC: GET /ajustes-horarios abre o formulário e a lista; POST /ajustes-horarios/salvar recebe setor, diaSemana, horarioInicio e horarioFim; POST /ajustes-horarios/{id}/excluir remove um horário. Os formulários usam sessão e CSRF e redirecionam para a lista depois de salvar ou excluir. Setor é obrigatório, com até 80 caracteres. O dia deve ser selecionado entre os dias da semana. Os horários usam HH:mm e o fim deve ser posterior ao início. Erros de validação são apresentados na própria página, sem gravar dados.

---

# Testes do Thindesk

## Ambiente e resultados

Execução em 15/11/2026, com Java 21.0.13, Spring Boot 3.4.13 e MongoDB 8.0.28.

| Suíte | Testes | Falhas | Erros | Ignorados |
| --- | --- | --- | --- | --- |
| DashboardIntegrationTest | 20 | 0 | 0 | 0 |
| FiltroChamadoTest | 4 | 0 | 0 | 0 |
| Total | 24 | 0 | 0 | 0 |

Relatórios de execução:

- [Testes de integração](evidencias/com.pfc.thindesk.DashboardIntegrationTest.txt)
- [Testes unitários](evidencias/com.pfc.thindesk.FiltroChamadoTest.txt)

## Cobertura

| Área | Verificações |
| --- | --- |
| Autenticação | Acesso à API sem sessão retorna 401. |
| CSRF | Escritas sem token válido retornam 403. |
| Indicadores | Contagem por abertura, atendimentos por conclusão e cálculo do tempo médio. |
| Série temporal | Conclusões no intervalo e preenchimento dos dias sem atendimentos. |
| Ranking | Quantidade por tipo, considerando apenas chamados concluídos. |
| Filtros | Combinação de status, tipo e técnico; rejeição de parâmetros inválidos ou repetidos. |
| Registros antigos | Consulta de chamados sem data, preservando a ausência do valor histórico. |
| Cadastro | Persistência dos campos, datas e histórico; rejeição de dados inválidos. |
| Andamento | Transições permitidas e conflito de versões. |
| Considerações | Persistência do texto e identificação do autor pela sessão. |
| Recursos inexistentes | Consulta de chamado inexistente retorna 404. |
| Navegação | Renderização de Dashboard, Chamados, Clientes e Horários. |
| Horários | Cadastro, listagem, validação do intervalo e exclusão por POST protegido. |

Os testes de integração utilizam bancos temporários com prefixo thindesk_test_ e identificador aleatório. Os dados são reiniciados entre cenários e o banco temporário é removido ao final. A suíte não modifica o banco de demonstração.

## Verificações da interface

Foram verificados no navegador: login, carregamento dos indicadores, cadastro de chamado, início do atendimento, registro de consideração, conclusão, atualização da tabela e consulta após reinício da aplicação.

Também foram verificados a navegação para Clientes e Horários e o cadastro, a listagem e a exclusão de um horário de teste.

![Dashboard](evidencias/dashboard.jpg)

![Horários de atendimento](evidencias/horarios.jpg)

## Reprodução

Com o MongoDB disponível, configure TEST_MONGO_URI sem o nome do banco e execute:

```powershell
$env:TEST_MONGO_URI = "mongodb://127.0.0.1:27028"
.\mvnw.cmd test
```

Sem essa variável, os 20 testes de integração ficam ignorados e apenas os quatro testes unitários são executados.

## Limites da verificação

Os resultados correspondem à execução local. Não incluem teste de carga, auditoria completa de acessibilidade ou verificação em múltiplos navegadores.
