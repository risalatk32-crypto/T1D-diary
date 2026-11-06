package com.example

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        var webViewInstance by remember { mutableStateOf<WebView?>(null) }

        BackHandler(enabled = webViewInstance?.canGoBack() == true) {
          webViewInstance?.goBack()
        }

        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          SugarDiaryWebView(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
            onWebViewCreated = { webViewInstance = it }
          )
        }
      }
    }
  }

  inner class WebAppBridge(private val context: Context, private val webView: WebView) {

    @JavascriptInterface
    fun shareText(text: String) {
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "दैनिक ब्लड शुगर एवं आहार डायरी (T1D Log)")
        putExtra(Intent.EXTRA_TEXT, text)
      }
      val chooser = Intent.createChooser(intent, "डॉक्टर रिपोर्ट शेयर करें (Share Report)")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    }

    @JavascriptInterface
    fun shareOnWhatsApp(text: String) {
      val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, text)
      }
      try {
        context.startActivity(whatsappIntent)
      } catch (e: Exception) {
        // Fallback to general share dialog if WhatsApp package isn't installed
        val generalIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, "दैनिक ब्लड शुगर डायरी रिपोर्ट")
          putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(generalIntent, "शेयर करें (WhatsApp उपलब्ध नहीं है)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
      }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("Sugar Diary Report", text)
      clipboard.setPrimaryClip(clip)
      runOnUiThread {
        Toast.makeText(context, "📋 डॉक्टर सारांश क्लिपबोर्ड पर कॉपी किया गया!", Toast.LENGTH_SHORT).show()
      }
    }

    @JavascriptInterface
    fun printPage() {
      runOnUiThread {
        try {
          val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
          val printAdapter = webView.createPrintDocumentAdapter("Sugar_Diary_Report")
          printManager?.print(
            "Sugar_Diary_Report",
            printAdapter,
            PrintAttributes.Builder().build()
          )
        } catch (e: Exception) {
          Toast.makeText(context, "प्रिंट सेवा अनुपलब्ध है", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Composable
  private fun SugarDiaryWebView(
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit
  ) {
    AndroidView(
      modifier = modifier,
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            textZoom = 100
          }

          webChromeClient = WebChromeClient()
          webViewClient = WebViewClient()

          addJavascriptInterface(WebAppBridge(ctx, this), "AndroidBridge")

          loadUrl("file:///android_asset/index.html")
          onWebViewCreated(this)
        }
      }
    )
  }
}
