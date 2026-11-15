package com.pfc.thindesk;

import com.pfc.thindesk.api.*;
import com.pfc.thindesk.service.ChamadoService;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.junit.jupiter.api.Assertions.*;

class FiltroChamadoTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-15T01:00:00Z"),ZoneOffset.UTC);
    @Test void periodoPadraoRespeitaSaoPaulo() {
        var f=FiltroChamado.parse(new LinkedMultiValueMap<>(),clock,false);
        assertEquals(LocalDate.parse("2026-11-14"),f.fim());
        assertEquals(29,java.time.temporal.ChronoUnit.DAYS.between(f.inicio(),f.fim()));
    }
    @Test void naoPermitePularAtendimento() {
        assertThrows(ApiException.class,()->ChamadoService.validarTransicao("ABERTO","CONCLUIDO"));
    }
    @Test void naoPermiteReabrirConcluido() {
        assertThrows(ApiException.class,()->ChamadoService.validarTransicao("CONCLUIDO","ABERTO"));
    }
    @Test void intervaloInvalido() {
        var p=new LinkedMultiValueMap<String,String>();p.add("inicio","2026-11-15");p.add("fim","2026-11-14");
        assertThrows(ApiException.class,()->FiltroChamado.parse(p,clock,false));
    }
}
