package com.routewise.routewise_driver

import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Expõe ao Dart o identificador deste aparelho.
 *
 * Por que não o IMEI: desde o Android 10 (API 29) ler o IMEI exige
 * READ_PRIVILEGED_PHONE_STATE, permissão de assinatura concedida só a app de sistema ou
 * do fabricante — app de terceiro recebe SecurityException, e não existe permissão que o
 * usuário possa conceder para destravar isso. A política do Google Play também proíbe
 * usar IMEI como identificador persistente.
 *
 * O substituto é o ANDROID_ID: escopado por chave de assinatura do app + aparelho +
 * usuário, estável entre reinstalações do mesmo APK, e muda só em formatação de fábrica.
 * É isso que o gestor cadastra como "o celular do Carlos".
 *
 * É canal nativo, e não plugin: o device_info_plus deixou de expor o ANDROID_ID na v4, e
 * trazer um pacote inteiro para uma linha de leitura não se paga.
 */
class MainActivity : FlutterActivity() {

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)

    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
      .setMethodCallHandler { call, result ->
        when (call.method) {
          // Pode voltar nulo: o contrato do Android não garante valor, e em ROM
          // modificada acontece. Quem chama no Dart trata esse caso.
          "androidId" -> result.success(
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
          )
          else -> result.notImplemented()
        }
      }
  }

  private companion object {
    const val CHANNEL = "routewise/device"
  }
}
