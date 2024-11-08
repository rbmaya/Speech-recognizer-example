package te.example.stt

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import te.example.stt.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    private var recognitionInProgress by mutableStateOf(false)
    private var recognitionResult by mutableStateOf("")

    val requestPermissionLauncher =
        registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                initSpeechRecognition()
            } else {
                showMessage("Insufficient permissions error")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestAudioPermissions()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            RecognitionResultTextField(recognitionResult)
                        }
                        LaunchButton(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp),
                            recognitionInProgress = recognitionInProgress
                        ) {
                            startSpeechRecognition()
                        }
                    }
                }
            }
        }
    }

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initSpeechRecognition()
            return
        }

        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun initSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showMessage("Speech recognition service IS NOT available!")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                recognitionInProgress = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                recognitionInProgress = false
            }

            override fun onError(error: Int) {
                recognitionInProgress = false

                ErrorMapper.mapRecognizerError(error)?.let {
                    showMessage(it)
                }
            }

            override fun onResults(results: Bundle?) {
                recognitionInProgress = false

                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                println("Results: ${data.orEmpty().joinToString("\n")}")
                recognitionResult = data.orEmpty().joinToString("\n")
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onSegmentResults(segmentResults: Bundle) {
                super.onSegmentResults(segmentResults)
            }

            override fun onEndOfSegmentedSession() {
                super.onEndOfSegmentedSession()
            }

            override fun onLanguageDetection(results: Bundle) {
                super.onLanguageDetection(results)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                println("Event: $eventType")
            }
        })
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startSpeechRecognition() {
        checkSupportedLanguages()

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, "ru-RU"
            )
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            speechRecognizer?.checkRecognitionSupport(recognizerIntent, Executors.newSingleThreadExecutor(),
//                object : RecognitionSupportCallback {
//                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
//                        println("Recognition support languages: ${recognitionSupport.supportedOnDeviceLanguages}")
//                        speechRecognizer?.startListening(recognizerIntent)
//                    }
//
//                    override fun onError(error: Int) {
//                        runOnUiThread {
//                            showMessage("Speech recognition service IS NOT available! Error: $error")
//                        }
//                    }
//                })
//        } else {
            speechRecognizer?.startListening(recognizerIntent)
        //}
    }

    private fun checkSupportedLanguages() {
        val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)

        sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (resultCode == RESULT_OK) {
                    val results = getResultExtras(true)

                    val prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
                    println("Pref language: $prefLang")
                    val allLangs =
                        results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                    println("All languages: $allLangs")
                }
            }
        }, null, RESULT_OK, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

@Composable
fun RecognitionResultTextField(
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxSize(),
        value = text,
        onValueChange = {},
        readOnly = true,
    )
}

@Composable
fun LaunchButton(
    modifier: Modifier = Modifier,
    recognitionInProgress: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
            .size(80.dp),
        onClick = { onClick() },
    ) {
        if (recognitionInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    RecognitionResultTextField("")
                }
                LaunchButton(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 24.dp),
                    recognitionInProgress = false,
                ) {

                }
            }
        }
    }
}