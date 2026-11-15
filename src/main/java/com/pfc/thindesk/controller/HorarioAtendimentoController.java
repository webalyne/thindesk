package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ajustes-horarios")
public class HorarioAtendimentoController {
    private final HorarioAtendimentoService service;
    public HorarioAtendimentoController(HorarioAtendimentoService service) { this.service=service; }

    @Data
    public static class HorarioForm {
        @NotBlank(message="Informe o setor.") @Size(max=80)
        private String setor;
        @NotBlank @Pattern(regexp="Segunda-feira|Terça-feira|Quarta-feira|Quinta-feira|Sexta-feira|Sábado|Domingo",message="Selecione um dia da semana.")
        private String diaSemana;
        @NotBlank @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]",message="Informe um horário válido.")
        private String horarioInicio;
        @NotBlank @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]",message="Informe um horário válido.")
        private String horarioFim;
    }

    @GetMapping
    public String listar(Model model,HttpServletRequest request) {
        model.addAttribute("novoHorario",new HorarioForm());
        return pagina(model,request);
    }
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("novoHorario") HorarioForm form,BindingResult errors,
                         Model model,HttpServletRequest request) {
        if(!errors.hasErrors() && !java.time.LocalTime.parse(form.getHorarioInicio()).isBefore(java.time.LocalTime.parse(form.getHorarioFim())))
            errors.rejectValue("horarioFim","intervalo","O fim deve ser posterior ao início, no mesmo dia.");
        if(errors.hasErrors()) return pagina(model,request);
        service.salvar(new HorarioAtendimento(null,form.getSetor().trim(),form.getDiaSemana(),form.getHorarioInicio(),form.getHorarioFim()));
        return "redirect:/ajustes-horarios?salvo";
    }
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable String id) {
        service.deletar(id);
        return "redirect:/ajustes-horarios?excluido";
    }
    private String pagina(Model model,HttpServletRequest request) {
        model.addAttribute("horarios",service.listarTodos());
        model.addAttribute("nomeUsuario",request.getRemoteUser());
        model.addAttribute("pagina","horarios");
        return "ajustes-horarios";
    }
}
