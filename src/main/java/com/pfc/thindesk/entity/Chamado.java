package com.pfc.thindesk.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chamados")
public class Chamado {
    @Id
    private String id;
    private String descricao;
    @Indexed
    private String status;
    private String tipo;
    private String tecnico;
    private String usuario;
    @Indexed
    private Instant dataAbertura;
    @Indexed
    private Instant dataConclusao;
    private Instant atualizadoEm;
    private long versao;
    private List<ConsideracaoTecnica> consideracoes = new ArrayList<>();
    private List<EventoChamado> historico = new ArrayList<>();

    public record ConsideracaoTecnica(String texto, String autor, Instant data) {}
    public record EventoChamado(String status, String descricao, Instant data) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTecnico() { return tecnico; }
    public void setTecnico(String tecnico) { this.tecnico = tecnico; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Instant getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(Instant dataAbertura) { this.dataAbertura = dataAbertura; }
    public Instant getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(Instant dataConclusao) { this.dataConclusao = dataConclusao; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public long getVersao() { return versao; }
    public void setVersao(long versao) { this.versao = versao; }
    public List<ConsideracaoTecnica> getConsideracoes() { return consideracoes == null ? List.of() : consideracoes; }
    public void setConsideracoes(List<ConsideracaoTecnica> consideracoes) { this.consideracoes = consideracoes; }
    public List<EventoChamado> getHistorico() { return historico == null ? List.of() : historico; }
    public void setHistorico(List<EventoChamado> historico) { this.historico = historico; }
}
