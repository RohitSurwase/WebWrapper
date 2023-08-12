package com.rohitrss.wrapper

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.MimeTypeMap.getFileExtensionFromUrl
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var mainWebView: WebView
    private lateinit var popupContainer: FrameLayout
    private lateinit var popupWebView: WebView
    private lateinit var progressBar: ProgressBar

    private val baseWebChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val newUrl = view?.hitTestResult?.extra
            if (newUrl != null) {
                popupContainer.isVisible = true
                openInPopup(newUrl)
            }
            return false
        }

        override fun onCloseWindow(window: WebView?) {
            super.onCloseWindow(window)
            popupContainer.isVisible = false
        }
    }

    private val baseWebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.isVisible = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.isVisible = false
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return false
        }
    }

    private val popupWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainWebView = findViewById(R.id.mainWebView)
        progressBar = findViewById(R.id.progress)

        findViewById<ImageView>(R.id.home).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mainWebView.loadUrl(getString(R.string.default_url))
        }
        findViewById<ImageView>(R.id.back).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onBackPressedDispatcher.onBackPressed()
        }
        findViewById<ImageView>(R.id.forward).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mainWebView.canGoForward()) {
                mainWebView.goForward()
            } else {
                Toast.makeText(this, "Navigation not available", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageView>(R.id.refresh).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mainWebView.reload()
        }
        findViewById<ImageView>(R.id.stop).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mainWebView.stopLoading()
            progressBar.isVisible = false
        }
        findViewById<ImageView>(R.id.share).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mainWebView.url.isNullOrEmpty().not() || URLUtil.isValidUrl(mainWebView.url)) {
                shareUrl(mainWebView.url!!)
            } else {
                Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.exit).setOnClickListener {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showExitAlert()
        }

        // Configure popupWebView
        popupContainer = findViewById(R.id.popupContainer)
        popupWebView = findViewById(R.id.popupWebView)
        findViewById<ImageView>(R.id.popupClose).setOnClickListener {
            popupContainer.isVisible = false
        }
        findViewById<ImageView>(R.id.popupShareUrl).setOnClickListener {
            if (popupWebView.url != null) {
                shareUrl(popupWebView.url!!)
            }
        }

        if (savedInstanceState == null) {
            mainWebView.apply {
                settings.apply {
                    webViewClient = baseWebViewClient
                    webChromeClient = baseWebChromeClient
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    domStorageEnabled = true
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    textZoom = 100
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        CookieManager.getInstance().setAcceptThirdPartyCookies(mainWebView, false)
                    }
                }
                registerForContextMenu(this)
                setDownloadListener { url, _, _, _, _ ->
                    downloadFile(url)
                }
            }
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (popupContainer.isVisible) {
                        Toast.makeText(this@MainActivity, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                        return
                    }
                    with(mainWebView) {
                        if (canGoBack()) {
                            goBack()
                        } else {
                            showExitAlert()
                        }
                    }
                }
            })
            mainWebView.loadUrl(getString(R.string.default_url))

            // Configure popupWebView
            popupWebView.apply {
                settings.apply {
                    webViewClient = popupWebViewClient
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    domStorageEnabled = true
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    textZoom = 100
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, false)
                    }
                }
                registerForContextMenu(this)
                setDownloadListener { url, _, _, _, _ ->
                    downloadFile(url)
                }
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = if (popupContainer.isVisible) popupWebView else mainWebView
        var targetUrl = webView.hitTestResult.extra

        if (targetUrl.isNullOrEmpty()) {
            return
        }
        when (webView.hitTestResult.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                if (webView.hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    val handlerThread = HandlerThread("TempHandlerThread").apply {
                        start()
                    }
                    val message = Handler(handlerThread.looper).obtainMessage()
                    webView.requestFocusNodeHref(message)
                    targetUrl = message.data.get("url") as? String
                    if (targetUrl.isNullOrEmpty()) {
                        return
                    }
                }
                if (URLUtil.isValidUrl(targetUrl)) {
                    if (URLUtil.isNetworkUrl(targetUrl) && popupContainer.isVisible.not()) {
                        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Open in Popup Window")
                            .setOnMenuItemClickListener {
                                openInPopup(targetUrl)
                                true
                            }
                    }
                    menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Open in Browser")
                        .setOnMenuItemClickListener {
                            shareUrl(targetUrl)
                            true
                        }
                }
                menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Share")
                    .setOnMenuItemClickListener {
                        shareUrl(targetUrl)
                        true
                    }
            }

            WebView.HitTestResult.IMAGE_TYPE -> {
                menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Save Image")
                    .setOnMenuItemClickListener {
                        downloadFile(targetUrl)
                        true
                    }
                menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Share")
                    .setOnMenuItemClickListener {
                        shareUrl(targetUrl)
                        true
                    }
            }

            WebView.HitTestResult.EDIT_TEXT_TYPE -> {
                val clipboard =
                    v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Paste")
                            .setOnMenuItemClickListener {
                                val textToPaste = clipData.getItemAt(0).text.toString()
                                webView.evaluateJavascript(
                                    "document.execCommand('insertText', false, '$textToPaste')",
                                    null
                                )
                                true
                            }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (popupContainer.isVisible) {
                Toast.makeText(this, "Not available on Popup Window", Toast.LENGTH_SHORT).show()
                return super.onKeyDown(keyCode, event)
            }
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mainWebView.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainWebView.saveState(outState)
    }

    private fun downloadFile(fileUrl: String?) {
        if (fileUrl == null || URLUtil.isValidUrl(fileUrl).not()) {
            return
        }
        val request = DownloadManager.Request(Uri.parse(fileUrl))
        var fileExtension = getFileExtensionFromUrl(fileUrl)
        if (fileExtension.isNullOrEmpty()) {
            val tempUrl: String = fileUrl.substring(fileUrl.lastIndexOf('.'))
            fileExtension = getFileExtensionFromUrl(tempUrl)
        }
        val fileName = "${getString(R.string.app_name)}_${
            UUID.randomUUID().toString().substring(0, 10)
        }.$fileExtension"
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        mimeType?.let {
            request.setMimeType(it)
        }
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        val snackbar = Snackbar.make(
            findViewById(R.id.rootLayout),
            "File saved to Downloads directory.",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("OK") {
            snackbar.dismiss()
        }.show()
    }

    private fun openInPopup(url: String) {
        if (URLUtil.isNetworkUrl(url)) {
            popupContainer.isVisible = true
            popupWebView.loadUrl("about:blank")
            popupWebView.loadUrl(url)
        } else {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            popupContainer.isVisible = false
        }
    }

    private fun shareUrl(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        if (shareIntent.resolveActivity(packageManager) != null) {
            val chooserIntent = Intent.createChooser(shareIntent, "Open with")
            if (chooserIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
            }
        }
    }

    private fun showExitAlert() {
        AlertDialog.Builder(this, R.style.CustomDialogTheme).apply {
            setMessage("Are you sure you want to exit?")
            setPositiveButton("Yes") { _, _ -> finish() }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        }.show()
    }
}