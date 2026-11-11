package cl.iqs.boilerreport

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

/**
 * BoilerReport: la app web completa va dentro del APK (carpeta assets/www),
 * así abre y funciona sin señal. Los datos quedan en el teléfono y se
 * sincronizan con el repositorio privado de GitHub cuando hay conexión.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val pickFiles =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val cb = fileCallback
            fileCallback = null
            if (cb != null) {
                val data = res.data
                var uris: Array<Uri>? = null
                if (res.resultCode == RESULT_OK && data != null) {
                    val clip = data.clipData
                    uris = if (clip != null) Array(clip.itemCount) { clip.getItemAt(it).uri }
                    else data.data?.let { arrayOf(it) }
                }
                cb.onReceiveValue(uris)
            }
        }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startListening() else js("window.__ndict && window.__ndict.error('not_granted')")
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.databaseEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.settings.mediaPlaybackRequiresUserGesture = false

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == "appassets.androidplatform.net") return false
                return try { startActivity(Intent(Intent.ACTION_VIEW, u)); true } catch (e: Exception) { true }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = params.createIntent()
                if (params.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    pickFiles.launch(intent); true
                } catch (e: Exception) {
                    fileCallback = null; false
                }
            }
        }

        web.addJavascriptInterface(Bridge(), "Android")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }

    /** Funciones que la página puede llamar como window.Android.xxx() */
    inner class Bridge {
        @JavascriptInterface
        fun saveFile(b64: String, name: String, mime: String): String {
            return try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BoilerReport")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return "error"
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Guardado en Descargas/BoilerReport", Toast.LENGTH_LONG).show()
                    if (mime.contains("word") || mime.contains("sheet") || mime.contains("pdf")) {
                        try {
                            startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(uri, mime)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
                        } catch (e: Exception) { /* sin app para abrirlo: queda en Descargas */ }
                    }
                }
                "ok"
            } catch (e: Exception) {
                "error"
            }
        }

        @JavascriptInterface
        fun hasDictation(): Boolean = SpeechRecognizer.isRecognitionAvailable(this@MainActivity)

        @JavascriptInterface
        fun startDictation() {
            runOnUiThread {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) startListening()
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        @JavascriptInterface
        fun stopDictation() {
            runOnUiThread {
                listening = false
                recognizer?.stopListening()
            }
        }
    }

    private fun startListening() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        js("window.__ndict && window.__ndict.state('listening')")
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (listening && (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ) {
                            listenOnce()
                        } else {
                            listening = false
                            js("window.__ndict && window.__ndict.state('idle')")
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!t.isNullOrBlank()) {
                            js("window.__ndict && window.__ndict.final(" + JSONObject.quote(" $t") + ")")
                        }
                        if (listening) listenOnce()
                        else js("window.__ndict && window.__ndict.state('idle')")
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                            ?: return
                        js("window.__ndict && window.__ndict.partial(" + JSONObject.quote(t) + ")")
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        listening = true
        listenOnce()
    }

    private fun listenOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
    }
}
