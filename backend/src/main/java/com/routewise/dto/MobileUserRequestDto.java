package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Criação/edição de um acesso do app.
 *
 * <p>{@code password} é obrigatória na criação e opcional na edição — em branco quer
 * dizer "mantém a atual", senão toda correção de rótulo de aparelho obrigaria o gestor a
 * inventar uma senha nova e avisar o motorista.
 */
public record MobileUserRequestDto(

  @NotNull(message = "Selecione o motorista.")
  UUID driverId,

  @NotBlank(message = "Informe o usuário.")
  @Size(max = 60, message = "Máximo de 60 caracteres.")
  @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use apenas letras, números, ponto, hífen ou sublinhado.")
  String username,

  /**
   * Sem mínimo aqui de propósito: em branco é um valor válido na edição ("mantém a
   * atual"), e {@code @Size(min = 6)} recusaria justamente esse caso. O mínimo é cobrado
   * no serviço, que é quem sabe se o pedido é criação ou edição.
   */
  @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres.")
  String password,

  /**
   * ANDROID_ID do aparelho, como o app mostra na tela de login. Hexadecimal de 16
   * dígitos nos aparelhos reais; o limite maior cobre o identificador de reserva que o
   * app gera quando o sistema não devolve o ANDROID_ID.
   */
  @Size(max = 64, message = "Identificador de aparelho inválido.")
  @Pattern(regexp = "^[a-zA-Z0-9-]*$", message = "O identificador do aparelho só tem letras, números e hífen.")
  String deviceId,

  @Size(max = 80, message = "Máximo de 80 caracteres.")
  String deviceLabel,

  @NotBlank(message = "Informe o status.")
  @Pattern(regexp = "^(ativo|bloqueado)$", message = "Status inválido.")
  String status

) {}
