import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/route_models.dart';
import 'session_service.dart';

/// Erro já traduzido pro que a tela mostra.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  /// Token inválido ou rota apagada — a tela volta pra abertura de rota.
  bool get isRouteGone => statusCode == 404;

  /// Sessão expirada ou revogada: o app volta pro login, e insistir na mesma chamada
  /// não resolve — ao contrário do erro de rede, que melhora quando o sinal volta.
  bool get isUnauthenticated => statusCode == 401;

  /// Credencial certa, aparelho não liberado. Só o gestor desfaz.
  bool get isDeviceRefused => statusCode == 403;

  @override
  String toString() => message;
}

/// O que o servidor respondeu sobre um lote da fila.
///
/// Separa o que entrou do que foi recusado porque os dois saem da fila, por motivos
/// opostos: o aceito está gravado, o recusado nunca vai ser aceito. O que fica na fila é
/// só aquilo sobre o que o servidor não se pronunciou.
class SyncOutcome {
  const SyncOutcome({required this.accepted, required this.rejected});

  const SyncOutcome.empty() : accepted = const {}, rejected = const {};

  final Set<String> accepted;

  /// `clientEventId` -> motivo da recusa, para poder dizer ao motorista o que caiu.
  final Map<String, String> rejected;

  Set<String> get settled => {...accepted, ...rejected.keys};
}

/// Cliente da superfície `/api/mobile/**`.
///
/// Quatro chamadas, todas pelo código da rota. O app não conhece id de rota.
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

  /// Token da sessão do motorista. Fora do login, toda chamada a `/api/mobile/**` precisa
  /// dele: o servidor passou a exigi-lo (`MobileAuthFilter`), porque o código da rota
  /// circula por WhatsApp e QR impresso e não prova quem está registrando.
  String? token;

  static const _timeout = Duration(seconds: 15);

  /// Entra com usuário, senha e o identificador deste aparelho.
  ///
  /// O `deviceId` é conferido contra o que o gestor cadastrou — é o que impede a senha,
  /// se vazar, de servir em outro celular.
  Future<DriverSession> login({
    required String username,
    required String password,
    required String deviceId,
  }) async {
    final body = jsonEncode({'username': username, 'password': password, 'deviceId': deviceId});
    final response = await _post('/api/mobile/auth/login', body);
    final json = jsonDecode(response) as Map<String, dynamic>;

    return DriverSession(
      token: json['token'] as String,
      driverId: json['driverId'] as String,
      driverName: json['driverName'] as String? ?? '',
      username: json['username'] as String? ?? username,
    );
  }

  Future<MobileRoute> fetchRoute(String code) async {
    final response = await _get('/api/mobile/routes/$code');
    return MobileRoute.fromJson(jsonDecode(response) as Map<String, dynamic>);
  }

  /// Encerra a rota. Idempotente no servidor: repetir depois de uma resposta perdida
  /// devolve a rota já encerrada em vez de erro.
  Future<MobileRoute> finishRoute(String code) async {
    final response = await _post('/api/mobile/routes/$code/finish', '');
    return MobileRoute.fromJson(jsonDecode(response) as Map<String, dynamic>);
  }

  /// Envia a fila inteira de uma vez. O servidor responde item a item: um evento
  /// recusado não derruba os outros, então o retorno diz quais podem sair da fila.
  Future<SyncOutcome> syncEvents(String code, List<PendingEvent> events) async {
    if (events.isEmpty) return const SyncOutcome.empty();

    final body = jsonEncode(events.map((event) => event.toJson()).toList());
    final response = await _post('/api/mobile/routes/$code/events/batch', body);
    final results = (jsonDecode(response) as List<dynamic>).cast<Map<String, dynamic>>();

    final accepted = <String>{};
    final rejected = <String, String>{};
    for (final result in results) {
      final id = result['clientEventId'] as String;
      if (result['accepted'] == true) {
        accepted.add(id);
      } else {
        rejected[id] = result['error'] as String? ?? 'Registro recusado pelo servidor.';
      }
    }
    return SyncOutcome(accepted: accepted, rejected: rejected);
  }

  /// Cabeçalhos comuns. O `Authorization` só entra quando há sessão: o próprio login
  /// passa por aqui e ainda não tem token nenhum para mandar.
  Map<String, String> _headers({bool json = false}) {
    final headers = {'Accept': 'application/json'};
    if (json) headers['Content-Type'] = 'application/json';
    final current = token;
    if (current != null && current.isNotEmpty) headers['Authorization'] = 'Bearer $current';
    return headers;
  }

  Future<String> _get(String path) async {
    try {
      final response = await _client
          .get(Uri.parse('$baseUrl$path'), headers: _headers())
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
            headers: _headers(json: true),
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
