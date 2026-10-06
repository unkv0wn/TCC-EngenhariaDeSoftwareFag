import 'package:flutter/material.dart';

import 'screens/open_route_screen.dart';
import 'screens/route_summary_screen.dart';
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

  /// Token guardado da última abertura. Enquanto não resolve, a tela de abertura
  /// aparece sem o atalho — carregar antes só atrasaria o primeiro frame.
  late final Future<String?> _savedToken = _controller.savedToken();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'RouteWise — Motorista',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: FutureBuilder<String?>(
        future: _savedToken,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }

          final token = snapshot.data;
          if (token == null) {
            return OpenRouteScreen(controller: _controller);
          }

          // Já abriu uma rota antes: entra direto nela. Se o token tiver sido revogado,
          // o controller limpa e a tela cai pra abertura.
          return _ResumeRoute(controller: _controller, token: token);
        },
      ),
    );
  }
}

class _ResumeRoute extends StatefulWidget {
  const _ResumeRoute({required this.controller, required this.token});

  final RouteController controller;
  final String token;

  @override
  State<_ResumeRoute> createState() => _ResumeRouteState();
}

class _ResumeRouteState extends State<_ResumeRoute> {
  @override
  void initState() {
    super.initState();
    widget.controller.open(widget.token);
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
          return OpenRouteScreen(controller: widget.controller, initialToken: widget.token);
        }
        return RouteSummaryScreen(controller: widget.controller);
      },
    );
  }
}
