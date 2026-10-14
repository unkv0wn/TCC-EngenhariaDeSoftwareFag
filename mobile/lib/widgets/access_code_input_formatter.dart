import 'package:flutter/services.dart';

import '../models/access_code.dart';

/// Máscara do campo de código: maiúsculas, grupos de quatro, nada fora do alfabeto.
///
/// Serve mais para guiar do que para restringir. Enquanto digita, o motorista vê
/// `XS4W-JZ6P-334C` tomando forma e percebe sozinho quando faltam caracteres ou quando
/// digitou um a mais — sem precisar submeter e tomar um erro de volta.
///
/// As trocas de leitura (O→0, I/L→1) acontecem aqui também, para o que aparece na tela
/// ser exatamente o que será enviado. Ainda assim o servidor normaliza de novo: esta
/// camada é conveniência, não validação.
class AccessCodeInputFormatter extends TextInputFormatter {
  static const _alphabet = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final buffer = StringBuffer();

    for (final rune in newValue.text.toUpperCase().runes) {
      if (buffer.length >= AccessCode.length) break;

      var char = String.fromCharCode(rune);
      if (char == 'O') char = '0';
      if (char == 'I' || char == 'L') char = '1';
      if (_alphabet.contains(char)) buffer.write(char);
    }

    final formatted = AccessCode.format(buffer.toString());

    return TextEditingValue(
      text: formatted,
      // O cursor vai sempre para o fim: a edição real é no final do código, e tentar
      // preservar a posição original com hífens entrando e saindo faria o cursor pular.
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}
