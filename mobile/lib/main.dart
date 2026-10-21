import 'package:flutter/material.dart';

import 'screens/login_screen.dart';
import 'screens/open_route_screen.dart';
import 'screens/route_summary_screen.dart';
import 'services/deep_link_service.dart';
import 'services/route_controller.dart';
import 'services/session_controller.dart';
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
  /// A sessão vem antes da rota: desde que o servidor passou a exigir token em
  /// `/api/mobile/**`, abrir rota sem estar logado só produziria 401.
  final _session = SessionController();
  late final RouteController _controller = RouteController(
    api: _session.api,
    // Token recusado no meio do caminho (expirou, ou o gestor bloqueou o acesso): o app
    // volta ao login em vez de insistir numa chamada que não vai mais passar.
    onUnauthenticated: _session.signOut,
  );
  final _deepLinks = DeepLinkService();
  final _navigatorKey = GlobalKey<NavigatorState>();

  /// Abertura: restaura a sessão e descobre qual código abrir. O do link que abriu o app
  /// tem prioridade sobre o da última rota guardada — se o gestor mandou outro link, é
  /// nele que o motorista quer entrar, não na rota de ontem.
  late final Future<String?> _startup = _resolveStartup();

  @override
  void initState() {
    super.initState();
    // Link recebido com o app já aberto (tocar no WhatsApp em segundo plano): troca a
    // rota na hora, sem passar pela tela de abertura.
    _deepLinks.listen(_openFromLink);
  }

  Future<String?> _resolveStartup() async {
    await _session.restore();
    return await _deepLinks.initialCode() ?? await _controller.savedCode();
  }

  Future<void> _openFromLink(String code) async {
    // Link chegando sem sessão: o código fica guardado pelo próprio controller e a tela
    // de login segue na frente. Abrir a rota aqui só geraria 401.
    if (!_session.isSignedIn) return;

    await _controller.open(code);
    final navigator = _navigatorKey.currentState;
    if (navigator == null || _controller.route == null) return;

    navigator.pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => RouteSummaryScreen(controller: _controller, session: _session)),
      (route) => route.isFirst,
    );
  }

  @override
  void dispose() {
    _deepLinks.dispose();
    _controller.dispose();
    _session.dispose();
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
        future: _startup,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }

          return _SessionGate(
            session: _session,
            controller: _controller,
            startupCode: snapshot.data,
          );
        },
      ),
    );
  }
}

/// Escolhe entre login e rota, e refaz essa escolha quando a sessão muda — é por aqui que
/// um 401 no meio do dia leva o motorista de volta ao login sem o app travar numa tela.
class _SessionGate extends StatelessWidget {
  const _SessionGate({required this.session, required this.controller, required this.startupCode});

  final SessionController session;
  final RouteController controller;
  final String? startupCode;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: session,
      builder: (context, _) {
        if (!session.isSignedIn) {
          return LoginScreen(controller: session);
        }

        if (startupCode == null) {
          return OpenRouteScreen(controller: controller, session: session);
        }

        // Já há um código: entra direto na rota. Se ele tiver sido revogado, o
        // controller limpa e a tela cai para a abertura.
        return _ResumeRoute(controller: controller, session: session, code: startupCode!);
      },
    );
  }
}

class _ResumeRoute extends StatefulWidget {
  const _ResumeRoute({required this.controller, required this.session, required this.code});

  final RouteController controller;
  final SessionController session;
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
          return OpenRouteScreen(
            controller: widget.controller,
            session: widget.session,
            initialCode: widget.code,
          );
        }
        return RouteSummaryScreen(controller: widget.controller, session: widget.session);
      },
    );
  }
}
