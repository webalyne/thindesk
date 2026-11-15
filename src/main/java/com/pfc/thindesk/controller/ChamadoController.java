package com.pfc.thindesk.controller;

import java.net.URI;
import java.security.Principal;
import java.time.Clock;
import java.util.Map;
import jakarta.validation.Valid;
import com.pfc.thindesk.api.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {
    private final ChamadoService service;
    private final Clock clock;
    public ChamadoController(ChamadoService service,Clock clock) { this.service=service;this.clock=clock; }

    @GetMapping
    public Map<String,Object> listar(@RequestParam MultiValueMap<String,String> params) {
        return service.consultar(FiltroChamado.parse(params,clock,true));
    }
    @GetMapping("/{id}")
    public Chamado buscar(@PathVariable String id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<Chamado> criar(@Valid @RequestBody ChamadoRequests.Novo body) {
        Chamado result=service.criar(body);
        return ResponseEntity.created(URI.create("/api/chamados/"+result.getId())).body(result);
    }
    @PutMapping("/{id}")
    public Chamado editar(@PathVariable String id,@Valid @RequestBody ChamadoRequests.Edicao body) {
        return service.editar(id,body);
    }
    @PatchMapping("/{id}/status")
    public Chamado status(@PathVariable String id,@Valid @RequestBody ChamadoRequests.Status body) {
        return service.alterarStatus(id,body);
    }
    @PostMapping("/{id}/consideracoes")
    public ResponseEntity<Chamado> considerar(@PathVariable String id,@Valid @RequestBody ChamadoRequests.Consideracao body,Principal principal) {
        return ResponseEntity.status(201).body(service.considerar(id,body,principal.getName()));
    }
}
