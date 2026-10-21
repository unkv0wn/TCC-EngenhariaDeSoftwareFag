import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:routewise_driver/services/api_client.dart';
import 'package:routewise_driver/services/device_identity.dart';

/// Testa o contrato do login com `/api/mobile/auth/login` e o envio do token nas
/// chamadas seguintes.
///
/// É o mesmo tipo de risco dos outros testes de contrato daqui: um campo renomeado no
/// `MobileLoginRequestDto` continua compilando do lado Dart e só aparece como login
/// recusado em campo, com o motorista achando que errou a senha. E se o `Authorization`
/// parar de sair, tudo falha de uma vez com 401 — longe de quem consiga diagnosticar.
void main() {
  group('login', () {
    test('manda usuário, senha e identificador do aparelho nos nomes que o backend lê', () async {
      Map<String, dynamic>? sent;
      String? path;

      final client = MockClient((request) async {
        path = request.url.path;
        sent = jsonDecode(request.body) as Map<String, dynamic>;
        return http.Response(
          jsonEncode({
            'token': 'jwt-aqui',
            'driverId': '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001',
            'driverName': 'Carlos Eduardo Santos',
            'username': 'carlos',
          }),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client);
      final session = await api.login(
        username: 'carlos',
        password: 'senha-secreta',
        deviceId: 'a1b2c3d4e5f60718',
      );

      expect(path, '/api/mobile/auth/login');
      expect(sent!['username'], 'carlos');
      expect(sent!['password'], 'senha-secreta');
      expect(sent!['deviceId'], 'a1b2c3d4e5f60718');

      expect(session.token, 'jwt-aqui');
      expect(session.driverId, '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001');
      expect(session.driverName, 'Carlos Eduardo Santos');
      expect(session.username, 'carlos');
    });

    test('senha errada chega como 401 e pede novo login, não contato com o gestor', () async {
      final client = MockClient((request) async => http.Response(
            jsonEncode({'detail': 'Usuário ou senha incorretos.'}),
            401,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          ));

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client);

      await expectLater(
        api.login(username: 'carlos', password: 'errada', deviceId: 'a1b2c3d4e5f60718'),
        throwsA(
          isA<ApiException>()
              .having((ex) => ex.isUnauthenticated, 'isUnauthenticated', true)
              .having((ex) => ex.isDeviceRefused, 'isDeviceRefused', false)
              .having((ex) => ex.message, 'message', 'Usuário ou senha incorretos.'),
        ),
      );
    });

    test('aparelho não cadastrado chega como 403, que é problema só o gestor resolve', () async {
      final client = MockClient((request) async => http.Response(
            jsonEncode({'detail': 'Este aparelho ainda não foi cadastrado.'}),
            403,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          ));

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client);

      await expectLater(
        api.login(username: 'carlos', password: 'senha-secreta', deviceId: 'outro-aparelho'),
        throwsA(
          isA<ApiException>()
              .having((ex) => ex.isDeviceRefused, 'isDeviceRefused', true)
              .having((ex) => ex.isUnauthenticated, 'isUnauthenticated', false),
        ),
      );
    });
  });

  group('token da sessão', () {
    test('acompanha as chamadas de rota depois do login', () async {
      final authorizations = <String?>[];

      final client = MockClient((request) async {
        authorizations.add(request.headers['Authorization']);
        return http.Response(
          jsonEncode({
            'accessCode': 'ABCD-EFGH-IJKL',
            'createdAt': '2026-10-20T07:00:00.000Z',
            'status': 'em_rota',
            'departureTime': '07:30',
            'ordersCount': 0,
            'totalValue': 0,
            'totalDistanceKm': 0,
            'totalDurationMin': 0,
            'driverName': 'Carlos Eduardo Santos',
            'stops': <dynamic>[],
            'events': <dynamic>[],
          }),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client);

      // Antes do login não há o que mandar — e é o próprio login que passa por aqui.
      await api.fetchRoute('ABCDEFGHIJKL');
      expect(authorizations.last, isNull);

      api.token = 'jwt-aqui';
      await api.fetchRoute('ABCDEFGHIJKL');
      expect(authorizations.last, 'Bearer jwt-aqui');
    });

    test('sessão expirada no meio da rota vem como 401, não como falha de rede', () async {
      final client = MockClient((request) async => http.Response(
            jsonEncode({'detail': 'Sua sessão expirou. Entre novamente para continuar.'}),
            401,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          ));

      final api = ApiClient(baseUrl: 'http://localhost:8080', client: client)..token = 'jwt-velho';

      await expectLater(
        api.fetchRoute('ABCDEFGHIJKL'),
        throwsA(isA<ApiException>().having((ex) => ex.isUnauthenticated, 'isUnauthenticated', true)),
      );
    });
  });

  group('identificador do aparelho', () {
    test('é mostrado em grupos de quatro, para ler em voz alta sem errar', () {
      expect(DeviceIdentity.format('a1b2c3d4e5f60718'), 'a1b2 c3d4 e5f6 0718');
    });

    test('não quebra quando o comprimento não é múltiplo de quatro', () {
      expect(DeviceIdentity.format('a1b2c3'), 'a1b2 c3');
      expect(DeviceIdentity.format(''), '');
    });
  });
}
