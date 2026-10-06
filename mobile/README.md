# RouteWise — App do Motorista (Flutter)

App de campo: o motorista abre a rota do dia por um link que o gestor manda e registra o
que acontece na rua — chegada, entrega, ocorrência. Sem login.

Design e decisões: [`docs/superpowers/specs/2026-10-05-driver-mobile-app-design.md`](../docs/superpowers/specs/2026-10-05-driver-mobile-app-design.md).

## Pré-requisitos

- Flutter SDK 3.24+ (Dart 3.5+)
- Android Studio com um emulador, ou um aparelho Android com depuração USB
- O backend do RouteWise rodando (`docker compose up` na raiz do projeto)

## Rodar

```bash
cd mobile
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080
```

`10.0.2.2` é como o emulador do Android enxerga o `localhost` da máquina. Em aparelho
físico, use o IP da máquina na rede local (`http://192.168.x.x:8080`) — e lembre que o
backend precisa estar aceitando conexões externas, não só de `localhost`.

Sem `--dart-define`, o app assume `http://10.0.2.2:8080`.

## Como obter um token para testar

O token nasce com a rota. Depois de gerar e salvar uma rota no painel web:

```bash
curl -s http://localhost:8080/api/routes | grep -o '"accessToken":"[^"]*"'
```

Cole o valor na tela de abertura do app.

## Estrutura

```
lib/
  main.dart                      # entrada; retoma a última rota aberta
  theme/app_theme.dart           # tokens de FrontEnd-Next/app/globals.css em ThemeData
  models/route_models.dart       # espelham os DTOs de /api/mobile/**
  services/
    api_client.dart              # as três chamadas da superfície mobile
    event_queue.dart             # fila offline persistida
    route_controller.dart        # junta servidor + fila; é a fonte da verdade das telas
  screens/
    open_route_screen.dart       # abertura por código (o deep link entra aqui depois)
    route_summary_screen.dart    # rota do dia, progresso e lista de paradas
    stop_detail_screen.dart      # onde o motorista passa o dia
  widgets/
    event_form_sheet.dart        # registro de ocorrência
    sync_badge.dart              # selo de eventos na fila
```

## O que ainda não existe

Em ordem, seguindo o fim da spec:

- **Deep link** (`routewise://rota/<token>`) — hoje só o código colado. Falta configurar
  o intent filter no `AndroidManifest.xml` e tratar o link de entrada.
- **Mapa da rota** — a geometria já vem no `result`, falta desenhar.
- **Anexo de canhoto/foto** — depende de decidir onde armazenar arquivo, igual na tela do
  gestor.
- **Projeto Android/iOS gerado** — este diretório tem só o código Dart. Rode
  `flutter create . --platforms=android` dentro de `mobile/` para gerar `android/`,
  `ios/` e os arquivos de plataforma antes do primeiro `flutter run`.

## Uma regra que atravessa o app

**Registrar nunca falha por falta de rede.** O evento entra na fila local, a tela já
reflete o efeito, e a sincronização acontece quando a conexão voltar. Cada evento leva um
`clientEventId` gerado no aparelho: se a resposta se perder num túnel e o app reenviar, o
backend reconhece e não duplica a entrega.
