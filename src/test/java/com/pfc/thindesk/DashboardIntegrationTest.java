package com.pfc.thindesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.test.context.support.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="tecnico.teste")
@EnabledIfEnvironmentVariable(named="TEST_MONGO_URI",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardIntegrationTest {
    private static final String DATABASE="thindesk_test_"+UUID.randomUUID().toString().replace("-","");
    private static final String PERIOD="?inicio=2026-11-14&fim=2026-11-14";
    @Autowired MockMvc mvc;
    @Autowired ChamadoRepository repository;
    @Autowired MongoTemplate mongo;
    @Autowired ObjectMapper json;
    String openId;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.data.mongodb.uri",()->System.getenv("TEST_MONGO_URI")+"/"+DATABASE);
        properties.add("spring.data.mongodb.database",()->DATABASE);
    }
    @BeforeEach
    void prepare() {
        assertTrue(mongo.getDb().getName().startsWith("thindesk_test_"));
        repository.deleteAll();
        mongo.getCollection("horariosAtendimento").deleteMany(new org.bson.Document());
        openId=insert("ABERTO","Hardware","2026-11-14T12:00:00Z",null).getId();
        insert("CONCLUIDO","Rede","2026-11-14T10:00:00Z","2026-11-14T11:00:00Z");
        insert("CONCLUIDO","Rede","2026-11-13T23:00:00Z","2026-11-14T13:00:00Z");
        insert("EM_ANDAMENTO","Software","2026-11-14T09:00:00Z",null);
        insert("ABERTO","Rede",null,null);
    }
    @AfterAll
    void cleanup() {
        assertEquals(DATABASE,mongo.getDb().getName());
        mongo.getDb().drop();
    }
    private Chamado insert(String status,String tipo,String opening,String closing) {
        Chamado c=new Chamado();
        c.setDescricao("Problema no equipamento");c.setStatus(status);c.setTipo(tipo);
        c.setTecnico("Ana");c.setUsuario("Pedro");
        if(opening!=null)c.setDataAbertura(Instant.parse(opening));
        if(closing!=null)c.setDataConclusao(Instant.parse(closing));
        return repository.insert(c);
    }
    @Test @WithAnonymousUser
    void consultaExigeLogin() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
    @Test
    void escritaExigeCsrf() throws Exception {
        mvc.perform(post("/api/chamados").contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test
    void indicadoresSeparamAberturaDeConclusao() throws Exception {
        mvc.perform(get("/api/dashboard"+PERIOD)).andExpect(status().isOk())
            .andExpect(jsonPath("$.indicadores.total").value(3))
            .andExpect(jsonPath("$.indicadores.abertos").value(1))
            .andExpect(jsonPath("$.indicadores.emAndamento").value(1))
            .andExpect(jsonPath("$.indicadores.atendidos").value(2))
            .andExpect(jsonPath("$.indicadores.tempoMedioMinutos").value(450.0))
            .andExpect(jsonPath("$.indicadores.semData").value(1));
    }
    @Test
    void serieUsaDataDeConclusao() throws Exception {
        mvc.perform(get("/api/dashboard/serie"+PERIOD)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].quantidade").value(2));
    }
    @Test
    void rankingUsaSomenteConcluidos() throws Exception {
        mvc.perform(get("/api/dashboard/tipos"+PERIOD)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tipo").value("Rede"))
            .andExpect(jsonPath("$[0].quantidade").value(2));
    }
    @Test
    void filtrosCombinados() throws Exception {
        mvc.perform(get("/api/chamados"+PERIOD+"&status=ABERTO&tipo=Hardware&tecnico=Ana"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }
    @Test
    void legadoContinuaConsultavelSemDataInventada() throws Exception {
        mvc.perform(get("/api/chamados"+PERIOD+"&semData=true")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.chamados[0].dataAbertura").isEmpty());
    }
    @Test
    void seriePreencheDiasVazios() throws Exception {
        mvc.perform(get("/api/dashboard/serie?inicio=2026-10-01&fim=2026-10-03"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].quantidade").value(0));
    }
    @Test
    void parametrosInvalidosSaoRejeitados() throws Exception {
        for(String query:List.of("?inicio=2026-02-30","?inicio=2026-12-01&fim=2026-11-01","?status=ERRADO","?limite=101","?pagina=-1","?status=ABERTO&status=CONCLUIDO","?campo=1"))
            mvc.perform(get("/api/chamados"+query)).andExpect(status().isBadRequest());
    }
    @Test
    void cadastroPersisteDatasEHistorico() throws Exception {
        mvc.perform(post("/api/chamados").with(csrf()).contentType("application/json")
            .content("{\"descricao\":\"Sem rede\",\"tipo\":\"Rede\",\"tecnico\":\"Ana\",\"usuario\":\"Pedro\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTO"))
            .andExpect(jsonPath("$.dataAbertura").isNotEmpty()).andExpect(jsonPath("$.historico.length()").value(1));
        assertEquals(6,repository.count());
    }
    @Test
    void cadastroInvalidoNaoGrava() throws Exception {
        mvc.perform(post("/api/chamados").with(csrf()).contentType("application/json").content("{\"descricao\":\"\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(5,repository.count());
    }
    @Test
    void fluxoEControleDeVersao() throws Exception {
        String path="/api/chamados/"+openId+"/status";
        mvc.perform(patch(path).with(csrf()).contentType("application/json").content("{\"status\":\"EM_ANDAMENTO\",\"versao\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
        mvc.perform(patch(path).with(csrf()).contentType("application/json").content("{\"status\":\"CONCLUIDO\",\"versao\":0}"))
            .andExpect(status().isConflict());
        mvc.perform(patch(path).with(csrf()).contentType("application/json").content("{\"status\":\"CONCLUIDO\",\"versao\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dataConclusao").isNotEmpty());
        mvc.perform(patch(path).with(csrf()).contentType("application/json").content("{\"status\":\"ABERTO\",\"versao\":2}"))
            .andExpect(status().isConflict());
    }
    @Test
    void consideracaoRegistraAutorAutenticado() throws Exception {
        mvc.perform(post("/api/chamados/"+openId+"/consideracoes").with(csrf()).contentType("application/json")
            .content("{\"texto\":\"Cabo substituído.\",\"versao\":0}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.consideracoes[0].autor").value("tecnico.teste"))
            .andExpect(jsonPath("$.status").value("ABERTO"));
        assertEquals("Cabo substituído.",repository.findById(openId).orElseThrow().getConsideracoes().getFirst().texto());
    }
    @Test
    void inexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/chamados/naoexiste")).andExpect(status().isNotFound());
    }
    @Test
    void paginaDoPainelRenderizaEIncluiCsrf() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Visão da operação")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void todasAsPaginasDoMenuRenderizam() throws Exception {
        for(String path:List.of("/dashboard","/chamados","/clientes","/ajustes-horarios"))
            mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Navegação principal")));
    }
    @Test
    void horarioPodeSerCadastradoEListado() throws Exception {
        mvc.perform(post("/ajustes-horarios/salvar").with(csrf())
            .param("setor","Financeiro").param("diaSemana","Segunda-feira")
            .param("horarioInicio","08:00").param("horarioFim","17:00"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/ajustes-horarios?salvo"));
        assertEquals(1,mongo.getCollection("horariosAtendimento").countDocuments());
        mvc.perform(get("/ajustes-horarios")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Financeiro")));
    }
    @Test
    void horarioComFimAnteriorAoInicioNaoEGravado() throws Exception {
        mvc.perform(post("/ajustes-horarios/salvar").with(csrf())
            .param("setor","Financeiro").param("diaSemana","Segunda-feira")
            .param("horarioInicio","17:00").param("horarioFim","08:00"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("novoHorario","horarioFim"));
        assertEquals(0,mongo.getCollection("horariosAtendimento").countDocuments());
    }
    @Test
    void cadastroDeHorarioExigeCsrf() throws Exception {
        mvc.perform(post("/ajustes-horarios/salvar")
            .param("setor","Financeiro").param("diaSemana","Segunda-feira")
            .param("horarioInicio","08:00").param("horarioFim","17:00"))
            .andExpect(status().isForbidden());
        assertEquals(0,mongo.getCollection("horariosAtendimento").countDocuments());
    }
    @Test
    void horarioSoEExcluidoPorPostProtegido() throws Exception {
        var horario=mongo.insert(new com.pfc.thindesk.entity.HorarioAtendimento(null,"Financeiro","Segunda-feira","08:00","17:00"));
        String path="/ajustes-horarios/"+horario.getId()+"/excluir";
        mvc.perform(get(path)).andExpect(status().isMethodNotAllowed());
        assertEquals(1,mongo.getCollection("horariosAtendimento").countDocuments());
        mvc.perform(post(path).with(csrf())).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/ajustes-horarios?excluido"));
        assertEquals(0,mongo.getCollection("horariosAtendimento").countDocuments());
    }

}
