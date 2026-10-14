import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/route_models.dart';

/// Fila local de eventos ainda não confirmados pelo servidor.
///
/// Entrega acontece em galpão, subsolo e estrada. Um registro feito sem rede tem que
/// sobreviver até ela voltar — inclusive se o app for fechado no meio. Por isso a fila
/// é gravada em disco a cada mudança, e não só mantida em memória.
///
/// O evento sai daqui apenas quando o servidor confirma pelo `clientEventId`. Enquanto
/// isso ele aparece na tela marcado como pendente: o motorista precisa saber que o
/// registro existe mas ainda não subiu.
class EventQueue {
  static const _queueKey = 'routewise.pending_events';
  static const _codeKey = 'routewise.route_code';

  Future<List<PendingEvent>> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_queueKey);
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
      await prefs.remove(_queueKey);
      return [];
    }
  }

  Future<void> add(PendingEvent event) async {
    final events = await load()..add(event);
    await _save(events);
  }

  /// Remove os que o servidor confirmou, preservando a ordem dos demais.
  Future<List<PendingEvent>> removeConfirmed(Set<String> confirmedIds) async {
    final remaining = (await load())
        .where((event) => !confirmedIds.contains(event.clientEventId))
        .toList();
    await _save(remaining);
    return remaining;
  }

  Future<void> _save(List<PendingEvent> events) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      _queueKey,
      jsonEncode(events.map((event) => event.toJson()).toList()),
    );
  }

  Future<String?> readCode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_codeKey);
  }

  Future<void> saveCode(String code) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_codeKey, code);
  }

  Future<void> clearCode() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_codeKey);
  }
}
