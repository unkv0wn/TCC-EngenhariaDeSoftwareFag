import 'package:flutter_test/flutter_test.dart';
import 'package:routewise_driver/models/access_code.dart';

/// O código chega de três lugares — QR, link do WhatsApp e teclado — e o motorista está
/// de pé, na rua, às vezes copiando o que ouviu. Estes testes fixam o que o app aceita.
void main() {
  group('AccessCode.normalize', () {
    test('aceita o código canônico', () {
      expect(AccessCode.normalize('XS4WJZ6P334C'), 'XS4WJZ6P334C');
    });

    test('aceita a forma exibida, com hífens', () {
      expect(AccessCode.normalize('XS4W-JZ6P-334C'), 'XS4WJZ6P334C');
    });

    test('aceita minúsculas e espaços ao redor', () {
      expect(AccessCode.normalize('  xs4w-jz6p-334c  '), 'XS4WJZ6P334C');
    });

    test('desfaz as trocas de leitura O/0 e I/1', () {
      // Quem digitou O queria 0; mandá-lo tentar de novo não ajudaria.
      expect(AccessCode.normalize('OOOO-IIII-LLLL'), '000011111111');
    });

    test('extrai o código do deep link que vem no QR', () {
      expect(AccessCode.normalize('routewise://rota/XS4WJZ6P334C'), 'XS4WJZ6P334C');
    });

    test('extrai mesmo do link colado dentro de uma frase', () {
      const message = 'Abra no app: routewise://rota/XS4WJZ6P334C';
      expect(AccessCode.normalize(message), 'XS4WJZ6P334C');
    });

    test('recusa código incompleto', () {
      expect(AccessCode.normalize('XS4W-JZ6P'), isNull);
    });

    test('recusa código longo demais', () {
      expect(AccessCode.normalize('XS4WJZ6P334CXYZ'), isNull);
    });

    test('recusa texto sem caracteres do alfabeto suficientes', () {
      // U está fora do alfabeto de Crockford.
      expect(AccessCode.normalize('UUUUUUUUUUUU'), isNull);
    });

    test('recusa nulo', () {
      expect(AccessCode.normalize(null), isNull);
    });
  });

  group('AccessCode.format', () {
    test('agrupa de quatro em quatro', () {
      expect(AccessCode.format('XS4WJZ6P334C'), 'XS4W-JZ6P-334C');
    });

    test('formata parcial enquanto se digita', () {
      expect(AccessCode.format('XS4WJ'), 'XS4W-J');
    });

    test('formatar e normalizar são inversos', () {
      const code = 'HWHSJ5GWA8JE';
      expect(AccessCode.normalize(AccessCode.format(code)), code);
    });
  });
}
