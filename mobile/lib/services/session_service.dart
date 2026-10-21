import 'package:shared_preferences/shared_preferences.dart';

/// Sessão do motorista, como o app a guarda entre aberturas.
class DriverSession {
  const DriverSession({
    required this.token,
    required this.driverId,
    required this.driverName,
    required this.username,
  });

  final String token;
  final String driverId;
  final String driverName;
  final String username;
}

/// Guarda a sessão em disco para o motorista não logar a cada abertura do app.
///
/// Fica em `shared_preferences`, junto da fila offline e do código da rota, e não em
/// armazenamento criptografado: o que está aqui é um token com validade, não a senha —
/// ela nunca é gravada. Em troca, sair do app apaga a sessão inteira de uma vez.
class SessionService {
  static const _tokenKey = 'routewise.session.token';
  static const _driverIdKey = 'routewise.session.driver_id';
  static const _driverNameKey = 'routewise.session.driver_name';
  static const _usernameKey = 'routewise.session.username';

  DriverSession? _cached;

  /// A sessão guardada, ou nulo se não há — e aí a tela de login é a primeira.
  Future<DriverSession?> load() async {
    final cached = _cached;
    if (cached != null) return cached;

    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(_tokenKey);
    if (token == null || token.isEmpty) return null;

    final session = DriverSession(
      token: token,
      driverId: prefs.getString(_driverIdKey) ?? '',
      driverName: prefs.getString(_driverNameKey) ?? '',
      username: prefs.getString(_usernameKey) ?? '',
    );
    _cached = session;
    return session;
  }

  Future<void> save(DriverSession session) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, session.token);
    await prefs.setString(_driverIdKey, session.driverId);
    await prefs.setString(_driverNameKey, session.driverName);
    await prefs.setString(_usernameKey, session.username);
    _cached = session;
  }

  /// Encerra a sessão. **Não** mexe na fila de eventos nem no código da rota: um evento
  /// registrado offline ainda não subiu, e apagá-lo junto com a sessão perderia trabalho
  /// de campo que ninguém mais tem como recuperar.
  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_driverIdKey);
    await prefs.remove(_driverNameKey);
    await prefs.remove(_usernameKey);
    _cached = null;
  }
}
