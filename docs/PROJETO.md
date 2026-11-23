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
