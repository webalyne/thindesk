package com.pfc.thindesk.controller;

import java.time.Clock;
import java.util.Map;
import com.pfc.thindesk.api.*;
import com.pfc.thindesk.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    private final Clock clock;
    public DashboardController(DashboardService service,Clock clock) { this.service=service;this.clock=clock; }
    @GetMapping
    public Map<String,Object> dashboard(@RequestParam MultiValueMap<String,String> params) {
        return service.consultar(FiltroChamado.parse(params,clock,false));
    }
    @GetMapping("/filtros")
    public Map<String,Object> filtros() { return service.filtros(); }
    @GetMapping("/{recurso:indicadores|serie|status|tipos}")
    public Object recurso(@PathVariable String recurso,@RequestParam MultiValueMap<String,String> params) {
        var f=FiltroChamado.parse(params,clock,false);
        return switch(recurso) {
            case "indicadores" -> service.indicadores(f);
            case "serie" -> service.serie(f);
            case "status" -> service.situacao(f);
            case "tipos" -> service.ranking(f);
            default -> throw new ApiException(HttpStatus.NOT_FOUND,"NAO_ENCONTRADO","Recurso inexistente.");
        };
    }
}
