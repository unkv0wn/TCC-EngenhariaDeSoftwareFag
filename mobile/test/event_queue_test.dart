import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:routewise_driver/models/route_models.dart';
import 'package:routewise_driver/services/event_queue.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// A fila é por rota, e estes testes existem por causa de um defeito concreto: com uma
/// fila só, a entrega que ficou pendente na rota de ontem subia para a rota aberta hoje.
/// Como o motorista abre a segunda rota do dia logo depois de fechar a primeira, é o
/// caminho normal do app — não um caso de borda.
void main() {
  const rotaA = 'HWHSJ5GWA8JE';
  const rotaB = '68FBYXRYYHX1';

  PendingEvent entrega(String id, int stop) => PendingEvent(
        clientEventId: id,
        type: RouteEventType.entrega,
        stopIndex: stop,
        occurredAt: DateTime.utc(2026, 10, 14, 15),
      );

  setUp(() => SharedPreferences.setMockInitialValues({}));

  group('fila por rota', () {
    test('o que ficou pendente numa rota não aparece na outra', () async {
      final queue = EventQueue();
      await queue.add(rotaA, entrega('a1', 9));

      expect(await queue.load(rotaA), hasLength(1));
      expect(await queue.load(rotaB), isEmpty);
    });

    test('guarda as duas filas ao mesmo tempo', () async {
      final queue = EventQueue();
      await queue.add(rotaA, entrega('a1', 9));
      await queue.add(rotaB, entrega('b1', 2));

      expect((await queue.load(rotaA)).single.clientEventId, 'a1');
      expect((await queue.load(rotaB)).single.clientEventId, 'b1');
      expect(await queue.pendingCodes(), containsAll([rotaA, rotaB]));
    });

    test('confirmar numa rota não mexe na fila da outra', () async {
      final queue = EventQueue();
      await queue.add(rotaA, entrega('a1', 9));
      await queue.add(rotaB, entrega('b1', 2));

      await queue.removeSettled(rotaA, {'a1'});

      expect(await queue.load(rotaA), isEmpty);
      expect(await queue.load(rotaB), hasLength(1));
    });

    test('rota sem pendência sai da lista de códigos', () async {
      final queue = EventQueue();
      await queue.add(rotaA, entrega('a1', 9));
      expect(await queue.pendingCodes(), [rotaA]);

      await queue.removeSettled(rotaA, {'a1'});

      expect(await queue.pendingCodes(), isEmpty);
    });

    test('recusado sai da fila igual ao aceito', () async {
      // Um evento recusado não melhora com o tempo: se ficasse, o app o reenviaria em
      // toda sincronização e o selo de pendentes mentiria sobre haver algo a caminho.
      final queue = EventQueue();
      await queue.add(rotaA, entrega('a1', 9));
      await queue.add(rotaA, entrega('a2', 3));

      final remaining = await queue.removeSettled(rotaA, {'a1', 'a2'});

      expect(remaining, isEmpty);
    });

    test('sair da rota preserva a fila dela', () async {
      final queue = EventQueue();
      await queue.saveCode(rotaA);
      await queue.add(rotaA, entrega('a1', 9));

      await queue.clearCode();

      expect(await queue.readCode(), isNull);
      expect(await queue.load(rotaA), hasLength(1), reason: 'o registro feito em campo não pode se perder');
    });
  });

  group('migração da fila única', () {
    /// Simula a versão anterior do app, que gravava tudo numa chave só.
    Future<void> seedLegacy(List<PendingEvent> events) {
      return SharedPreferences.getInstance().then((prefs) {
        prefs.setString(
          'routewise.pending_events',
          jsonEncode(events.map((event) => event.toJson()).toList()),
        );
      });
    }

    test('a fila antiga vira fila da rota que estava aberta', () async {
      await seedLegacy([entrega('velho', 9)]);
      final queue = EventQueue();

      await queue.migrateLegacyQueue(rotaA);

      expect((await queue.load(rotaA)).single.clientEventId, 'velho');
      expect(await queue.pendingCodes(), [rotaA]);
    });

    test('sem dono conhecido, a fila antiga é descartada', () async {
      // Subir evento sem saber a que rota pertence é exatamente o defeito que a separação
      // por código existe para impedir.
      await seedLegacy([entrega('orfao', 9)]);
      final queue = EventQueue();

      await queue.migrateLegacyQueue(null);

      expect(await queue.load(rotaA), isEmpty);
      expect(await queue.pendingCodes(), isEmpty);
    });

    test('roda uma vez: a segunda chamada não duplica nada', () async {
      await seedLegacy([entrega('velho', 9)]);
      final queue = EventQueue();

      await queue.migrateLegacyQueue(rotaA);
      await queue.migrateLegacyQueue(rotaA);

      expect(await queue.load(rotaA), hasLength(1));
    });

    test('preserva o que já havia na fila da rota', () async {
      final queue = EventQueue();
      await queue.add(rotaA, entrega('novo', 2));
      await seedLegacy([entrega('velho', 9)]);

      await queue.migrateLegacyQueue(rotaA);

      expect(
        (await queue.load(rotaA)).map((event) => event.clientEventId),
        containsAll(['novo', 'velho']),
      );
    });
  });
}
