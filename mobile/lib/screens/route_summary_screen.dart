import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/route_models.dart';
import '../services/route_controller.dart';
import '../theme/app_theme.dart';
import '../widgets/sync_badge.dart';
import 'stop_detail_screen.dart';

/// Rota do dia: resumo, progresso, próxima parada em destaque e a lista completa.
class RouteSummaryScreen extends StatelessWidget {
  const RouteSummaryScreen({super.key, required this.controller});

  final RouteController controller;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final route = controller.route;
        if (route == null) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }

        final stops = controller.stops;
        final next = controller.nextStop;

        return Scaffold(
          appBar: AppBar(
            title: const Text('Rota do dia'),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 12),
                child: Center(child: SyncBadge(pendingCount: controller.pendingCount)),
              ),
            ],
          ),
          body: RefreshIndicator(
            onRefresh: controller.refresh,
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _summaryHeader(route),
                _vehicleAndProgress(route, stops),
                if (next != null) _nextStopCard(context, next),
                const Padding(
                  padding: EdgeInsets.fromLTRB(16, 18, 16, 8),
                  child: Text(
                    'TODAS AS PARADAS',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.6,
                      color: AppColors.gray600,
                    ),
                  ),
                ),
                ...stops.map((stop) => _stopTile(context, stop, isNext: stop.number == next?.number)),
                const SizedBox(height: 24),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _summaryHeader(MobileRoute route) {
    return Container(
      width: double.infinity,
      color: AppColors.primary600,
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
      child: Row(
        children: [
          _headerStat('Saída', route.departureTime),
          _headerStat('Distância', '${route.totalDistanceKm.toStringAsFixed(1)} km'),
          _headerStat('Tempo', '${route.totalDurationMin.round()} min'),
        ],
      ),
    );
  }

  Widget _headerStat(String label, String value) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: AppColors.primary200, fontSize: 11, fontWeight: FontWeight.w600)),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }

  Widget _vehicleAndProgress(MobileRoute route, List<RouteStop> stops) {
    final delivered = stops.where((stop) => stop.delivered).length;
    final total = stops.length;
    final ratio = total == 0 ? 0.0 : delivered / total;
    final weight = NumberFormat.decimalPattern('pt_BR').format(route.vehicleCapacityKg);

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(20, 14, 20, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(color: AppColors.gray100, borderRadius: BorderRadius.circular(10)),
                child: const Icon(Icons.local_shipping_outlined, size: 18, color: AppColors.gray600),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      [route.vehiclePlate, route.vehicleModel].whereType<String>().join(' — '),
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: AppColors.gray900),
                    ),
                    Text(
                      '${route.driverName} · $weight kg',
                      style: const TextStyle(fontSize: 12, color: AppColors.gray600),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '$delivered de $total entregues',
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.gray900),
              ),
              Text(
                '${total - delivered} restantes',
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.gray600),
              ),
            ],
          ),
          const SizedBox(height: 7),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: ratio,
              minHeight: 8,
              backgroundColor: AppColors.gray200,
              valueColor: const AlwaysStoppedAnimation(AppColors.success600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _nextStopCard(BuildContext context, RouteStop stop) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'PRÓXIMA PARADA',
            style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 0.6, color: AppColors.gray600),
          ),
          const SizedBox(height: 8),
          Material(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => _openStop(context, stop),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.primary600, width: 2),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: [
                    _numberBadge(stop.number, background: AppColors.primary600, foreground: Colors.white, size: 34),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            stop.label,
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: AppColors.gray900),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            _ordersLabel(stop.ordersCount),
                            style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.gray600),
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right, color: AppColors.gray400),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _stopTile(BuildContext context, RouteStop stop, {required bool isNext}) {
    final done = stop.delivered;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: () => _openStop(context, stop),
          child: Container(
            constraints: const BoxConstraints(minHeight: kTouchTarget),
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
            decoration: BoxDecoration(
              border: Border.all(color: AppColors.gray200),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                _numberBadge(
                  stop.number,
                  background: done
                      ? AppColors.success600
                      : isNext
                          ? AppColors.primary600
                          : AppColors.gray100,
                  foreground: done || isNext ? Colors.white : AppColors.gray600,
                  size: 28,
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        stop.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w700, color: AppColors.gray900),
                      ),
                      Text(
                        _ordersLabel(stop.ordersCount),
                        style: const TextStyle(fontSize: 11.5, color: AppColors.gray600),
                      ),
                    ],
                  ),
                ),
                Text(
                  done ? 'Entregue' : (isNext ? 'Próxima' : 'Pendente'),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: done
                        ? AppColors.success700
                        : isNext
                            ? AppColors.primary700
                            : AppColors.gray500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _numberBadge(int number, {required Color background, required Color foreground, required double size}) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: background, shape: BoxShape.circle),
      child: Text(
        '$number',
        style: TextStyle(color: foreground, fontSize: size * 0.45, fontWeight: FontWeight.w800),
      ),
    );
  }

  String _ordersLabel(int count) => count == 1 ? '1 pedido' : '$count pedidos';

  void _openStop(BuildContext context, RouteStop stop) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => StopDetailScreen(controller: controller, stopNumber: stop.number),
      ),
    );
  }
}
