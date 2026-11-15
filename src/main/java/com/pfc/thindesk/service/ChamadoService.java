package com.pfc.thindesk.service;

import java.time.Clock;
import java.util.*;
import com.pfc.thindesk.api.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Chamado.EventoChamado;
import com.pfc.thindesk.entity.Chamado.ConsideracaoTecnica;
import com.pfc.thindesk.repository.ChamadoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {
    private final ChamadoRepository repository;
    private final MongoTemplate mongo;
    private final Clock clock;

    public ChamadoService(ChamadoRepository repository, MongoTemplate mongo, Clock clock) {
        this.repository=repository; this.mongo=mongo; this.clock=clock;
    }
    public List<Chamado> listarChamados() { return repository.findAll(); }

    public Chamado buscar(String id) {
        if(id==null || !id.matches("[a-zA-Z0-9_-]{1,100}")) FiltroChamado.invalid("Identificador inválido.");
        return repository.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND,"NAO_ENCONTRADO","Chamado não encontrado."));
    }

    public Map<String,Object> consultar(FiltroChamado f) {
        Query query=new Query(f.criteria("dataAbertura",false));
        long total=mongo.count(query,Chamado.class);
        query.with(Sort.by(Sort.Order.desc("dataAbertura"),Sort.Order.desc("id")));
        query.skip((long)(f.pagina()-1)*f.limite()).limit(f.limite());
        return Map.of("chamados",mongo.find(query,Chamado.class),"pagina",f.pagina(),"limite",f.limite(),
            "total",total,"paginas",(long)Math.ceil((double)total/f.limite()));
    }

    public Chamado criar(ChamadoRequests.Novo body) {
        Chamado chamado=new Chamado();
        chamado.setDescricao(body.descricao().trim());
        chamado.setTipo(body.tipo().trim());
        chamado.setTecnico(body.tecnico().trim());
        chamado.setUsuario(body.usuario().trim());
        chamado.setStatus("ABERTO");
        chamado.setDataAbertura(clock.instant());
        chamado.setAtualizadoEm(clock.instant());
        chamado.setHistorico(List.of(new EventoChamado("ABERTO","Chamado registrado.",clock.instant())));
        return repository.insert(chamado);
    }

    public Chamado editar(String id,ChamadoRequests.Edicao body) {
        Chamado current=buscar(id);
        validarTransicao(current.getStatus(),body.status());
        Update update=new Update().set("descricao",body.descricao().trim()).set("tipo",body.tipo().trim())
            .set("tecnico",body.tecnico().trim()).set("usuario",body.usuario().trim()).set("status",body.status());
        if(!Objects.equals(current.getStatus(),body.status()) && body.status().equals("CONCLUIDO"))
            update.set("dataConclusao",clock.instant());
        return atualizar(current,body.versao(),update,new EventoChamado(body.status(),"Dados do chamado atualizados.",clock.instant()));
    }

    public Chamado alterarStatus(String id,ChamadoRequests.Status body) {
        Chamado current=buscar(id);
        validarTransicao(current.getStatus(),body.status());
        if(current.getStatus().equals(body.status())) throw new ApiException(HttpStatus.CONFLICT,"TRANSICAO_INVALIDA","O chamado já está nesse status.");
        Update update=new Update().set("status",body.status());
        if(body.status().equals("CONCLUIDO")) update.set("dataConclusao",clock.instant());
        return atualizar(current,body.versao(),update,
            new EventoChamado(body.status(),"Status alterado de "+current.getStatus()+" para "+body.status()+".",clock.instant()));
    }

    public Chamado considerar(String id,ChamadoRequests.Consideracao body,String autor) {
        Chamado current=buscar(id);
        Update update=new Update().push("consideracoes",new ConsideracaoTecnica(body.texto().trim(),autor,clock.instant()));
        return atualizar(current,body.versao(),update,
            new EventoChamado(current.getStatus(),"Consideração técnica registrada por "+autor+".",clock.instant()));
    }

    private Chamado atualizar(Chamado current,long versao,Update update,EventoChamado evento) {
        if(current.getVersao()!=versao) conflito();
        Criteria version=versao==0
            ? new Criteria().orOperator(Criteria.where("versao").is(0),Criteria.where("versao").exists(false))
            : Criteria.where("versao").is(versao);
        Query query=new Query(new Criteria().andOperator(Criteria.where("id").is(current.getId()),version));
        update.set("atualizadoEm",clock.instant()).inc("versao",1).push("historico",evento);
        Chamado updated=mongo.findAndModify(query,update,FindAndModifyOptions.options().returnNew(true),Chamado.class);
        if(updated==null) conflito();
        return updated;
    }
    private static void conflito() {
        throw new ApiException(HttpStatus.CONFLICT,"VERSAO_DESATUALIZADA","O chamado foi alterado. Atualize os detalhes antes de salvar.");
    }
    public static void validarTransicao(String atual,String destino) {
        if(Objects.equals(atual,destino)) return;
        boolean allowed=("ABERTO".equals(atual) && "EM_ANDAMENTO".equals(destino))
            || ("EM_ANDAMENTO".equals(atual) && "CONCLUIDO".equals(destino));
        if(!allowed) throw new ApiException(HttpStatus.CONFLICT,"TRANSICAO_INVALIDA","Use o fluxo aberto, em andamento e concluído.");
    }
}
