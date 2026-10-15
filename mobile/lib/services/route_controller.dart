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

  String? _code;
  MobileRoute? _route;
  List<PendingEvent> _pending = [];
  int _strandedCount = 0;
  String? _rejection;
  bool _isLoading = false;
  String? _error;

  MobileRoute? get route => _route;
  bool get isLoading => _isLoading;
  String? get error => _error;
  int get pendingCount => _pending.length;

  /// Quantos registros de **outras** rotas ainda não subiram. A tela avisa: são de uma
  /// rota que o motorista já fechou, e ele é a única pessoa que pode voltar para uma área
  /// com sinal sabendo que há algo a subir.
  int get strandedCount => _strandedCount;

  /// Último registro recusado em definitivo pelo servidor, se houve. Recusa não é falha
  /// de rede: o evento saiu da fila e não vai voltar, então precisa ser dito.
  String? get rejection => _rejection;

  bool get isFinished => _route?.status == routeStatusFinished;

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

  bool get allDelivered {
    final list = stops;
    return list.isNotEmpty && list.every((stop) => stop.delivered);
  }

  Future<String?> savedCode() => _queue.readCode();

  /// Abre a rota pelo código. Se a rede falhar mas já houver rota carregada, mantém o que
  /// está na tela — é melhor um dado de minutos atrás do que uma tela vazia na rua.
  ///
  /// Antes de trocar de rota, tenta subir o que ficou pendente da anterior. É o momento
  /// certo para isso: o motorista costuma abrir a rota nova no pátio, onde há sinal, e
  /// depois de sair ninguém mais se lembra daquela entrega que não subiu.
  Future<void> open(String code) async {
    _isLoading = true;
    _error = null;
    _rejection = null;
    notifyListeners();

    // Uma vez, vindo da versão de fila única: atribui aquela fila à rota em que ela foi
    // criada antes que esta abertura mude o código salvo.
    await _queue.migrateLegacyQueue(await _queue.readCode());
    await _flushStranded(except: code);

    _code = code;
    _pending = await _queue.load(code);

    try {
      _route = await _api.fetchRoute(code);
      await _queue.saveCode(code);
      _error = null;
      await _sync();
    } on ApiException catch (ex) {
      if (ex.isRouteGone) {
        await _queue.clearCode();
        _code = null;
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
    final code = _code;
    if (code == null) return;

    final event = PendingEvent(
      clientEventId: _uuid.v4(),
      type: type,
      stopIndex: stopIndex,
      notes: notes,
      occurredAt: DateTime.now(),
    );

    await _queue.add(code, event);
    _pending = await _queue.load(code);
    notifyListeners();

    await _sync();
  }

  /// Encerra a rota: registra o retorno ao depósito e fecha no servidor.
  ///
  /// Devolve `null` quando encerrou, ou a frase que explica por que não deu — é o que a
  /// tela mostra. O encerramento exige servidor de propósito: fechar só no aparelho
  /// deixaria o gestor olhando uma rota em andamento que já acabou, e é justamente o
  /// quadro que esta tela existe para evitar.
  ///
  /// O evento de retorno vai pela fila, como todos os outros, então ele sobrevive à falta
  /// de sinal mesmo quando o fechamento não acontece agora.
  Future<String?> finish() async {
    final code = _code;
    if (code == null) return 'Nenhuma rota aberta.';

    final alreadyReturned = events.any((event) => event.type == RouteEventType.retorno);
    if (alreadyReturned) {
      await _sync();
    } else {
      await registerEvent(type: RouteEventType.retorno);
    }

    if (_pending.isNotEmpty) {
      final count = _pending.length;
      return count == 1
          ? 'Falta subir 1 registro antes de encerrar. Ele sobe sozinho quando a conexão voltar — tente de novo em seguida.'
          : 'Faltam subir $count registros antes de encerrar. Eles sobem sozinhos quando a conexão voltar — tente de novo em seguida.';
    }

    try {
      _route = await _api.finishRoute(code);
    } on ApiException catch (ex) {
      notifyListeners();
      return ex.message;
    }

    await close();
    return null;
  }

  /// Sobe a fila da rota aberta e recarrega a rota. Silenciosa quanto à rede de propósito:
  /// falha de sincronização não vira erro na tela, porque o evento continua guardado e o
  /// selo de pendentes já conta a história. Recusa definitiva, sim, é dita.
  Future<void> _sync() async {
    final code = _code;
    if (code == null || _pending.isEmpty) return;

    try {
      final outcome = await _api.syncEvents(code, _pending);
      if (outcome.settled.isNotEmpty) {
        _pending = await _queue.removeSettled(code, outcome.settled);
        if (outcome.rejected.isNotEmpty) {
          _rejection = outcome.rejected.values.first;
        }
        _route = await _api.fetchRoute(code);
      }
    } on ApiException {
      // Continua na fila. Nova tentativa quando a conexão voltar.
    } finally {
      notifyListeners();
    }
  }

  /// Sobe as filas de rotas que não são a que está sendo aberta.
  ///
  /// Cada fila vai para o código em que nasceu — é isso que impede o registro de uma rota
  /// de ser gravado em outra. O que não subir continua guardado e conta em
  /// [strandedCount].
  Future<void> _flushStranded({String? except}) async {
    var stranded = 0;

    for (final code in await _queue.pendingCodes()) {
      if (code == except) continue;

      final queued = await _queue.load(code);
      if (queued.isEmpty) continue;

      try {
        final outcome = await _api.syncEvents(code, queued);
        final remaining = await _queue.removeSettled(code, outcome.settled);
        stranded += remaining.length;
        if (outcome.rejected.isNotEmpty) {
          _rejection = outcome.rejected.values.first;
        }
      } on ApiException {
        stranded += queued.length;
      }
    }

    _strandedCount = stranded;
  }

  Future<void> refresh() async {
    final code = _code ?? await _queue.readCode();
    if (code == null) return;
    await open(code);
  }

  /// Sai da rota sem encerrá-la — o motorista vai abrir outra, ou passou o aparelho.
  /// A fila da rota continua guardada: o que ele registrou não se perde por sair da tela.
  Future<void> close() async {
    await _queue.clearCode();
    _pending = [];
    _code = null;
    _route = null;
    _error = null;
    notifyListeners();

    // O que ficou pendente desta rota passa a ser fila de outra rota, do ponto de vista
    // de quem está na tela: entra na conta que a abertura da próxima vai tentar subir.
    _strandedCount = await _countStranded();
    notifyListeners();
  }

  Future<int> _countStranded() async {
    var total = 0;
    for (final code in await _queue.pendingCodes()) {
      total += (await _queue.load(code)).length;
    }
    return total;
  }

  void dismissRejection() {
    _rejection = null;
    notifyListeners();
  }

  void _watchConnectivity() {
    _connectivitySubscription ??= Connectivity().onConnectivityChanged.listen((results) {
      final online = results.any((result) => result != ConnectivityResult.none);
      if (!online) return;

      if (_pending.isNotEmpty) _sync();
      if (_strandedCount > 0) _flushStranded(except: _code).then((_) => notifyListeners());
    });
  }

  @override
  void dispose() {
    _connectivitySubscription?.cancel();
    super.dispose();
  }
}
