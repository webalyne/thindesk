package com.pfc.thindesk.config;

import java.time.*;
import java.util.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(2)
public class DemoData implements ApplicationRunner {
    private final ChamadoRepository repository;
    private final Clock clock;
    public DemoData(ChamadoRepository repository,Clock clock) { this.repository=repository;this.clock=clock; }
    @Override public void run(ApplicationArguments args) {
        if(repository.count()>0) return;
        var random=new Random(78);
        String[] tipos={"Rede","Hardware","Software","Acesso","Impressora"};
        String[] tecnicos={"Ana Lima","Bruno Costa","Carla Santos"};
        String[] usuarios={"Mariana Alves","Pedro Souza","Julia Ribeiro","Lucas Dias"};
        String[] descriptions={"Sem conexão com a rede do setor","Computador reiniciando durante o uso","Aplicativo apresenta erro ao iniciar","Solicitação de liberação de acesso","Impressora não conclui a fila"};
        List<Chamado> samples=new ArrayList<>();
        for(int i=0;i<150;i++) {
            int type=random.nextInt(tipos.length);
            Chamado c=new Chamado();
            c.setDescricao(descriptions[type]);c.setTipo(tipos[type]);
            c.setTecnico(tecnicos[random.nextInt(3)]);c.setUsuario(usuarios[random.nextInt(4)]);
            var opened=clock.instant().minus(Duration.ofHours(1+random.nextInt(24*30)));
            boolean done=i<110;
            c.setStatus(done?"CONCLUIDO":i<132?"EM_ANDAMENTO":"ABERTO");
            c.setDataAbertura(opened);c.setAtualizadoEm(opened);
            c.setHistorico(new ArrayList<>(List.of(new Chamado.EventoChamado("ABERTO","Chamado registrado na base de demonstração.",opened))));
            if(done) {
                var completed=opened.plus(Duration.ofMinutes(20+random.nextInt(180)));
                if(completed.isAfter(clock.instant())) completed=clock.instant();
                c.setDataConclusao(completed);c.setAtualizadoEm(completed);
                c.getHistorico().add(new Chamado.EventoChamado("CONCLUIDO","Atendimento concluído.",completed));
                c.setConsideracoes(List.of(new Chamado.ConsideracaoTecnica("Verificação realizada e funcionamento confirmado com o usuário.",c.getTecnico(),completed)));
            }
            samples.add(c);
        }
        repository.insert(samples);
    }
}
