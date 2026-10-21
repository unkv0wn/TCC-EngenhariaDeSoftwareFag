import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/route_models.dart';
import '../services/route_controller.dart';
import '../services/session_controller.dart';
import '../theme/app_theme.dart';
import '../widgets/sync_badge.dart';
import 'stop_detail_screen.dart';

/// Rota do dia: resumo, progresso, próxima parada em destaque e a lista completa.
class RouteSummaryScreen extends StatelessWidget {
  const RouteSummaryScreen({super.key, required this.controller, required this.session});

  final RouteController controller;
  final SessionController session;

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
              Center(child: SyncBadge(pendingCount: controller.pendingCount)),
              PopupMenuButton<_RouteAction>(
                onSelected: (action) => _onAction(context, action),
                itemBuilder: (_) => const [
                  PopupMenuItem(
                    value: _RouteAction.switchRoute,
                    child: ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      leading: Icon(Icons.qr_code_scanner, size: 20),
                      title: Text('Abrir outra rota'),
                    ),
                  ),
                  PopupMenuItem(
                    value: _RouteAction.signOut,
                    child: ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      leading: Icon(Icons.logout, size: 20),
                      title: Text('Sair da conta'),
                    ),
                  ),
                ],
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
                if (controller.rejection != null) _rejectionBanner(context, controller.rejection!),
                if (controller.strandedCount > 0) _strandedBanner(controller.strandedCount),
                if (controller.isFinished) _finishedBanner(),
                if (next != null && !controller.isFinished) _nextStopCard(context, next),
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
                if (!controller.isFinished) _finishSection(context, stops),
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

  /// O fim da rota. Fica depois da lista, e não numa barra fixa, porque encerrar é o
  /// último toque do dia — ter que rolar até ele é o que impede o toque acidental em cima
  /// de um botão que o motorista não pode desfazer sozinho.
  Widget _finishSection(BuildContext context, List<RouteStop> stops) {
    final remaining = stops.where((stop) => !stop.delivered).length;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Divider(),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: () => _finish(context, remaining),
            icon: const Icon(Icons.flag_outlined, size: 18),
            label: const Text('Encerrar rota'),
            style: FilledButton.styleFrom(
              backgroundColor: remaining == 0 ? AppColors.success600 : AppColors.gray600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            remaining == 0
                ? 'Registra o retorno ao depósito e fecha a rota para o gestor.'
                : '$remaining ${remaining == 1 ? 'parada' : 'paradas'} sem entrega registrada.',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: AppColors.gray500, height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _finishedBanner() {
    return _banner(
      icon: Icons.check_circle_outline,
      color: AppColors.success700,
      background: AppColors.success50,
      border: AppColors.success600,
      text: 'Rota encerrada. Não aceita mais registros — para corrigir algo, fale com o gestor.',
    );
  }

  /// Registros de uma rota que não é mais a aberta. Ele é a única pessoa capaz de levar o
  /// aparelho de volta a um lugar com sinal, então precisa saber que há algo preso aqui.
  Widget _strandedBanner(int count) {
    return _banner(
      icon: Icons.cloud_off_outlined,
      color: AppColors.warning800,
      background: AppColors.warning50,
      border: AppColors.warning200,
      text: count == 1
          ? '1 registro de uma rota anterior ainda não subiu. Ele sobe sozinho quando houver conexão.'
          : '$count registros de rotas anteriores ainda não subiram. Eles sobem sozinhos quando houver conexão.',
    );
  }

  /// Recusa definitiva é diferente de falta de rede: o registro saiu da fila e não volta.
  /// Fica na tela até o motorista dispensar, porque é a única chance de ele saber.
  Widget _rejectionBanner(BuildContext context, String message) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Container(
        padding: const EdgeInsets.fromLTRB(13, 11, 7, 11),
        decoration: BoxDecoration(
          color: AppColors.danger50,
          border: Border.all(color: AppColors.danger200),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.error_outline, size: 18, color: AppColors.danger700),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: AppColors.danger700,
                  height: 1.5,
                ),
              ),
            ),
            IconButton(
              onPressed: controller.dismissRejection,
              icon: const Icon(Icons.close, size: 18, color: AppColors.danger700),
              tooltip: 'Dispensar',
            ),
          ],
        ),
      ),
    );
  }

  Widget _banner({
    required IconData icon,
    required Color color,
    required Color background,
    required Color border,
    required String text,
  }) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
        decoration: BoxDecoration(
          color: background,
          border: Border.all(color: border),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 18, color: color),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                text,
                style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: color, height: 1.5),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _onAction(BuildContext context, _RouteAction action) async {
    switch (action) {
      case _RouteAction.switchRoute:
        await _switchRoute(context);
      case _RouteAction.signOut:
        await _signOut(context);
    }
  }

  /// Sair da conta no meio da rota.
  ///
  /// A rota não é encerrada e a fila não é apagada: o aviso diz isso porque sair parece
  /// perder tudo, e quem sai costuma estar devolvendo o aparelho no fim do turno — com
  /// registro pendente que ainda vai subir no próximo login.
  Future<void> _signOut(BuildContext context) async {
    final pending = controller.pendingCount;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Sair da conta?'),
        content: Text(
          pending == 0
              ? 'A rota continua aberta e você volta a ela pelo mesmo código depois de entrar de novo.'
              : 'Há $pending ${pending == 1 ? 'registro' : 'registros'} que ainda não subiram. '
                  'Eles ficam guardados neste aparelho e sobem quando você entrar de novo com conexão.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Sair')),
        ],
      ),
    );

    if (confirmed != true) return;
    await session.signOut();

    // Mesmo tratamento de "abrir outra rota": o portão de sessão da raiz troca a tela
    // sozinho quando esta é a tela inicial, mas quando ela foi empilhada sobre a abertura
    // é preciso o pop — senão a rota fica viva por cima do login.
    if (context.mounted) _leave(context);
  }

  /// Sair da rota sem encerrá-la — é como se abre a segunda rota do dia quando o código
  /// novo chegou por telefone, sem link para tocar.
  Future<void> _switchRoute(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Abrir outra rota'),
        content: Text(
          controller.isFinished
              ? 'Esta rota já está encerrada. Você pode abri-la de novo pelo mesmo código para consultar o histórico.'
              : 'Esta rota continua em andamento para o gestor e você pode voltar a ela pelo mesmo código. '
                  'O que você registrou e ainda não subiu continua guardado.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Abrir outra')),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;
    await controller.close();
    if (context.mounted) _leave(context);
  }

  Future<void> _finish(BuildContext context, int remaining) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Encerrar a rota?'),
        content: Text(
          remaining == 0
              ? 'Registra o retorno ao depósito e fecha a rota. Depois disso não é possível registrar mais nada por aqui.'
              : 'Ainda há $remaining ${remaining == 1 ? 'parada' : 'paradas'} sem entrega registrada. '
                  'Encerrando agora, elas ficam assim no histórico e não dá para registrar mais nada por aqui.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            style: FilledButton.styleFrom(
              backgroundColor: remaining == 0 ? AppColors.success600 : AppColors.danger600,
            ),
            child: const Text('Encerrar'),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    final problem = await controller.finish();
    if (!context.mounted) return;

    if (problem != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(problem), duration: const Duration(seconds: 6)),
      );
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Rota encerrada. Bom descanso!')),
    );
    _leave(context);
  }

  /// Volta para a tela de abertura. Quando esta tela foi empilhada sobre ela, basta o pop;
  /// quando ela é a tela inicial (app aberto já com uma rota salva), quem reage é o
  /// `ListenableBuilder` da raiz, que vê `route == null` e mostra a abertura.
  void _leave(BuildContext context) {
    final navigator = Navigator.of(context);
    if (navigator.canPop()) navigator.pop();
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

/// Ações do menu da rota. Enum de um item só por enquanto, mas é ele que mantém o
/// `onSelected` honesto quando a segunda ação aparecer.
enum _RouteAction { switchRoute, signOut }
