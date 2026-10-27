package com.routewise.dto;

import java.util.UUID;

/**
 * Token do painel mais o mínimo sobre quem entrou.
 *
 * <p>O nome vem junto porque o cookie é {@code httpOnly}: a página não consegue abrir o
 * token para descobrir quem está logado, e sem isto precisaria de uma chamada a mais só
 * para escrever um nome no canto da tela.
 */
public record LoginResponseDto(String token, UUID userId, String name, String email) {}
