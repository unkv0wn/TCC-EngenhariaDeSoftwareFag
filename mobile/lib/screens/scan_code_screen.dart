import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../models/access_code.dart';
import '../theme/app_theme.dart';

/// Leitor do QR Code que o gestor mostra na tela.
///
/// Devolve o código normalizado via `Navigator.pop`, ou `null` se o motorista desistir.
/// Não chama o backend: quem valida se a rota existe é a tela que abriu esta.
class ScanCodeScreen extends StatefulWidget {
  const ScanCodeScreen({super.key});

  static Future<String?> open(BuildContext context) {
    return Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const ScanCodeScreen()),
    );
  }

  @override
  State<ScanCodeScreen> createState() => _ScanCodeScreenState();
}

class _ScanCodeScreenState extends State<ScanCodeScreen> {
  final _controller = MobileScannerController(formats: const [BarcodeFormat.qrCode]);

  /// A câmera dispara o callback muitas vezes por segundo enquanto o QR estiver no quadro.
  /// Sem esta trava, o primeiro código válido empilharia várias telas de rota.
  bool _handled = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_handled) return;

    for (final barcode in capture.barcodes) {
      final code = AccessCode.normalize(barcode.rawValue);
      if (code != null) {
        _handled = true;
        Navigator.of(context).pop(code);
        return;
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Ler QR Code'),
        backgroundColor: Colors.black,
        actions: [
          IconButton(
            onPressed: () => _controller.toggleTorch(),
            icon: const Icon(Icons.flashlight_on_outlined),
            tooltip: 'Lanterna',
          ),
        ],
      ),
      body: Stack(
        fit: StackFit.expand,
        children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error) => _cameraError(error),
          ),
          _aim(),
          Positioned(
            left: 24,
            right: 24,
            bottom: 48,
            child: Text(
              'Aponte para o QR Code na tela do computador',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white,
                fontSize: 14,
                fontWeight: FontWeight.w700,
                shadows: [Shadow(color: Colors.black.withValues(alpha: 0.6), blurRadius: 6)],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Moldura de mira. Sem ela o motorista não sabe onde posicionar o celular.
  Widget _aim() {
    return Center(
      child: Container(
        width: 240,
        height: 240,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.white, width: 3),
          borderRadius: BorderRadius.circular(24),
        ),
      ),
    );
  }

  /// Câmera negada ou indisponível: o caminho digitado continua lá, então a tela explica
  /// e manda voltar em vez de deixar um retângulo preto sem resposta.
  Widget _cameraError(MobileScannerException error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.no_photography_outlined, color: Colors.white54, size: 40),
            const SizedBox(height: 14),
            const Text(
              'Não foi possível abrir a câmera',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            const Text(
              'Verifique a permissão de câmera nas configurações do aparelho, '
              'ou volte e digite o código da rota.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white70, fontSize: 13, height: 1.5),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(),
              style: FilledButton.styleFrom(backgroundColor: AppColors.primary600),
              child: const Text('Digitar o código'),
            ),
          ],
        ),
      ),
    );
  }
}
