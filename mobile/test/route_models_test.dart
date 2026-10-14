import 'package:flutter_test/flutter_test.dart';
import 'package:routewise_driver/models/route_models.dart';

/// Testa o contrato com `/api/mobile/**`.
///
/// É o que pode quebrar em silêncio: um campo renomeado no backend não causa erro de
/// compilação aqui, só um evento recusado na hora de sincronizar — em campo, longe de
/// qualquer um que consiga diagnosticar.
void main() {
  group('PendingEvent', () {
    test('serializa no formato que o MobileEventRequestDto espera', () {
      final event = PendingEvent(
        clientEventId: '11111111-2222-3333-4444-555555555555',
        type: RouteEventType.entrega,
        stopIndex: 4,
        notes: 'Entregue ao encarregado',
        occurredAt: DateTime.utc(2026, 10, 9, 14, 30),
      );

      final json = event.toJson();

      expect(json['clientEventId'], '11111111-2222-3333-4444-555555555555');
      expect(json['type'], 'entrega');
      expect(json['stopIndex'], 4);
      expect(json['notes'], 'Entregue ao encarregado');
      expect(json['occurredAt'], '2026-10-09T14:30:00.000Z');
    });

    test('sobrevive a ida e volta pela fila', () {
      final original = PendingEvent(
        clientEventId: 'abc',
        type: RouteEventType.ocorrencia,
        occurredAt: DateTime.utc(2026, 10, 9, 8),
        notes: 'Cliente ausente',
      );

      final restored = PendingEvent.fromJson(original.toJson());

      expect(restored.clientEventId, original.clientEventId);
      expect(restored.type, original.type);
      expect(restored.notes, original.notes);
      expect(restored.occurredAt, original.occurredAt);
      expect(restored.stopIndex, isNull);
    });

    test('horario local vira UTC no envio', () {
      final event = PendingEvent(
        clientEventId: 'x',
        type: RouteEventType.saida,
        occurredAt: DateTime(2026, 10, 9, 20, 2),
      );

      expect(event.toJson()['occurredAt'] as String, endsWith('Z'));
    });
  });

  group('RouteEventType', () {
    test('cada tipo usa o valor que o backend valida', () {
      expect(RouteEventType.saida.wireValue, 'saida');
      expect(RouteEventType.chegada.wireValue, 'chegada');
      expect(RouteEventType.entrega.wireValue, 'entrega');
      expect(RouteEventType.ocorrencia.wireValue, 'ocorrencia');
      expect(RouteEventType.abastecimento.wireValue, 'abastecimento');
      expect(RouteEventType.retorno.wireValue, 'retorno');
    });

    test('tipo desconhecido não derruba a tela', () {
      // Um backend mais novo pode mandar um tipo que esta versão do app não conhece.
      // Cair em "ocorrência" mostra o evento na lista; lançar deixaria o motorista
      // sem conseguir abrir a rota.
      expect(RouteEventType.fromWire('decolagem'), RouteEventType.ocorrencia);
    });
  });

  group('MobileRoute', () {
    Map<String, dynamic> routeJson({List<Map<String, dynamic>>? stops}) => {
          'accessCode': 'HWHSJ5GWA8JE',
          'createdAt': '2026-10-09T23:03:00Z',
          'status': 'em_rota',
          'departureTime': '20:02',
          'ordersCount': 18,
          'totalValue': 11206.7,
          'totalDistanceKm': 39.0,
          'totalDurationMin': 58.0,
          'driverName': 'Carlos Eduardo Santos',
          'vehiclePlate': 'ABC-1234',
          'vehicleModel': 'Sprinter',
          'vehicleCapacityKg': 1200.0,
          'stops': stops ??
              [
                {'number': 1, 'label': 'Ricardo Gomes', 'lat': -24.67, 'lng': -53.79, 'ordersCount': 2, 'delivered': true},
                {'number': 2, 'label': 'Bom Preço', 'lat': -24.71, 'lng': -53.73, 'ordersCount': 2, 'delivered': false},
                {'number': 3, 'label': 'Juliana Marques', 'lat': -24.72, 'lng': -53.74, 'ordersCount': 3, 'delivered': false},
              ],
          'events': [
            {
              'type': 'entrega',
              'occurredAt': '2026-10-09T21:15:00Z',
              'stopIndex': 1,
              'notes': null,
              'authorName': 'Carlos Eduardo Santos',
            }
          ],
        };

    test('lê a rota inteira', () {
      final route = MobileRoute.fromJson(routeJson());

      expect(route.driverName, 'Carlos Eduardo Santos');
      expect(route.vehicleCapacityKg, 1200.0);
      expect(route.stops, hasLength(3));
      expect(route.events.single.type, RouteEventType.entrega);
      expect(route.events.single.pending, isFalse);
    });

    test('conta entregues e aponta a próxima pendente', () {
      final route = MobileRoute.fromJson(routeJson());

      expect(route.deliveredCount, 1);
      expect(route.nextStop?.number, 2);
    });

    test('rota toda entregue não tem próxima parada', () {
      final route = MobileRoute.fromJson(routeJson(stops: [
        {'number': 1, 'label': 'A', 'lat': 0, 'lng': 0, 'ordersCount': 1, 'delivered': true},
      ]));

      expect(route.nextStop, isNull);
    });

    test('campos opcionais ausentes não quebram a leitura', () {
      final json = routeJson()
        ..remove('vehiclePlate')
        ..remove('vehicleModel')
        ..remove('vehicleCapacityKg')
        ..remove('events');

      final route = MobileRoute.fromJson(json);

      expect(route.vehiclePlate, isNull);
      expect(route.vehicleCapacityKg, 0);
      expect(route.events, isEmpty);
    });
  });
}
