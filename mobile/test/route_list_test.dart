import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:routewise_driver/models/route_models.dart';
import 'package:routewise_driver/services/api_client.dart';

/// Testa o contrato da tela inicial com `GET /api/mobile/routes`.
///
/// Mesmo risco dos outros testes de contrato daqui: um campo renomeado no
/// `MobileRouteSummaryDto` continua compilando do lado Dart e só aparece como tela
/// inicial vazia ou com números errados — e um motorista que não vê a rota do dia
/// conclui que não tem rota, não que o app quebrou.
void main() {
  Map<String, dynamic> summaryJson({
    String accessCode = 'ABCDEFGHIJKL',
    String status = 'planejada',
    int totalStops = 4,
    int deliveredStops = 1,
  }) {
    return {
      'accessCode': accessCode,
      'createdAt': '2026-10-22T07:00:00.000Z',
      'status': status,
      'departureTime': '07:30',
      'ordersCount': 6,
      'totalStops': totalStops,
      'deliveredStops': deliveredStops,
      'totalDistanceKm': 42.5,
      'totalDurationMin': 68.0,
      'vehiclePlate': 'ABC-1234',
      'vehicleModel': 'Sprinter',
    };
  }

  group('lista de rotas', () {
    test('lê o resumo nos nomes que o backend manda', () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/mobile/routes');
        return http.Response(
          jsonEncode([summaryJson()]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final routes = await ApiClient(baseUrl: 'http://localhost:8080', client: client).fetchMyRoutes();

      expect(routes, hasLength(1));
      final route = routes.single;
      expect(route.accessCode, 'ABCDEFGHIJKL');
      expect(route.status, 'planejada');
      expect(route.departureTime, '07:30');
      expect(route.ordersCount, 6);
      expect(route.totalStops, 4);
      expect(route.deliveredStops, 1);
      expect(route.totalDistanceKm, 42.5);
      expect(route.vehiclePlate, 'ABC-1234');
      expect(route.vehicleModel, 'Sprinter');
    });

    test('manda o token — sem ele o servidor não sabe de quem é a lista', () async {
      String? authorization;
      final client = MockClient((request) async {
        authorization = request.headers['Authorization'];
        return http.Response('[]', 200, headers: {'content-type': 'application/json; charset=utf-8'});
      });

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client)..token = 'jwt-aqui';
      await api.fetchMyRoutes();

      expect(authorization, 'Bearer jwt-aqui');
    });

    test('lista vazia é caso normal, não erro', () async {
      final client = MockClient((request) async =>
          http.Response('[]', 200, headers: {'content-type': 'application/json; charset=utf-8'}));

      final routes = await ApiClient(baseUrl: 'http://localhost:8080', client: client).fetchMyRoutes();

      expect(routes, isEmpty);
    });

    test('veículo ausente não quebra a leitura', () async {
      final json = summaryJson()
        ..remove('vehiclePlate')
        ..remove('vehicleModel');
      final client = MockClient((request) async => http.Response(
            jsonEncode([json]),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          ));

      final route = (await ApiClient(baseUrl: 'http://localhost:8080', client: client).fetchMyRoutes()).single;

      expect(route.vehiclePlate, isNull);
      expect(route.vehicleModel, isNull);
    });

    /// Regressão: quatro rotas anteriores à migração V16 não tinham código de acesso. O
    /// `as String` sobre o nulo lançava `TypeError`, que **não é `Exception`** e escapava
    /// do `catch` da tela — o `setState` nunca rodava e a tela inicial girava para
    /// sempre, sem erro e sem saída. O servidor passou a não listar rota sem código, mas
    /// o app não pode voltar a congelar por causa de uma resposta inesperada, venha ela
    /// de onde vier.
    test('campo inesperado vira ApiException, não TypeError que trava a tela', () async {
      final semCodigo = summaryJson()..remove('accessCode');
      final client = MockClient((request) async => http.Response(
            jsonEncode([semCodigo]),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          ));

      await expectLater(
        ApiClient(baseUrl: 'http://localhost:8080', client: client).fetchMyRoutes(),
        throwsA(isA<ApiException>()),
      );
    });

    test('corpo que não é JSON também vira ApiException', () async {
      final client = MockClient((request) async =>
          http.Response('isto nao e json', 200, headers: {'content-type': 'text/plain'}));

      await expectLater(
        ApiClient(baseUrl: 'http://localhost:8080', client: client).fetchMyRoutes(),
        throwsA(isA<ApiException>()),
      );
    });

    test('sessão expirada vem como 401, para a tela voltar ao login', () async {
      final client = MockClient((request) async => http.Response(
            jsonEncode({'detail': 'Sua sessão expirou. Entre novamente para continuar.'}),
            401,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          ));

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client)..token = 'velho';

      await expectLater(
        api.fetchMyRoutes(),
        throwsA(isA<ApiException>().having((ex) => ex.isUnauthenticated, 'isUnauthenticated', true)),
      );
    });
  });

  group('RouteSummary', () {
    test('separa em aberto de encerrada — é o que divide as seções da tela', () {
      final planned = RouteSummary.fromJson(summaryJson(status: 'planejada'));
      final running = RouteSummary.fromJson(summaryJson(status: 'em_rota'));
      final done = RouteSummary.fromJson(summaryJson(status: 'concluida'));

      expect(planned.isOpen, isTrue);
      expect(running.isOpen, isTrue);
      expect(done.isOpen, isFalse);
      expect(done.isFinished, isTrue);
    });

    test('progresso é a fração entregue', () {
      final route = RouteSummary.fromJson(summaryJson(totalStops: 4, deliveredStops: 1));
      expect(route.progress, 0.25);
    });

    test('rota sem parada não estoura na divisão', () {
      final route = RouteSummary.fromJson(summaryJson(totalStops: 0, deliveredStops: 0));
      expect(route.progress, 0);
    });
  });
}
