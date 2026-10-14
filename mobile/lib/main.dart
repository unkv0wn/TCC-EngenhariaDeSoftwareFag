import 'package:flutter/material.dart';

import 'screens/open_route_screen.dart';
import 'screens/route_summary_screen.dart';
import 'services/deep_link_service.dart';
import 'services/route_controller.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const RouteWiseDriverApp());
}

class RouteWiseDriverApp extends StatefulWidget {
  const RouteWiseDriverApp({super.key});

  @override
  State<RouteWiseDriverApp> createState() => _RouteWiseDriverAppState();
}

class _RouteWiseDriverAppState extends State<RouteWiseDriverApp> {
  final _controller = RouteController();
  final _deepLinks = DeepLinkService();
  final _navigatorKey = GlobalKey<NavigatorState>();

  /// Qual código abrir ao iniciar: o do link que abriu o app tem prioridade sobre o da
  /// última rota guardada — se o gestor mandou outro link, é nele que o motorista quer
  /// entrar, não na rota de ontem.
  late final Future<String?> _startupCode = _resolveStartupCode();

  @override
  void initState() {
    super.initState();
    // Link recebido com o app já aberto (tocar no WhatsApp em segundo plano): troca a
    // rota na hora, sem passar pela tela de abertura.
    _deepLinks.listen(_openFromLink);
  }

  Future<String?> _resolveStartupCode() async {
    return await _deepLinks.initialCode() ?? await _controller.savedCode();
  }

  Future<void> _openFromLink(String code) async {
    await _controller.open(code);
    final navigator = _navigatorKey.currentState;
    if (navigator == null || _controller.route == null) return;

    navigator.pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => RouteSummaryScreen(controller: _controller)),
      (route) => route.isFirst,
    );
  }

  @override
  void dispose() {
    _deepLinks.dispose();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'RouteWise — Motorista',
      navigatorKey: _navigatorKey,
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: FutureBuilder<String?>(
        future: _startupCode,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }

          final code = snapshot.data;
          if (code == null) {
            return OpenRouteScreen(controller: _controller);
          }

          // Já há um código: entra direto na rota. Se ele tiver sido revogado, o
          // controller limpa e a tela cai para a abertura.
          return _ResumeRoute(controller: _controller, code: code);
        },
      ),
    );
  }
}

class _ResumeRoute extends StatefulWidget {
  const _ResumeRoute({required this.controller, required this.code});

  final RouteController controller;
  final String code;

  @override
  State<_ResumeRoute> createState() => _ResumeRouteState();
}

class _ResumeRouteState extends State<_ResumeRoute> {
  @override
  void initState() {
    super.initState();
    widget.controller.open(widget.code);
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.controller,
      builder: (context, _) {
        if (widget.controller.isLoading) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        if (widget.controller.route == null) {
          return OpenRouteScreen(controller: widget.controller, initialCode: widget.code);
        }
        return RouteSummaryScreen(controller: widget.controller);
      },
    );
  }
}
