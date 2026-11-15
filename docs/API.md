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

O contrato de GET /chamados mudou da lista simples original para paginação. A entidade original permanece na mesma coleção.
