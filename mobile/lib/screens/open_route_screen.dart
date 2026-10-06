import 'package:flutter/material.dart';

import '../services/route_controller.dart';
import '../theme/app_theme.dart';
import 'route_summary_screen.dart';

/// Entrada do app. O caminho normal é o deep link do WhatsApp; o campo de código é o
/// plano B, para quando o link não abre o app (aparelho sem o app instalado, link
/// copiado e colado, mensagem encaminhada).
class OpenRouteScreen extends StatefulWidget {
  const OpenRouteScreen({super.key, required this.controller, this.initialToken});

  final RouteController controller;
  final String? initialToken;

  @override
  State<OpenRouteScreen> createState() => _OpenRouteScreenState();
}

class _OpenRouteScreenState extends State<OpenRouteScreen> {
  late final TextEditingController _tokenField = TextEditingController(text: widget.initialToken ?? '');
  String? _savedToken;

  @override
  void initState() {
    super.initState();
    widget.controller.savedToken().then((token) {
      if (mounted) setState(() => _savedToken = token);
    });
  }

  @override
  void dispose() {
    _tokenField.dispose();
    super.dispose();
  }

  Future<void> _open(String token) async {
    final trimmed = token.trim();
    if (trimmed.isEmpty) return;

    await widget.controller.open(trimmed);
    if (!mounted) return;

    final error = widget.controller.error;
    if (error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error)));
      return;
    }

    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => RouteSummaryScreen(controller: widget.controller)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: ListenableBuilder(
        listenable: widget.controller,
        builder: (context, _) {
          return SafeArea(
            top: false,
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _header(context),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _hint(),
                        const SizedBox(height: 22),
                        const Text(
                          'Ou digite o código da rota',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w800, color: AppColors.gray900),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'O gestor encontra esse código na tela de acompanhamento da rota.',
                          style: TextStyle(fontSize: 12.5, color: AppColors.gray600, height: 1.5),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _tokenField,
                          autocorrect: false,
                          enableSuggestions: false,
                          textInputAction: TextInputAction.go,
                          onSubmitted: _open,
                          style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, letterSpacing: 0.5),
                          decoration: const InputDecoration(hintText: 'Cole o código aqui'),
                        ),
                        const SizedBox(height: 14),
                        FilledButton(
                          onPressed: widget.controller.isLoading ? null : () => _open(_tokenField.text),
                          child: widget.controller.isLoading
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white),
                                )
                              : const Text('Abrir rota'),
                        ),
                        if (_savedToken != null) ...[
                          const SizedBox(height: 24),
                          const Divider(),
                          const SizedBox(height: 16),
                          const Text(
                            'ÚLTIMA ROTA ABERTA',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.6,
                              color: AppColors.gray600,
                            ),
                          ),
                          const SizedBox(height: 10),
                          OutlinedButton.icon(
                            onPressed: () => _open(_savedToken!),
                            icon: const Icon(Icons.place_outlined, size: 18),
                            label: const Text('Continuar de onde parei'),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _header(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;
    return Container(
      width: double.infinity,
      color: AppColors.primary600,
      padding: EdgeInsets.fromLTRB(28, topPadding + 40, 28, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.18),
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(Icons.route_outlined, color: Colors.white, size: 28),
          ),
          const SizedBox(height: 14),
          const Text(
            'RouteWise',
            style: TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 6),
          const Text(
            'App do motorista — abra a rota que o gestor enviou',
            style: TextStyle(color: AppColors.primary200, fontSize: 14, fontWeight: FontWeight.w600, height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _hint() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: BoxDecoration(
        color: AppColors.primary50,
        border: Border.all(color: AppColors.primary200),
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.description_outlined, size: 18, color: AppColors.primary700),
          SizedBox(width: 11),
          Expanded(
            child: Text(
              'Normalmente é só tocar no link que chega pelo WhatsApp — ele abre o app já na rota do dia.',
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.primary700, height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}
