package com.nanolm.control

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.time.Instant

class MainActivity : Activity() {
 private lateinit var input: EditText
 private lateinit var status: TextView
 private var recognizer: SpeechRecognizer? = null
 private var pending: Command? = null
 private val logFile by lazy { File(filesDir, "audit.log") }
 private fun log(event: String) {
  // Avoid storing speech, searches or typed content in the audit log.
  logFile.appendText("${Instant.now()} | $event\n")
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 28, 28, 28) }
  val heading = TextView(this).apply { text = "NanoLM Control 0.1 — execução supervisionada"; textSize = 20f }
  input = EditText(this).apply { hint = "Ex.: abre YouTube, pesquisa motores elétricos, voltar"; minLines = 2 }
  status = TextView(this).apply { text = "Pronto. Nada é executado sem confirmação."; textSize = 16f }
  input.addTextChangedListener(object : android.text.TextWatcher {
   override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
   override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    pending = null
    ControlAccessibilityService.instance?.cancel()
    status.text = "Toque em Interpretar comando para revisar o texto."
   }
   override fun afterTextChanged(s: android.text.Editable?) {}
  })
  fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
  layout.addView(heading); layout.addView(input)
  button("Falar (toque para ativar)") { listen() }
  button("Interpretar comando") { prepare(input.text.toString()) }
  button("Confirmar e executar") { val c = pending; pending = null; if(c != null) execute(c) else status.text = "Nenhuma ação pendente." }
  button("Cancelar ação") { ControlAccessibilityService.instance?.cancel(); pending = null; status.text = "Ação cancelada."; log("CANCELLED") }
  button("Ativar serviço de acessibilidade") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
  layout.addView(status)
  setContentView(ScrollView(this).apply { addView(layout) })
 }
 private fun prepare(text: String) {
  ControlAccessibilityService.instance?.cancel()
  pending = CommandParser.parse(text)
  status.text = "Ação proposta: ${CommandParser.describe(pending!!)}\nConfira antes de executar."
  log("PROPOSED ${pending!!::class.simpleName}")
  if(pending is Command.Unknown) { pending = null; status.text = "Comando não reconhecido. Nenhuma ação será executada." }
 }
 private fun execute(c: Command) {
  val success = try {
   when(c) {
    is Command.OpenApp -> openApp(c.name)
    is Command.Search -> { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(c.query)))); true }
    is Command.TypeText, Command.Back -> return armExternal(c)
    Command.Home -> ControlAccessibilityService.instance?.home() ?: false
    is Command.Unknown -> false
   }
  } catch(e: Exception) { false }
  status.text = if(success) "Ação solicitada ao Android. Verifique o resultado na tela." else "Falhou ou falta permissão/serviço."
  log("EXECUTED ${c::class.simpleName} requested=$success")
 }
 private fun openApp(name: String): Boolean {
  // Only known app names are accepted: never select a package by fuzzy similarity.
  val packages = mapOf("youtube" to "com.google.android.youtube", "whatsapp" to "com.whatsapp", "chrome" to "com.android.chrome", "navegador" to "com.android.chrome", "configuracoes" to "com.android.settings", "configurações" to "com.android.settings")
  val pkg = packages[name.lowercase().trim()] ?: return false
  val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return false
  startActivity(intent); return true
 }
 private fun armExternal(command: Command) {
  val service = ControlAccessibilityService.instance
  val armed = service?.arm(command) == true
  log("ARMED ${command::class.simpleName} accepted=$armed")
  status.text = if (armed) "Abra o app de destino. Confira a tela e toque no botão flutuante em até 45 segundos." else "Ative o serviço de acessibilidade e tente novamente."
 }
 private fun listen() {
  pending = null
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10); status.text = "Permita o microfone e toque novamente."; return }

  if(android.os.Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) { status.text = "Reconhecimento estritamente local indisponível neste aparelho. Use comandos por texto."; return }
  recognizer?.destroy()
  recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
  recognizer?.setRecognitionListener(object : RecognitionListener {
   override fun onReadyForSpeech(params: Bundle?) { status.text = "Ouvindo..." }
   override fun onBeginningOfSpeech() {}
   override fun onRmsChanged(rmsdB: Float) {}
   override fun onBufferReceived(buffer: ByteArray?) {}
   override fun onEndOfSpeech() {}
   override fun onError(error: Int) { status.text = "Erro no reconhecimento local: $error" }
   override fun onResults(results: Bundle?) {
    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
    input.setText(text); prepare(text)
   }
   override fun onPartialResults(partialResults: Bundle?) {}
   override fun onEvent(eventType: Int, params: Bundle?) {}
  })
  recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR"); putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) })
 }
 override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
}
