import 'package:flutter/material.dart';

import 'screens/home_screen.dart';
import 'screens/login_screen.dart';
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
  /// A sessão vem antes de tudo: desde que o servidor passou a exigir token em
  /// `/api/mobile/**`, qualquer chamada sem ela só produziria 401.
  final _session = SessionController();
  late final RouteController _controller = RouteController(
    api: _session.api,
    // Token recusado no meio do caminho (expirou, ou o gestor bloqueou o acesso): o app
    // volta ao login em vez de insistir numa chamada que não vai mais passar.
    onUnauthenticated: _session.signOut,
  );
  final _deepLinks = DeepLinkService();
  final _navigatorKey = GlobalKey<NavigatorState>();

  late final Future<void> _startup = _session.restore();

  @override
  void initState() {
    super.initState();
    // Link recebido com o app já aberto (tocar no WhatsApp em segundo plano): abre a
    // rota por cima da tela inicial.
    _deepLinks.listen(_openFromLink);
    _handleInitialLink();
  }

  /// Link que abriu o app. Esperado o startup, senão a sessão ainda não foi restaurada e
  /// a chamada sairia sem token.
  Future<void> _handleInitialLink() async {
    await _startup;
    final code = await _deepLinks.initialCode();
    if (code != null) await _openFromLink(code);
  }

  Future<void> _openFromLink(String code) async {
    // Link chegando sem sessão: a tela de login segue na frente, e abrir a rota aqui só
    // geraria 401.
    if (!_session.isSignedIn) return;

    await _controller.open(code);
    final navigator = _navigatorKey.currentState;
    if (navigator == null || _controller.route == null) return;

    // Empilha sobre a tela inicial em vez de substituí-la: voltar da rota tem que levar
    // o motorista de volta à lista, não a lugar nenhum.
    navigator.push(
      MaterialPageRoute(
        builder: (_) => RouteSummaryScreen(controller: _controller, session: _session),
      ),
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
      home: FutureBuilder<void>(
        future: _startup,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }

          // Observa a sessão: é por aqui que o logout, e um 401 no meio do dia, levam o
          // motorista de volta ao login sem o app travar numa tela.
          return ListenableBuilder(
            listenable: _session,
            builder: (context, _) {
              if (!_session.isSignedIn) {
                return LoginScreen(controller: _session);
              }
              return HomeScreen(controller: _controller, session: _session);
            },
          );
        },
      ),
    );
  }
}
