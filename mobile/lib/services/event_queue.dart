import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/route_models.dart';

/// Fila local de eventos ainda não confirmados pelo servidor.
///
/// Entrega acontece em galpão, subsolo e estrada. Um registro feito sem rede tem que
/// sobreviver até ela voltar — inclusive se o app for fechado no meio. Por isso a fila
/// é gravada em disco a cada mudança, e não só mantida em memória.
///
/// O evento sai daqui apenas quando o servidor responde sobre ele pelo `clientEventId`.
/// Enquanto isso ele aparece na tela marcado como pendente: o motorista precisa saber
/// que o registro existe mas ainda não subiu.
///
/// **Há uma fila por rota**, e isso não é detalhe de organização. Com uma fila só, um
/// evento que ficou pendente na rota de ontem subia para a rota aberta hoje — a entrega
/// de uma parada aparecia feita em outra rota, com um número de parada que às vezes nem
/// existia nela. Guardar por código faz cada fila só poder subir para a rota em que foi
/// criada, e é o que permite abrir a segunda rota do dia sem perder o que ficou da
/// primeira: a fila antiga continua aqui, esperando a conexão.
class EventQueue {
  static const _queuePrefix = 'routewise.pending_events.';
  static const _pendingCodesKey = 'routewise.pending_codes';
  static const _codeKey = 'routewise.route_code';

  /// Chave da fila única, de antes da separação por rota. Só é lida para migrar.
  static const _legacyQueueKey = 'routewise.pending_events';

  Future<List<PendingEvent>> load(String code) async {
    final prefs = await SharedPreferences.getInstance();
    return _read(prefs, _queuePrefix + code);
  }

  Future<void> add(String code, PendingEvent event) async {
    final events = await load(code)..add(event);
    await _save(code, events);
  }

  /// Remove os eventos sobre os quais o servidor já se pronunciou, preservando a ordem
  /// dos demais.
  ///
  /// Aceito e recusado saem os dois. Um evento recusado — tipo inválido, parada que não
  /// existe, rota encerrada — não melhora com o tempo: mantê-lo na fila faria o app
  /// reenviá-lo em toda sincronização, para sempre, e o selo de pendentes mentiria sobre
  /// haver algo a caminho.
  Future<List<PendingEvent>> removeSettled(String code, Set<String> settledIds) async {
    final remaining = (await load(code))
        .where((event) => !settledIds.contains(event.clientEventId))
        .toList();
    await _save(code, remaining);
    return remaining;
  }

  /// Códigos de rota que ainda têm eventos esperando — inclusive de rotas já encerradas
  /// ou trocadas. É por aqui que a fila da rota anterior encontra o caminho de volta.
  Future<List<String>> pendingCodes() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList(_pendingCodesKey);
    return raw ?? const [];
  }

  /// Adota a fila antiga, de quando havia uma só, atribuindo-a a [ownerCode].
  ///
  /// Roda uma vez, na primeira abertura depois da atualização. O dono certo é a rota que
  /// estava aberta quando aqueles eventos foram criados — a última salva. Sem um dono
  /// conhecido a fila é descartada: subir eventos sem saber a que rota pertencem é
  /// justamente o defeito que a separação por código existe para impedir.
  Future<void> migrateLegacyQueue(String? ownerCode) async {
    final prefs = await SharedPreferences.getInstance();
    final legacy = _read(prefs, _legacyQueueKey);
    if (legacy.isEmpty) {
      await prefs.remove(_legacyQueueKey);
      return;
    }

    if (ownerCode != null) {
      final merged = await load(ownerCode)..addAll(legacy);
      await _save(ownerCode, merged);
    }
    await prefs.remove(_legacyQueueKey);
  }

  List<PendingEvent> _read(SharedPreferences prefs, String key) {
    final raw = prefs.getString(key);
    if (raw == null || raw.isEmpty) return [];

    try {
      final list = jsonDecode(raw) as List<dynamic>;
      return list
          .cast<Map<String, dynamic>>()
          .map(PendingEvent.fromJson)
          .toList();
    } on FormatException {
      // Fila corrompida é melhor descartada do que impedindo o app de abrir. Perde-se
      // o que não subiu; manter trava o motorista numa tela de erro sem saída.
      prefs.remove(key);
      return [];
    }
  }

  Future<void> _save(String code, List<PendingEvent> events) async {
    final prefs = await SharedPreferences.getInstance();
    final key = _queuePrefix + code;

    if (events.isEmpty) {
      await prefs.remove(key);
    } else {
      await prefs.setString(key, jsonEncode(events.map((event) => event.toJson()).toList()));
    }
    await _trackPendingCode(prefs, code, hasEvents: events.isNotEmpty);
  }

  Future<void> _trackPendingCode(
    SharedPreferences prefs,
    String code, {
    required bool hasEvents,
  }) async {
    final codes = (prefs.getStringList(_pendingCodesKey) ?? const <String>[]).toList();
    final listed = codes.contains(code);

    if (hasEvents && !listed) {
      codes.add(code);
    } else if (!hasEvents && listed) {
      codes.remove(code);
    } else {
      return;
    }
    await prefs.setStringList(_pendingCodesKey, codes);
  }

  Future<String?> readCode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_codeKey);
  }

  Future<void> saveCode(String code) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_codeKey, code);
  }

  /// Esquece a rota aberta — **sem** apagar a fila dela. O que ficou pendente continua
  /// guardado sob o código da rota e sobe na próxima vez que houver conexão.
  Future<void> clearCode() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_codeKey);
  }
}
