package com.pfc.thindesk.config;

import java.util.List;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ChamadoMigration implements ApplicationRunner {
    private final MongoTemplate mongo;
    public ChamadoMigration(MongoTemplate mongo) { this.mongo=mongo; }
    @Override public void run(ApplicationArguments args) {
        var collection=mongo.getCollection("chamados");
        for(var pair:List.of(new String[]{"aberto","ABERTO"},new String[]{"em andamento","EM_ANDAMENTO"},new String[]{"concluído","CONCLUIDO"},new String[]{"concluido","CONCLUIDO"})) {
            var filter=new Document("status",Pattern.compile("^"+Pattern.quote(pair[0])+"$",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE));
            filter.append("statusAnteriorMigracao",new Document("$exists",false));
            collection.updateMany(filter,List.of(new Document("$set",
                new Document("statusAnteriorMigracao","$status").append("status",pair[1]))));
        }
        collection.updateMany(new Document("Tipo",new Document("$exists",true)),
            List.of(new Document("$set",new Document("tipo",new Document("$ifNull",List.of("$tipo","$Tipo"))))));
    }
}
