import 'dart:async';

import 'package:app_links/app_links.dart';

import '../models/access_code.dart';

/// Escuta `routewise://rota/<codigo>` — o mesmo conteúdo do QR Code.
///
/// Dois caminhos chegam aqui: o app estava fechado e o link o abriu (`getInitialLink`),
/// ou já estava aberto e o link veio por cima (`uriLinkStream`). O segundo é o caso de o
/// motorista tocar no link do WhatsApp com o app em segundo plano.
///
/// Devolve sempre o código já normalizado, nunca a URI crua: quem consome não deveria
/// precisar saber o formato do link.
class DeepLinkService {
  DeepLinkService({AppLinks? appLinks}) : _appLinks = appLinks ?? AppLinks();

  final AppLinks _appLinks;
  StreamSubscription<Uri>? _subscription;

  /// Código do link que abriu o app, quando houve um.
  Future<String?> initialCode() async {
    try {
      final uri = await _appLinks.getInitialLink();
      return AccessCode.normalize(uri?.toString());
    } on Exception {
      // Link malformado não pode impedir o app de abrir: a tela de abertura continua
      // oferecendo o QR e o campo de código.
      return null;
    }
  }

  /// Links recebidos com o app já aberto.
  void listen(void Function(String code) onCode) {
    _subscription ??= _appLinks.uriLinkStream.listen((uri) {
      final code = AccessCode.normalize(uri.toString());
      if (code != null) onCode(code);
    });
  }

  void dispose() {
    _subscription?.cancel();
    _subscription = null;
  }
}
