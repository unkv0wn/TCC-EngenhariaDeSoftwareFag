/// Código de acesso da rota, do lado do app.
///
/// Espelha `RouteAccessCodeGenerator` no backend: 12 caracteres em base32 de Crockford,
/// exibidos em grupos de quatro. O servidor normaliza de novo ao receber — aqui a
/// normalização existe para a tela poder dizer "faltam caracteres" antes de gastar uma
/// chamada de rede, o que importa quando o sinal é ruim.
library;

abstract final class AccessCode {
  static const _alphabet = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  static const length = 12;
  static const _groupSize = 4;

  /// Prefixo do deep link que vai dentro do QR Code.
  static const deepLinkScheme = 'routewise';
  static const _deepLinkPrefix = '$deepLinkScheme://rota/';

  /// Aceita o que vier: código digitado, com hífen, em minúscula, ou o deep link inteiro
  /// lido do QR. Devolve a forma canônica, ou `null` quando não dá 12 caracteres válidos.
  static String? normalize(String? raw) {
    if (raw == null) return null;

    var input = raw.trim();
    // O QR carrega o deep link; a câmera também pode ler um link copiado de outro lugar.
    final schemeIndex = input.toLowerCase().indexOf(_deepLinkPrefix);
    if (schemeIndex >= 0) {
      input = input.substring(schemeIndex + _deepLinkPrefix.length);
    }

    final buffer = StringBuffer();
    for (final rune in input.toUpperCase().runes) {
      var char = String.fromCharCode(rune);
      // Mesmas trocas que o backend desfaz: quem digitou O queria 0.
      if (char == 'O') char = '0';
      if (char == 'I' || char == 'L') char = '1';
      if (_alphabet.contains(char)) buffer.write(char);
    }

    final normalized = buffer.toString();
    return normalized.length == length ? normalized : null;
  }

  /// `XS4WJZ6P334C` → `XS4W-JZ6P-334C`.
  static String format(String code) {
    final groups = <String>[];
    for (var i = 0; i < code.length; i += _groupSize) {
      groups.add(code.substring(i, (i + _groupSize).clamp(0, code.length)));
    }
    return groups.join('-');
  }
}
