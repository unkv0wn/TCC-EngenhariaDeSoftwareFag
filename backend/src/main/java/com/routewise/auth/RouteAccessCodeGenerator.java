package com.routewise.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Gera e normaliza o código de acesso de uma rota — o que vai no QR Code, no link do
 * WhatsApp e no campo que o motorista digita quando a câmera não ajuda.
 *
 * <h2>Por que 12 caracteres, e não 32 bytes</h2>
 * A versão anterior usava 32 bytes aleatórios em base64url (43 caracteres). Ótimo para um
 * link, inútil para o caminho que mais importa quando algo dá errado: alguém ditando o
 * código por telefone para o motorista. 12 caracteres em base32 dão 60 bits — a 1000
 * tentativas por segundo, uma busca exaustiva levaria mais que a idade do universo, então
 * a diferença para 256 bits não é alcançável por ninguém, e um deles cabe numa conversa.
 *
 * <h2>Alfabeto</h2>
 * Base32 de Crockford: sem {@code I}, {@code L}, {@code O} e {@code U}. Os três primeiros
 * saem porque se confundem com {@code 1} e {@code 0} em voz alta e em letra de forma; o
 * {@code U} sai para reduzir a chance de o acaso formar palavra ofensiva num código que
 * aparece na tela do gestor.
 *
 * <p>Na leitura, {@link #normalize} desfaz essas confusões em vez de recusar: quem digitou
 * {@code O} onde era {@code 0} queria o mesmo código, e mandá-lo tentar de novo não ajuda
 * ninguém.
 */
@Component
public class RouteAccessCodeGenerator {

  private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  private static final int CODE_LENGTH = 12;
  private static final int GROUP_SIZE = 4;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }

  /**
   * Converte o que foi digitado na forma canônica armazenada.
   *
   * <p>Aceita minúsculas, separadores (o código é exibido como {@code K7M2-P4XQ-9ZTB}) e as
   * trocas clássicas de leitura. Devolve {@code null} quando o resultado não tem o tamanho
   * esperado — aí é código errado mesmo, não variação de digitação.
   */
  public String normalize(String raw) {
    if (raw == null) {
      return null;
    }

    StringBuilder normalized = new StringBuilder(CODE_LENGTH);
    for (char c : raw.toUpperCase().toCharArray()) {
      char mapped = switch (c) {
        case 'O' -> '0';
        case 'I', 'L' -> '1';
        default -> c;
      };
      if (ALPHABET.indexOf(mapped) >= 0) {
        normalized.append(mapped);
      }
      // Qualquer outro caractere (hífen, espaço, U) é descartado como separador ou ruído.
    }

    return normalized.length() == CODE_LENGTH ? normalized.toString() : null;
  }

  /** Forma de exibição, agrupada de 4 em 4 para leitura e ditado. */
  public String format(String code) {
    if (code == null || code.length() != CODE_LENGTH) {
      return code;
    }
    StringBuilder formatted = new StringBuilder(CODE_LENGTH + 2);
    for (int i = 0; i < CODE_LENGTH; i += GROUP_SIZE) {
      if (i > 0) {
        formatted.append('-');
      }
      formatted.append(code, i, i + GROUP_SIZE);
    }
    return formatted.toString();
  }
}
