import 'package:flutter/material.dart';

import '../models/access_code.dart';
import '../widgets/access_code_input_formatter.dart';
import '../services/route_controller.dart';
import '../theme/app_theme.dart';
import 'route_summary_screen.dart';
import 'scan_code_screen.dart';

/// Entrada do app. O caminho normal é o deep link do WhatsApp; o campo de código é o
/// plano B, para quando o link não abre o app (aparelho sem o app instalado, link
/// copiado e colado, mensagem encaminhada).
class OpenRouteScreen extends StatefulWidget {
  const OpenRouteScreen({super.key, required this.controller, this.initialCode});

  final RouteController controller;
  final String? initialCode;

  @override
  State<OpenRouteScreen> createState() => _OpenRouteScreenState();
}

class _OpenRouteScreenState extends State<OpenRouteScreen> {
  late final TextEditingController _codeField = TextEditingController(
    text: widget.initialCode != null ? AccessCode.format(widget.initialCode!) : '',
  );
  String? _savedCode;

  @override
  void initState() {
    super.initState();
    widget.controller.savedCode().then((code) {
      if (mounted) setState(() => _savedCode = code);
    });
  }

  @override
  void dispose() {
    _codeField.dispose();
    super.dispose();
  }

  Future<void> _scan() async {
    final scanned = await ScanCodeScreen.open(context);
    if (scanned == null || !mounted) return;

    _codeField.text = AccessCode.format(scanned);
    await _open(scanned);
  }

  Future<void> _open(String raw) async {
    // Normaliza antes de chamar: dá para dizer "código incompleto" sem gastar uma
    // requisição, o que importa quando o sinal é ruim.
    final code = AccessCode.normalize(raw);
    if (code == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('O código tem 12 caracteres. Confira o que foi digitado.')),
      );
      return;
    }

    await widget.controller.open(code);
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
                        // O caminho principal é a câmera: no pátio, ao lado do computador
                        // do gestor, ler o QR é mais rápido e não erra. Digitar é o plano B.
                        FilledButton.icon(
                          onPressed: widget.controller.isLoading ? null : _scan,
                          icon: const Icon(Icons.qr_code_scanner, size: 20),
                          label: const Text('Ler QR Code'),
                        ),
                        const SizedBox(height: 18),
                        const Row(
                          children: [
                            Expanded(child: Divider()),
                            Padding(
                              padding: EdgeInsets.symmetric(horizontal: 12),
                              child: Text(
                                'ou',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.gray400,
                                ),
                              ),
                            ),
                            Expanded(child: Divider()),
                          ],
                        ),
                        const SizedBox(height: 18),
                        const Text(
                          'Digite o código da rota',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w800, color: AppColors.gray900),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'O gestor vê esse código na mesma tela em que mostra o QR.',
                          style: TextStyle(fontSize: 12.5, color: AppColors.gray600, height: 1.5),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _codeField,
                          autocorrect: false,
                          enableSuggestions: false,
                          textCapitalization: TextCapitalization.characters,
                          textInputAction: TextInputAction.go,
                          inputFormatters: [AccessCodeInputFormatter()],
                          onSubmitted: _open,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.5,
                          ),
                          decoration: const InputDecoration(hintText: 'XXXX-XXXX-XXXX'),
                        ),
                        const SizedBox(height: 14),
                        FilledButton(
                          onPressed: widget.controller.isLoading ? null : () => _open(_codeField.text),
                          child: widget.controller.isLoading
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white),
                                )
                              : const Text('Abrir rota'),
                        ),
                        if (_savedCode != null) ...[
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
                            onPressed: () => _open(_savedCode!),
                            icon: const Icon(Icons.place_outlined, size: 18),
                            label: const Text('Continuar de onde parei'),
                          ),
                        ],
                        const SizedBox(height: 24),
                        _hint(),
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
              'Se o gestor mandou o link pelo WhatsApp, basta tocar nele — o app abre direto na rota do dia.',
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.primary700, height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}
