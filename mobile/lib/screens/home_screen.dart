import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/route_models.dart';
import '../services/api_client.dart';
import '../services/route_controller.dart';
import '../services/session_controller.dart';
import '../theme/app_theme.dart';
import 'route_summary_screen.dart';
import 'open_route_screen.dart';

/// Tela inicial depois do login: as rotas deste motorista.
///
/// Existe porque o login passou a dizer ao servidor quem está perguntando. Antes dele a
/// superfície do app só sabia responder "a rota deste código", então a única entrada
/// possível era o QR ou o código digitado — o motorista dependia de o gestor lhe mandar
/// alguma coisa antes de conseguir trabalhar.
///
/// Tocar num cartão abre a rota direto: ela já é dele, e o servidor confirmou isso ao
/// listá-la. A leitura de QR continua na tela, mas como porta secundária — serve para a
/// rota que acabou de ser atribuída e ainda não apareceu aqui.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.controller, required this.session});

  final RouteController controller;
  final SessionController session;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<RouteSummary>? _routes;
  String? _error;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final routes = await widget.session.api.fetchMyRoutes();
      if (!mounted) return;
      setState(() {
        _routes = routes;
        _isLoading = false;
      });
    } on ApiException catch (ex) {
      if (!mounted) return;
      // Sessão recusada: quem leva de volta ao login é o SessionController, e a raiz
      // troca a tela. Aqui só não se insiste num erro que não melhora com nova tentativa.
      if (ex.isUnauthenticated) {
        await widget.session.signOut();
        return;
      }
      setState(() {
        _error = ex.message;
        _isLoading = false;
      });
    }
  }

  /// Abre a rota e vai para a tela dela. Ao voltar, recarrega: a rota pode ter sido
  /// encerrada ou ter avançado, e a lista mostraria o estado de antes.
  Future<void> _open(String code) async {
    await widget.controller.open(code);
    if (!mounted) return;

    final error = widget.controller.error;
    if (error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error)));
      await _load();
      return;
    }

    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RouteSummaryScreen(controller: widget.controller, session: widget.session),
      ),
    );
    if (mounted) await _load();
  }

  /// Abre a tela de código — QR ou digitado. Ela devolve o código; quem abre a rota é
  /// esta tela, para a pilha ficar `inicial → rota` e o voltar cair na lista.
  Future<void> _openByCode() async {
    final code = await OpenRouteScreen.open(context, controller: widget.controller);
    if (code == null || !mounted) return;
    await _open(code);
  }

  Future<void> _signOut() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Sair da conta?'),
        content: const Text(
          'Você vai precisar do seu usuário e senha para entrar de novo. '
          'Os registros que ainda não subiram continuam guardados neste aparelho.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Sair')),
        ],
      ),
    );

    if (confirmed != true) return;
    await widget.session.signOut();
  }

  @override
  Widget build(BuildContext context) {
    final name = widget.session.session?.driverName ?? '';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Minhas rotas'),
        actions: [
          IconButton(
            onPressed: _signOut,
            icon: const Icon(Icons.logout),
            tooltip: 'Sair da conta',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openByCode,
        icon: const Icon(Icons.qr_code_scanner),
        label: const Text('Abrir por código'),
        backgroundColor: AppColors.primary600,
        foregroundColor: Colors.white,
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _body(name),
      ),
    );
  }

  Widget _body(String name) {
    if (_isLoading && _routes == null) {
      return const Center(child: CircularProgressIndicator());
    }

    final routes = _routes ?? const <RouteSummary>[];
    final open = routes.where((route) => route.isOpen).toList();
    final finished = routes.where((route) => route.isFinished).toList();

    return ListView(
      // Sempre rolável: sem isto o "puxar para atualizar" não funciona na tela vazia,
      // que é justamente quando o motorista mais quer tentar de novo.
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
      children: [
        if (name.isNotEmpty) _greeting(name),
        if (_error != null) ...[
          _errorBanner(_error!),
          const SizedBox(height: 16),
        ],
        if (routes.isEmpty && _error == null) _emptyState(),
        if (open.isNotEmpty) ...[
          _sectionTitle('Em aberto', open.length),
          const SizedBox(height: 10),
          ...open.map(_routeCard),
        ],
        if (finished.isNotEmpty) ...[
          const SizedBox(height: 22),
          _sectionTitle('Encerradas nos últimos dias', finished.length),
          const SizedBox(height: 10),
          ...finished.map(_routeCard),
        ],
      ],
    );
  }

  Widget _greeting(String name) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            name,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: AppColors.gray900),
          ),
          const SizedBox(height: 2),
          const Text(
            'Toque numa rota para abrir',
            style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.gray500),
          ),
        ],
      ),
    );
  }

  Widget _sectionTitle(String label, int count) {
    return Row(
      children: [
        Text(
          label.toUpperCase(),
          style: const TextStyle(
            fontSize: 11.5,
            fontWeight: FontWeight.w800,
            letterSpacing: 0.6,
            color: AppColors.gray600,
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
          decoration: BoxDecoration(
            color: AppColors.gray200,
            borderRadius: BorderRadius.circular(999),
          ),
          child: Text(
            '$count',
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: AppColors.gray700),
          ),
        ),
      ],
    );
  }

  Widget _routeCard(RouteSummary route) {
    // Padrão numérico explícito, sem locale: `DateFormat` com 'pt_BR' exigiria
    // `initializeDateFormatting`, que o app não chama — e a falta dele só apareceria
    // como exceção em runtime, nesta tela, no celular do motorista.
    final date = DateFormat('dd/MM').format(route.createdAt.toLocal());

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          onTap: () => _open(route.accessCode),
          borderRadius: BorderRadius.circular(16),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.gray200),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Rota de $date',
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: AppColors.gray900,
                        ),
                      ),
                    ),
                    _statusBadge(route.status),
                  ],
                ),
                const SizedBox(height: 10),
                _line(Icons.schedule, 'Saída ${route.departureTime}'),
                if (route.vehiclePlate != null)
                  _line(Icons.local_shipping_outlined,
                      '${route.vehiclePlate}${route.vehicleModel != null ? ' · ${route.vehicleModel}' : ''}'),
                _line(
                  Icons.inventory_2_outlined,
                  '${route.ordersCount} ${route.ordersCount == 1 ? 'pedido' : 'pedidos'}'
                  ' · ${route.totalDistanceKm.toStringAsFixed(1)} km',
                ),
                const SizedBox(height: 12),
                _progress(route),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _line(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          Icon(icon, size: 15, color: AppColors.gray400),
          const SizedBox(width: 7),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600, color: AppColors.gray600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _progress(RouteSummary route) {
    final done = route.deliveredStops;
    final total = route.totalStops;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(999),
          child: LinearProgressIndicator(
            value: route.progress,
            minHeight: 7,
            backgroundColor: AppColors.gray200,
            valueColor: AlwaysStoppedAnimation(
              route.isFinished ? AppColors.gray400 : AppColors.success600,
            ),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          '$done de $total ${total == 1 ? 'parada entregue' : 'paradas entregues'}',
          style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: AppColors.gray500),
        ),
      ],
    );
  }

  Widget _statusBadge(String status) {
    final (label, background, foreground) = switch (status) {
      routeStatusInProgress => ('Em rota', AppColors.primary50, AppColors.primary700),
      routeStatusFinished => ('Encerrada', AppColors.gray100, AppColors.gray600),
      _ => ('Planejada', AppColors.secondary50, AppColors.secondary700),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(999)),
      child: Text(
        label,
        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: foreground),
      ),
    );
  }

  Widget _emptyState() {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 48),
      child: Column(
        children: [
          Icon(Icons.route_outlined, size: 52, color: AppColors.gray300),
          SizedBox(height: 14),
          Text(
            'Nenhuma rota para você agora',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: AppColors.gray700),
          ),
          SizedBox(height: 6),
          Text(
            'Quando o gestor escalar uma rota no seu nome, ela aparece aqui.\n'
            'Se ele já te passou um código, use o botão abaixo.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600, color: AppColors.gray500, height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _errorBanner(String message) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.danger50,
        border: Border.all(color: AppColors.danger200),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.error_outline, color: AppColors.danger600, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontSize: 13.5,
                fontWeight: FontWeight.w700,
                color: AppColors.danger700,
                height: 1.35,
              ),
            ),
          ),
          TextButton(onPressed: _load, child: const Text('Tentar de novo')),
        ],
      ),
    );
  }
}
