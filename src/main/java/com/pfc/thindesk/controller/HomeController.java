package com.pfc.thindesk.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.pfc.thindesk.service.ClienteService;
import com.pfc.thindesk.service.HorarioAtendimentoService;

@Controller
public class HomeController {
    private final ClienteService clientes;
    private final HorarioAtendimentoService horarios;
    public HomeController(ClienteService clientes,HorarioAtendimentoService horarios) { this.clientes=clientes;this.horarios=horarios; }
    @GetMapping("/login")
    public String login() { return "login"; }
    @GetMapping("/")
    public String inicio() { return "redirect:/dashboard"; }
    @GetMapping({"/dashboard","/chamados"})
    public String dashboard(HttpServletRequest request,Model model) {
        model.addAttribute("nomeUsuario",request.getRemoteUser());
        model.addAttribute("pagina",request.getRequestURI().equals("/chamados")?"chamados":"dashboard");
        return "dashboard";
    }
    @GetMapping("/clientes")
    public String clientes(Model model,HttpServletRequest request) { model.addAttribute("pagina","clientes"); model.addAttribute("nomeUsuario",request.getRemoteUser()); model.addAttribute("clientes",clientes.listarClientes());return "clientes"; }
}
