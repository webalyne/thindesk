package com.pfc.thindesk.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import com.pfc.thindesk.api.FiltroChamado;
import com.pfc.thindesk.entity.Chamado;

@Service
public class DashboardService {
    private final MongoTemplate mongo;
    private final Clock clock;
    public DashboardService(MongoTemplate mongo,Clock clock) { this.mongo=mongo;this.clock=clock; }

    private List<Document> aggregate(Criteria criteria,Document... stages) {
        List<AggregationOperation> operations=new ArrayList<>();
        operations.add(Aggregation.match(criteria));
        for(Document stage:stages) operations.add(context -> stage);
        return mongo.aggregate(Aggregation.newAggregation(operations),"chamados",Document.class).getMappedResults();
    }
    public Map<String,Object> indicadores(FiltroChamado f) {
        long total=mongo.count(new Query(f.criteria("dataAbertura",false)),Chamado.class);
        List<Map<String,Object>> statuses=situacao(f);
        long abertos=statuses.stream().filter(s->s.get("status").equals("ABERTO")).mapToLong(s->((Number)s.get("quantidade")).longValue()).sum();
        long andamento=statuses.stream().filter(s->s.get("status").equals("EM_ANDAMENTO")).mapToLong(s->((Number)s.get("quantidade")).longValue()).sum();
        long atendidos=mongo.count(new Query(f.criteria("dataConclusao",true)),Chamado.class);
        Document duration=new Document("$cond",Arrays.asList(
            new Document("$and",List.of(new Document("$ne",Arrays.asList("$dataAbertura",null)),
                new Document("$gte",List.of("$dataConclusao","$dataAbertura")))),
            new Document("$divide",List.of(new Document("$subtract",List.of("$dataConclusao","$dataAbertura")),60000)),null));
        List<Document> average=aggregate(f.criteria("dataConclusao",true),
            new Document("$group",new Document("_id",null).append("media",new Document("$avg",duration))));
        Double tempo=average.isEmpty() || average.getFirst().get("media")==null ? null : Math.round(((Number)average.getFirst().get("media")).doubleValue()*10)/10.0;
        Criteria missing=new Criteria().andOperator(f.criteria(null,false),Criteria.where("dataAbertura").is(null));
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("total",total);out.put("abertos",abertos);out.put("emAndamento",andamento);
        out.put("atendidos",atendidos);out.put("tempoMedioMinutos",tempo);
        out.put("semData",mongo.count(new Query(missing),Chamado.class));
        return out;
    }
    public List<Map<String,Object>> situacao(FiltroChamado f) {
        List<Document> rows=aggregate(f.criteria("dataAbertura",false),
            new Document("$group",new Document("_id","$status").append("quantidade",new Document("$sum",1))));
        List<Map<String,Object>> result=new ArrayList<>();
        for(String status:List.of("ABERTO","EM_ANDAMENTO","CONCLUIDO")) {
            long value=rows.stream().filter(row->status.equals(row.getString("_id"))).mapToLong(row->((Number)row.get("quantidade")).longValue()).sum();
            result.add(Map.of("status",status,"quantidade",value));
        }
        return result;
    }
    public List<Document> ranking(FiltroChamado f) {
        List<Document> rows=aggregate(f.criteria("dataConclusao",true),
            new Document("$group",new Document("_id",new Document("$ifNull",List.of("$tipo","Não informado"))).append("quantidade",new Document("$sum",1))),
            new Document("$sort",new Document("quantidade",-1).append("_id",1)),
            new Document("$limit",5),
            new Document("$project",new Document("_id",0).append("tipo","$_id").append("quantidade",1)));
        return rows;
    }
    public List<Map<String,Object>> serie(FiltroChamado f) {
        Document date=new Document("$dateToString",new Document("format","%Y-%m-%d")
            .append("date","$dataConclusao").append("timezone","America/Sao_Paulo"));
        List<Document> rows=aggregate(f.criteria("dataConclusao",true),
            new Document("$group",new Document("_id",date).append("quantidade",new Document("$sum",1))));
        Map<String,Long> daily=new HashMap<>();
        rows.forEach(row->daily.put(row.getString("_id"),((Number)row.get("quantidade")).longValue()));
        Map<LocalDate,Long> groups=new TreeMap<>();
        for(LocalDate day=f.inicio();!day.isAfter(f.fim());day=day.plusDays(1)) {
            LocalDate key=switch(f.agrupamento()) {
                case "semana" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case "mes" -> day.withDayOfMonth(1);
                default -> day;
            };
            groups.merge(key,daily.getOrDefault(day.toString(),0L),Long::sum);
        }
        List<Map<String,Object>> result=new ArrayList<>();
        groups.forEach((dateKey,quantidade)->result.add(Map.of("data",dateKey.toString(),"quantidade",quantidade)));
        return result;
    }
    public Map<String,Object> filtros() {
        Query nonBlank=new Query();
        List<String> tipos=mongo.findDistinct(nonBlank,"tipo",Chamado.class,String.class).stream().filter(s->s!=null&&!s.isBlank()).sorted().toList();
        List<String> tecnicos=mongo.findDistinct(nonBlank,"tecnico",Chamado.class,String.class).stream().filter(s->s!=null&&!s.isBlank()).sorted().toList();
        List<String> usuarios=mongo.findDistinct(nonBlank,"usuario",Chamado.class,String.class).stream().filter(s->s!=null&&!s.isBlank()).sorted().toList();
        return Map.of("tipos",tipos,"tecnicos",tecnicos,"usuarios",usuarios,
            "status",List.of("ABERTO","EM_ANDAMENTO","CONCLUIDO"));
    }
    public Map<String,Object> consultar(FiltroChamado f) {
        return Map.of("indicadores",indicadores(f),"serie",serie(f),"situacao",situacao(f),
            "ranking",ranking(f),"atualizadoEm",clock.instant(),"inicio",f.inicio(),"fim",f.fim());
    }
}
