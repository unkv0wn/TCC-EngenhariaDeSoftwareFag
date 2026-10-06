import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';

import '../models/route_models.dart';
import 'api_client.dart';
import 'event_queue.dart';

/// Estado da rota em andamento, juntando servidor e fila local.
///
/// A regra que organiza tudo aqui: **registrar nunca falha por falta de rede**. O evento
/// entra na fila, a tela já reflete o efeito dele, e a sincronização acontece depois —
/// quando a conexão voltar ou na próxima vez que o app abrir.
class RouteController extends ChangeNotifier {
  RouteController({ApiClient? api, EventQueue? queue})
      : _api = api ?? ApiClient(baseUrl: ApiClient.defaultBaseUrl),
        _queue = queue ?? EventQueue();

  final ApiClient _api;
  final EventQueue _queue;
  final _uuid = const Uuid();

  StreamSubscription<List<ConnectivityResult>>? _connectivitySubscription;

  MobileRoute? _route;
  List<PendingEvent> _pending = [];
  bool _isLoading = false;
  String? _error;

  MobileRoute? get route => _route;
  bool get isLoading => _isLoading;
  String? get error => _error;
  int get pendingCount => _pending.length;

  /// Paradas com a fila local aplicada por cima: uma entrega registrada offline já
  /// aparece concluída, senão o motorista registraria de novo achando que não pegou.
  List<RouteStop> get stops {
    final route = _route;
    if (route == null) return const [];

    final deliveredOffline = _pending
        .where((event) => event.type == RouteEventType.entrega && event.stopIndex != null)
        .map((event) => event.stopIndex)
        .toSet();

    return route.stops
        .map((stop) => deliveredOffline.contains(stop.number) ? stop.copyWith(delivered: true) : stop)
        .toList();
  }

  List<RouteEvent> get events {
    final confirmed = _route?.events ?? const <RouteEvent>[];
    final queued = _pending.map((event) => event.toRouteEvent());
    return [...confirmed, ...queued]..sort((a, b) => a.occurredAt.compareTo(b.occurredAt));
  }

  int get deliveredCount => stops.where((stop) => stop.delivered).length;

  RouteStop? get nextStop {
    for (final stop in stops) {
      if (!stop.delivered) return stop;
    }
    return null;
  }

  Future<String?> savedToken() => _queue.readToken();

  /// Abre a rota pelo token. Se a rede falhar mas já houver rota carregada, mantém o que
  /// está na tela — é melhor um dado de minutos atrás do que uma tela vazia na rua.
  Future<void> open(String token) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    _pending = await _queue.load();

    try {
      _route = await _api.fetchRoute(token);
      await _queue.saveToken(token);
      _error = null;
      await _sync();
    } on ApiException catch (ex) {
      if (ex.isRouteGone) {
        await _queue.clearToken();
        _route = null;
      }
      _error = ex.message;
    } finally {
      _isLoading = false;
      notifyListeners();
    }

    _watchConnectivity();
  }

  /// Registra um evento. Grava na fila primeiro e tenta subir em seguida — nessa ordem,
  /// porque o que não pode acontecer é o app perder um registro feito em campo.
  Future<void> registerEvent({
    required RouteEventType type,
    int? stopIndex,
    String? notes,
  }) async {
    final event = PendingEvent(
      clientEventId: _uuid.v4(),
      type: type,
      stopIndex: stopIndex,
      notes: notes,
      occurredAt: DateTime.now(),
    );

    await _queue.add(event);
    _pending = await _queue.load();
    notifyListeners();

    await _sync();
  }

  /// Sobe a fila e recarrega a rota. Silenciosa de propósito: falha de sincronização não
  /// vira erro na tela, porque o evento continua guardado e o selo de pendentes já conta
  /// a história.
  Future<void> _sync() async {
    final token = await _queue.readToken();
    if (token == null || _pending.isEmpty) return;

    try {
      final confirmed = await _api.syncEvents(token, _pending);
      if (confirmed.isNotEmpty) {
        _pending = await _queue.removeConfirmed(confirmed);
        _route = await _api.fetchRoute(token);
      }
    } on ApiException {
      // Continua na fila. Nova tentativa quando a conexão voltar.
    } finally {
      notifyListeners();
    }
  }

  Future<void> refresh() async {
    final token = await _queue.readToken();
    if (token == null) return;
    await open(token);
  }

  Future<void> close() async {
    await _queue.clearToken();
    _route = null;
    notifyListeners();
  }

  void _watchConnectivity() {
    _connectivitySubscription ??= Connectivity().onConnectivityChanged.listen((results) {
      final online = results.any((result) => result != ConnectivityResult.none);
      if (online && _pending.isNotEmpty) {
        _sync();
      }
    });
  }

  @override
  void dispose() {
    _connectivitySubscription?.cancel();
    super.dispose();
  }
}
