import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/device_identity.dart';
import '../services/session_controller.dart';
import '../theme/app_theme.dart';

/// Entrada do app. Usuário, senha, e o identificador deste aparelho à mostra.
///
/// O identificador aparece aqui porque o gestor precisa dele para liberar o acesso, e o
/// Android não o exibe em tela nenhuma do sistema — sem isto, o cadastro do aparelho
/// ficaria impossível de completar. Fica visível antes do login de propósito: o motorista
/// que ainda não tem acesso é justamente quem precisa enviá-lo.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.controller});

  final SessionController controller;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _username = TextEditingController();
  final _password = TextEditingController();
  bool _showPassword = false;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final username = _username.text.trim();
    final password = _password.text;

    if (username.isEmpty || password.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Preencha usuário e senha.')),
      );
      return;
    }

    // O teclado some antes de a resposta chegar: o erro aparece embaixo do formulário, e
    // com o teclado aberto ele nasce fora da vista.
    FocusScope.of(context).unfocus();
    await widget.controller.signIn(username: username, password: password);
  }

  Future<void> _copyDeviceId(String id) async {
    await Clipboard.setData(ClipboardData(text: id));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Identificador copiado. Envie ao gestor.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.controller,
      builder: (context, _) {
        final controller = widget.controller;

        return Scaffold(
          body: SafeArea(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
              children: [
                const SizedBox(height: 8),
                _header(),
                const SizedBox(height: 32),
                TextField(
                  controller: _username,
                  autocorrect: false,
                  enableSuggestions: false,
                  textCapitalization: TextCapitalization.none,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Usuário',
                    prefixIcon: Icon(Icons.person_outline),
                  ),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _password,
                  obscureText: !_showPassword,
                  autocorrect: false,
                  enableSuggestions: false,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _submit(),
                  decoration: InputDecoration(
                    labelText: 'Senha',
                    prefixIcon: const Icon(Icons.lock_outline),
                    // Senha digitada de pé, com uma mão, às vezes de luva: sem poder
                    // conferir o que foi digitado, o erro só aparece como login recusado.
                    suffixIcon: IconButton(
                      onPressed: () => setState(() => _showPassword = !_showPassword),
                      icon: Icon(_showPassword ? Icons.visibility_off : Icons.visibility),
                      tooltip: _showPassword ? 'Esconder senha' : 'Mostrar senha',
                    ),
                  ),
                ),
                if (controller.error != null) ...[
                  const SizedBox(height: 16),
                  _errorBanner(controller.error!),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: controller.isLoading ? null : _submit,
                  child: controller.isLoading
                      ? const SizedBox(
                          height: 22,
                          width: 22,
                          child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white),
                        )
                      : const Text('Entrar'),
                ),
                const SizedBox(height: 32),
                _deviceCard(controller.deviceId),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _header() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          height: 56,
          width: 56,
          decoration: BoxDecoration(
            color: AppColors.primary600,
            borderRadius: BorderRadius.circular(16),
          ),
          child: const Icon(Icons.local_shipping_outlined, color: Colors.white, size: 30),
        ),
        const SizedBox(height: 20),
        const Text(
          'RouteWise',
          style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900, color: AppColors.gray900),
        ),
        const SizedBox(height: 4),
        const Text(
          'Entre com o usuário que o gestor cadastrou para você.',
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.gray500),
        ),
      ],
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
        ],
      ),
    );
  }

  /// O identificador do aparelho, para enviar ao gestor.
  ///
  /// Em grupos de quatro e com botão de copiar porque o caminho real é o motorista mandar
  /// isso por mensagem — ditar 16 dígitos hexadecimais ao telefone termina em aparelho
  /// cadastrado errado, que falha no login sem dizer por quê.
  Widget _deviceCard(String? deviceId) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.gray100,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.gray200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.smartphone, size: 18, color: AppColors.gray500),
              SizedBox(width: 8),
              Text(
                'Identificador deste aparelho',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: AppColors.gray700),
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (deviceId == null)
            const Text(
              'Lendo...',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: AppColors.gray400),
            )
          else
            SelectableText(
              DeviceIdentity.format(deviceId),
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: AppColors.gray900,
                fontFamily: 'monospace',
                letterSpacing: 0.5,
              ),
            ),
          const SizedBox(height: 10),
          const Text(
            'O gestor precisa cadastrar este código para liberar o seu acesso neste celular.',
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w600,
              color: AppColors.gray500,
              height: 1.35,
            ),
          ),
          if (deviceId != null) ...[
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: () => _copyDeviceId(deviceId),
              icon: const Icon(Icons.copy, size: 18),
              label: const Text('Copiar identificador'),
            ),
          ],
        ],
      ),
    );
  }
}
