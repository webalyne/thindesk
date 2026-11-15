package com.pfc.thindesk.api;

import jakarta.validation.constraints.*;

public final class ChamadoRequests {
    private ChamadoRequests() {}

    public record Novo(
        @NotBlank @Size(max=1000) String descricao,
        @NotBlank @Size(max=80) String tipo,
        @NotBlank @Size(max=100) String tecnico,
        @NotBlank @Size(max=100) String usuario) {}

    public record Edicao(
        @NotBlank @Size(max=1000) String descricao,
        @NotBlank @Pattern(regexp="ABERTO|EM_ANDAMENTO|CONCLUIDO") String status,
        @NotBlank @Size(max=80) String tipo,
        @NotBlank @Size(max=100) String tecnico,
        @NotBlank @Size(max=100) String usuario,
        @NotNull @PositiveOrZero Long versao) {}

    public record Status(
        @NotBlank @Pattern(regexp="ABERTO|EM_ANDAMENTO|CONCLUIDO") String status,
        @NotNull @PositiveOrZero Long versao) {}

    public record Consideracao(
        @NotBlank @Size(max=2000) String texto,
        @NotNull @PositiveOrZero Long versao) {}
}
