import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/route_models.dart';

/// Erro já traduzido pro que a tela mostra.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  /// Token inválido ou rota apagada — a tela volta pra abertura de rota.
  bool get isRouteGone => statusCode == 404;

  @override
  String toString() => message;
}

/// Cliente da superfície `/api/mobile/**`.
///
/// Só três chamadas, todas por token. O app não conhece id de rota.
class ApiClient {
  ApiClient({required this.baseUrl, http.Client? client}) : _client = client ?? http.Client();

  /// Definido em tempo de build: `flutter run --dart-define=API_URL=http://10.0.2.2:8080`.
  /// 10.0.2.2 é como o emulador do Android enxerga o localhost da máquina.
  static const defaultBaseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  final String baseUrl;
  final http.Client _client;

  static const _timeout = Duration(seconds: 15);

  Future<MobileRoute> fetchRoute(String token) async {
    final response = await _get('/api/mobile/routes/$token');
    return MobileRoute.fromJson(jsonDecode(response) as Map<String, dynamic>);
  }

  /// Envia a fila inteira de uma vez. O servidor responde item a item: um evento
  /// recusado não derruba os outros, então o retorno diz quais podem sair da fila.
  Future<Set<String>> syncEvents(String token, List<PendingEvent> events) async {
    if (events.isEmpty) return <String>{};

    final body = jsonEncode(events.map((event) => event.toJson()).toList());
    final response = await _post('/api/mobile/routes/$token/events/batch', body);
    final results = jsonDecode(response) as List<dynamic>;

    return results
        .cast<Map<String, dynamic>>()
        .where((result) => result['accepted'] == true)
        .map((result) => result['clientEventId'] as String)
        .toSet();
  }

  Future<String> _get(String path) async {
    try {
      final response = await _client
          .get(Uri.parse('$baseUrl$path'), headers: {'Accept': 'application/json'})
          .timeout(_timeout);
      return _read(response);
    } on ApiException {
      rethrow;
    } on Exception {
      throw ApiException('Sem conexão com o servidor.');
    }
  }

  Future<String> _post(String path, String body) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl$path'),
            headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
            body: body,
          )
          .timeout(_timeout);
      return _read(response);
    } on ApiException {
      rethrow;
    } on Exception {
      throw ApiException('Sem conexão com o servidor.');
    }
  }

  String _read(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      // O backend devolve UTF-8; `response.body` assume latin-1 quando o charset
      // não vem no header, e aí "Ocorrência" chega quebrado na tela.
      return utf8.decode(response.bodyBytes);
    }
    throw ApiException(_errorMessage(response), statusCode: response.statusCode);
  }

  /// Lê o problem detail (RFC 9457) que o `GlobalExceptionHandler` devolve.
  String _errorMessage(http.Response response) {
    if (response.statusCode == 404) {
      return 'Rota não encontrada. Confira o link ou peça um novo ao gestor.';
    }
    try {
      final body = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
      return (body['detail'] ?? body['title'] ?? 'Erro ${response.statusCode}') as String;
    } on Exception {
      return 'Erro ${response.statusCode}';
    }
  }
}
