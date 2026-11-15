package com.pfc.thindesk.api;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

public record FiltroChamado(LocalDate inicio, LocalDate fim, String status, String tipo,
        String tecnico, String usuario, String busca, String agrupamento,
        int pagina, int limite, boolean semData) {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static FiltroChamado parse(MultiValueMap<String,String> params, Clock clock, boolean listagem) {
        Set<String> allowed = new HashSet<>(List.of("inicio","fim","status","tipo","tecnico","usuario","busca","agrupamento"));
        if (listagem) allowed.addAll(List.of("pagina","limite","semData"));
        for (String key : params.keySet()) {
            if (!allowed.contains(key) || params.get(key).size() != 1) invalid("Parâmetro inválido ou repetido: " + key);
        }
        LocalDate hoje = LocalDate.now(clock.withZone(ZONE));
        LocalDate inicio, fim;
        try {
            inicio = LocalDate.parse(value(params,"inicio",hoje.minusDays(29).toString()));
            fim = LocalDate.parse(value(params,"fim",hoje.toString()));
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"DADOS_INVALIDOS","Informe datas válidas no formato AAAA-MM-DD.");
        }
        if (fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio,fim) > 365) invalid("Selecione um período de 1 a 366 dias.");
        String status = value(params,"status","");
        if (!status.isEmpty() && !List.of("ABERTO","EM_ANDAMENTO","CONCLUIDO").contains(status)) invalid("Status inválido.");
        String grouping = value(params,"agrupamento","dia");
        if (!List.of("dia","semana","mes").contains(grouping)) invalid("Agrupamento inválido.");
        String missing = value(params,"semData","false");
        if (!List.of("true","false").contains(missing)) invalid("semData deve ser true ou false.");
        String busca = value(params,"busca","");
        if (busca.length()>100) invalid("Busca limitada a 100 caracteres.");
        String tipo=value(params,"tipo",""), tecnico=value(params,"tecnico",""), usuario=value(params,"usuario","");
        if (tipo.length()>80 || tecnico.length()>100 || usuario.length()>100) invalid("Filtro excede o tamanho permitido.");
        return new FiltroChamado(inicio,fim,status,tipo,tecnico,usuario,busca,grouping,
            number(params,"pagina",1,1000000),number(params,"limite",8,100),Boolean.parseBoolean(missing));
    }
    private static String value(MultiValueMap<String,String> p,String key,String fallback) {
        String value=p.getFirst(key);
        return value==null || value.isBlank() ? fallback : value.trim();
    }
    private static int number(MultiValueMap<String,String> p,String key,int fallback,int max) {
        try {
            String text=value(p,key,String.valueOf(fallback));
            if (!text.matches("[0-9]+")) throw new NumberFormatException();
            int n=Integer.parseInt(text);
            if(n<1 || n>max) throw new NumberFormatException();
            return n;
        } catch(NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"DADOS_INVALIDOS",key+" deve estar entre 1 e "+max+".");
        }
    }
    public static void invalid(String message) { throw new ApiException(HttpStatus.BAD_REQUEST,"DADOS_INVALIDOS",message); }

    public Criteria criteria(String dateField, boolean onlyCompleted) {
        List<Criteria> parts=new ArrayList<>();
        if(!status.isBlank()) parts.add(Criteria.where("status").is(status));
        if(onlyCompleted) parts.add(Criteria.where("status").is("CONCLUIDO"));
        if(!tipo.isBlank()) parts.add(Criteria.where("tipo").is(tipo));
        if(!tecnico.isBlank()) parts.add(Criteria.where("tecnico").is(tecnico));
        if(!usuario.isBlank()) parts.add(Criteria.where("usuario").is(usuario));
        if(!busca.isBlank()) parts.add(new Criteria().orOperator(
            Criteria.where("descricao").regex(Pattern.quote(busca),"i"),
            Criteria.where("usuario").regex(Pattern.quote(busca),"i")));
        if(dateField!=null) {
            if(semData) parts.add(Criteria.where(dateField).is(null));
            else parts.add(Criteria.where(dateField).gte(inicio.atStartOfDay(ZONE).toInstant())
                .lt(fim.plusDays(1).atStartOfDay(ZONE).toInstant()));
        }
        return parts.isEmpty() ? new Criteria() : new Criteria().andOperator(parts.toArray(Criteria[]::new));
    }
}
