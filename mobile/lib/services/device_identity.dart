import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

/// Identificador deste aparelho, que o gestor cadastra para liberar o acesso.
///
/// É o `Settings.Secure.ANDROID_ID`, lido pelo canal nativo em `MainActivity.kt` — **não
/// é o IMEI**, que o Android não entrega a app de terceiro desde a versão 10.
///
/// Quando o sistema não devolve o ANDROID_ID (acontece em ROM modificada, e no desktop
/// durante os testes), cai para um UUID gerado uma vez e gravado. O de reserva é mais
/// fraco: limpar os dados do app o apaga, e o aparelho precisaria ser cadastrado de novo.
/// Mesmo assim é melhor do que um app que não consegue nem se apresentar.
class DeviceIdentity {
  DeviceIdentity({MethodChannel? channel}) : _channel = channel ?? const MethodChannel('routewise/device');

  static const _fallbackKey = 'routewise.device_id';

  final MethodChannel _channel;

  String? _cached;

  /// O identificador, sempre o mesmo dentro da instalação.
  Future<String> current() async {
    final cached = _cached;
    if (cached != null) return cached;

    final resolved = await _androidId() ?? await _fallbackId();
    _cached = resolved;
    return resolved;
  }

  Future<String?> _androidId() async {
    try {
      final id = await _channel.invokeMethod<String>('androidId');
      if (id == null || id.trim().isEmpty) return null;
      return id.trim();
    } on MissingPluginException {
      // Plataforma sem o canal — é o caso dos testes e de qualquer alvo que não Android.
      return null;
    } on PlatformException {
      return null;
    }
  }

  Future<String> _fallbackId() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(_fallbackKey);
    if (stored != null && stored.isNotEmpty) return stored;

    final generated = const Uuid().v4();
    await prefs.setString(_fallbackKey, generated);
    return generated;
  }

  /// O identificador em grupos de quatro, para o motorista conseguir ler em voz alta ou
  /// conferir o que digitou. Um hexadecimal de 16 dígitos corrido é um convite ao erro.
  static String format(String id) {
    final chunks = <String>[];
    for (var i = 0; i < id.length; i += 4) {
      chunks.add(id.substring(i, i + 4 > id.length ? id.length : i + 4));
    }
    return chunks.join(' ');
  }
}
