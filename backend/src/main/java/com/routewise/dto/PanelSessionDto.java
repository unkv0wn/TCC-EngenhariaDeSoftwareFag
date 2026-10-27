package com.routewise.dto;

import java.util.UUID;

/** Quem está logado, para a interface mostrar e para o Next confirmar a sessão. */
public record PanelSessionDto(UUID id, String name, String email) {}
