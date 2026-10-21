import 'package:flutter/foundation.dart';

import 'api_client.dart';
import 'device_identity.dart';
import 'session_service.dart';

/// Quem está usando o app, e em qual aparelho.
///
/// Mantém o token em um lugar só e o entrega ao [ApiClient] que o resto do app usa — por
/// isso o cliente HTTP é criado aqui e passado ao `RouteController`, em vez de cada um ter
/// o seu: com dois clientes, o login gravaria o token num e as chamadas de rota sairiam
/// pelo outro, sem sessão.
class SessionController extends ChangeNotifier {
  SessionController({ApiClient? api, SessionService? sessions, DeviceIdentity? device})
      : api = api ?? ApiClient(baseUrl: ApiClient.defaultBaseUrl),
        _sessions = sessions ?? SessionService(),
        _device = device ?? DeviceIdentity();

  final ApiClient api;
  final SessionService _sessions;
  final DeviceIdentity _device;

  DriverSession? _session;
  String? _deviceId;
  bool _isLoading = false;
  String? _error;

  DriverSession? get session => _session;
  bool get isSignedIn => _session != null;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Identificador deste aparelho, mostrado na tela de login para o motorista enviar ao
  /// gestor. Nulo só enquanto a leitura não terminou.
  String? get deviceId => _deviceId;

  /// Lê a sessão guardada e o identificador do aparelho. Chamada uma vez, na abertura.
  Future<void> restore() async {
    _deviceId = await _device.current();
    final stored = await _sessions.load();
    if (stored != null) {
      _session = stored;
      api.token = stored.token;
    }
    notifyListeners();
  }

  /// Entra. Devolve `true` quando a sessão foi aberta; no erro, [error] já traz a frase
  /// que a tela mostra — o servidor distingue senha errada de aparelho não liberado, e
  /// as duas levam a ações diferentes do motorista.
  Future<bool> signIn({required String username, required String password}) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final session = await api.login(
        username: username.trim(),
        password: password,
        deviceId: _deviceId ?? await _device.current(),
      );
      await _sessions.save(session);
      _session = session;
      api.token = session.token;
      return true;
    } on ApiException catch (ex) {
      _error = ex.message;
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Sai. A fila de eventos offline não é tocada: ela pertence ao aparelho e à rota, não
  /// à sessão, e um registro de campo que ainda não subiu não pode morrer num logout.
  Future<void> signOut() async {
    await _sessions.clear();
    _session = null;
    api.token = null;
    _error = null;
    notifyListeners();
  }

  void dismissError() {
    _error = null;
    notifyListeners();
  }
}
