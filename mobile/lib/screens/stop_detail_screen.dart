import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/route_models.dart';
import '../services/route_controller.dart';
import '../theme/app_theme.dart';
import '../widgets/event_form_sheet.dart';
import '../widgets/sync_badge.dart';

/// A tela onde o motorista passa o dia.
class StopDetailScreen extends StatelessWidget {
  const StopDetailScreen({super.key, required this.controller, required this.stopNumber});

  final RouteController controller;
  final int stopNumber;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final stops = controller.stops;
        final stop = stops.where((item) => item.number == stopNumber).firstOrNull;

        if (stop == null) {
          return Scaffold(
            appBar: AppBar(title: const Text('Parada')),
            body: const Center(child: Text('Parada não encontrada nesta rota.')),
          );
        }

        return Scaffold(
          appBar: AppBar(
            title: Text('Parada ${stop.number} de ${stops.length}'),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 12),
                child: Center(child: SyncBadge(pendingCount: controller.pendingCount)),
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _customerCard(stop),
              const SizedBox(height: 12),
              if (stop.delivered) _deliveredBanner() else _actions(context, stop),
              const SizedBox(height: 14),
              const Text(
                'Sem internet, o registro fica na fila e sobe sozinho quando a conexão voltar.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: AppColors.gray500, height: 1.5),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _customerCard(RouteStop stop) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              stop.label,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.gray900),
            ),
            const SizedBox(height: 6),
            Text(
              stop.ordersCount == 1 ? '1 pedido nesta parada' : '${stop.ordersCount} pedidos nesta parada',
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.gray600),
            ),
          ],
        ),
      ),
    );
  }

  Widget _deliveredBanner() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      decoration: BoxDecoration(
        color: AppColors.success50,
        border: Border.all(color: AppColors.success600),
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Row(
        children: [
          Icon(Icons.check_circle, color: AppColors.success700),
          SizedBox(width: 11),
          Expanded(
            child: Text(
              'Entrega registrada. Para corrigir, peça ao gestor para apagar o evento.',
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.success700, height: 1.4),
            ),
          ),
        ],
      ),
    );
  }

  Widget _actions(BuildContext context, RouteStop stop) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        FilledButton.icon(
          onPressed: () => _navigateTo(context, stop),
          icon: const Icon(Icons.navigation_outlined, size: 18),
          label: const Text('Navegar até aqui'),
        ),
        const SizedBox(height: 9),
        OutlinedButton.icon(
          onPressed: () => _register(context, RouteEventType.chegada, stop),
          icon: const Icon(Icons.place_outlined, size: 18, color: AppColors.secondary700),
          label: const Text('Cheguei', style: TextStyle(color: AppColors.secondary700)),
          style: OutlinedButton.styleFrom(
            backgroundColor: AppColors.secondary50,
            side: const BorderSide(color: AppColors.secondary700, width: 1.2),
          ),
        ),
        const SizedBox(height: 9),
        FilledButton.icon(
          onPressed: () => _confirmDelivery(context, stop),
          icon: const Icon(Icons.inventory_2_outlined, size: 18),
          label: const Text('Entreguei'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.success600),
        ),
        const SizedBox(height: 9),
        OutlinedButton.icon(
          onPressed: () => _reportIncident(context, stop),
          icon: const Icon(Icons.warning_amber_rounded, size: 18, color: AppColors.danger700),
          label: const Text('Registrar ocorrência', style: TextStyle(color: AppColors.danger700)),
          style: OutlinedButton.styleFrom(
            backgroundColor: AppColors.danger50,
            side: const BorderSide(color: AppColors.danger200, width: 1.5),
          ),
        ),
      ],
    );
  }

  /// Navegação é delegada ao app de mapas do celular: ele tem trânsito ao vivo, voz e
  /// o motorista já sabe usar. Reimplementar isso dentro do RouteWise seria pior em
  /// tudo que importa na rua.
  Future<void> _navigateTo(BuildContext context, RouteStop stop) async {
    final uri = Uri.parse('google.navigation:q=${stop.lat},${stop.lng}&mode=d');
    final fallback = Uri.parse('https://www.google.com/maps/dir/?api=1&destination=${stop.lat},${stop.lng}');

    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
      return;
    }
    await launchUrl(fallback, mode: LaunchMode.externalApplication);
  }

  /// "Entreguei" é o toque que muda o estado da rota, e desfazer significa pedir pro
  /// gestor apagar o evento. Por isso confirma antes.
  Future<void> _confirmDelivery(BuildContext context, RouteStop stop) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Confirmar entrega'),
        content: Text('Registrar a entrega da parada ${stop.number} — ${stop.label}?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            style: FilledButton.styleFrom(backgroundColor: AppColors.success600),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;
    await _register(context, RouteEventType.entrega, stop);
  }

  Future<void> _reportIncident(BuildContext context, RouteStop stop) async {
    final result = await EventFormSheet.show(context, stopLabel: stop.label, stopNumber: stop.number);
    if (result == null || !context.mounted) return;

    await controller.registerEvent(type: result.type, stopIndex: stop.number, notes: result.notes);
    if (context.mounted) _toast(context, 'Ocorrência registrada.');
  }

  Future<void> _register(BuildContext context, RouteEventType type, RouteStop stop) async {
    await controller.registerEvent(type: type, stopIndex: stop.number);
    if (!context.mounted) return;

    _toast(context, type == RouteEventType.entrega ? 'Entrega registrada.' : 'Chegada registrada.');
    if (type == RouteEventType.entrega) Navigator.of(context).pop();
  }

  void _toast(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}
