/// Modelos do app, espelhando os DTOs de `/api/mobile/**`.
///
/// Repare no que NÃO existe aqui: id de rota, de motorista ou de veículo. O app
/// navega por token e recebe tudo já resolvido — ele não tem como, e não deve
/// poder, montar uma chamada ao CRUD do gestor.
library;

class MobileRoute {
  const MobileRoute({
    required this.accessToken,
    required this.createdAt,
    required this.status,
    required this.departureTime,
    required this.ordersCount,
    required this.totalValue,
    required this.totalDistanceKm,
    required this.totalDurationMin,
    required this.driverName,
    required this.vehiclePlate,
    required this.vehicleModel,
    required this.vehicleCapacityKg,
    required this.stops,
    required this.events,
  });

  final String accessToken;
  final DateTime createdAt;
  final String status;
  final String departureTime;
  final int ordersCount;
  final double totalValue;
  final double totalDistanceKm;
  final double totalDurationMin;
  final String driverName;
  final String? vehiclePlate;
  final String? vehicleModel;
  final double vehicleCapacityKg;
  final List<RouteStop> stops;
  final List<RouteEvent> events;

  int get deliveredCount => stops.where((stop) => stop.delivered).length;

  /// Primeira parada ainda não entregue — é ela que a tela destaca.
  RouteStop? get nextStop {
    for (final stop in stops) {
      if (!stop.delivered) return stop;
    }
    return null;
  }

  factory MobileRoute.fromJson(Map<String, dynamic> json) {
    return MobileRoute(
      accessToken: json['accessToken'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      status: json['status'] as String,
      departureTime: json['departureTime'] as String,
      ordersCount: (json['ordersCount'] as num).toInt(),
      totalValue: (json['totalValue'] as num).toDouble(),
      totalDistanceKm: (json['totalDistanceKm'] as num).toDouble(),
      totalDurationMin: (json['totalDurationMin'] as num).toDouble(),
      driverName: json['driverName'] as String? ?? 'Motorista',
      vehiclePlate: json['vehiclePlate'] as String?,
      vehicleModel: json['vehicleModel'] as String?,
      vehicleCapacityKg: (json['vehicleCapacityKg'] as num?)?.toDouble() ?? 0,
      stops: (json['stops'] as List<dynamic>? ?? const [])
          .map((item) => RouteStop.fromJson(item as Map<String, dynamic>))
          .toList(),
      events: (json['events'] as List<dynamic>? ?? const [])
          .map((item) => RouteEvent.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class RouteStop {
  const RouteStop({
    required this.number,
    required this.label,
    required this.lat,
    required this.lng,
    required this.ordersCount,
    required this.delivered,
  });

  /// Posição na ordem otimizada — o mesmo número que o gestor vê no mapa.
  final int number;
  final String label;
  final double lat;
  final double lng;
  final int ordersCount;
  final bool delivered;

  RouteStop copyWith({bool? delivered}) => RouteStop(
        number: number,
        label: label,
        lat: lat,
        lng: lng,
        ordersCount: ordersCount,
        delivered: delivered ?? this.delivered,
      );

  factory RouteStop.fromJson(Map<String, dynamic> json) {
    return RouteStop(
      number: (json['number'] as num).toInt(),
      label: json['label'] as String? ?? '',
      lat: (json['lat'] as num).toDouble(),
      lng: (json['lng'] as num).toDouble(),
      ordersCount: (json['ordersCount'] as num?)?.toInt() ?? 1,
      delivered: json['delivered'] as bool? ?? false,
    );
  }
}

/// Tipos aceitos pelo backend (`RouteEventServiceImpl.VALID_TYPES`). Incluir um tipo
/// aqui sem incluir lá faz o evento ser recusado com 400 na hora de sincronizar.
enum RouteEventType {
  saida('saida', 'Saída do depósito'),
  chegada('chegada', 'Chegada na parada'),
  entrega('entrega', 'Entrega concluída'),
  ocorrencia('ocorrencia', 'Ocorrência'),
  abastecimento('abastecimento', 'Abastecimento'),
  retorno('retorno', 'Retorno ao depósito');

  const RouteEventType(this.wireValue, this.label);

  final String wireValue;
  final String label;

  static RouteEventType fromWire(String value) {
    return RouteEventType.values.firstWhere(
      (type) => type.wireValue == value,
      orElse: () => RouteEventType.ocorrencia,
    );
  }
}

class RouteEvent {
  const RouteEvent({
    required this.type,
    required this.occurredAt,
    this.stopIndex,
    this.notes,
    this.authorName,
    this.pending = false,
  });

  final RouteEventType type;
  final DateTime occurredAt;
  final int? stopIndex;
  final String? notes;
  final String? authorName;

  /// true enquanto o evento está só na fila local, sem confirmação do servidor.
  final bool pending;

  factory RouteEvent.fromJson(Map<String, dynamic> json) {
    return RouteEvent(
      type: RouteEventType.fromWire(json['type'] as String),
      occurredAt: DateTime.parse(json['occurredAt'] as String),
      stopIndex: (json['stopIndex'] as num?)?.toInt(),
      notes: json['notes'] as String?,
      authorName: json['authorName'] as String?,
    );
  }
}

/// Evento aguardando envio. O `clientEventId` nasce no aparelho e é o que impede
/// um reenvio (rede caindo na hora da resposta) de virar uma segunda entrega.
class PendingEvent {
  const PendingEvent({
    required this.clientEventId,
    required this.type,
    required this.occurredAt,
    this.stopIndex,
    this.notes,
  });

  final String clientEventId;
  final RouteEventType type;
  final DateTime occurredAt;
  final int? stopIndex;
  final String? notes;

  Map<String, dynamic> toJson() => {
        'clientEventId': clientEventId,
        'type': type.wireValue,
        'stopIndex': stopIndex,
        'notes': notes,
        'occurredAt': occurredAt.toUtc().toIso8601String(),
      };

  factory PendingEvent.fromJson(Map<String, dynamic> json) {
    return PendingEvent(
      clientEventId: json['clientEventId'] as String,
      type: RouteEventType.fromWire(json['type'] as String),
      occurredAt: DateTime.parse(json['occurredAt'] as String),
      stopIndex: (json['stopIndex'] as num?)?.toInt(),
      notes: json['notes'] as String?,
    );
  }

  RouteEvent toRouteEvent() => RouteEvent(
        type: type,
        occurredAt: occurredAt,
        stopIndex: stopIndex,
        notes: notes,
        pending: true,
      );
}
