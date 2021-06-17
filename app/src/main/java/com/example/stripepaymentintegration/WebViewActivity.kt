package com.example.stripepaymentintegration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build

import android.util.Log
import android.webkit.*
import android.webkit.WebView
import android.webkit.WebChromeClient

import android.app.Dialog
import android.os.Message
import android.view.WindowManager
import android.content.pm.PackageManager


class WebViewActivity : AppCompatActivity() {

    // URL to load in the WebView
    private val URL = "https://connect.stripe.com/express/oauth/authorize?&redirect_uri=http%3A%2F%2Fkarlr-stripe.ngrok.io%2Fconnect%2Fcallback&client_id=ca_CrMA5imLyc6DL1vZYIOO8YpiHQmqa8QE"

    // variables relating to managing a file chooser dialog
    private val OPEN_FILE_CHOOSER = 7
    private var fileUpload:ValueCallback<Array<Uri>>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view) // define this in your res/layout directory

        // allows debugging from desktop Chrome(devtools->More tools->Remote devices)
        WebView.setWebContentsDebuggingEnabled(true);

        var webView:WebView = findViewById(R.id.webview)

        // set various options that allow for better compatibility with the pages in the WebView
        webView.getSettings().setJavaScriptEnabled(true)
        webView.getSettings().setAllowContentAccess(true)
        webView.getSettings().setAllowFileAccess(true)
        webView.getSettings().setDomStorageEnabled(true)
        // these are needed to support pages that open new tabs(like certain 3D Secure pages)
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // decide how to handle URLs. You might also want to check the scheme
                // of the URL and create Intents to open other apps
                //  https://stackoverflow.com/questions/41693263/android-webview-err-unknown-url-scheme/53059413#53059413
                // we attempt to do that here
                if (url == null || url.startsWith("http://") || url.startsWith("https://")) {
                    // allow the WebView to handle http links itself
                    return false
                }
                // for other schemes, try launching other apps that might handle the scheme
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    // check for apps installed that can handle this Intent
                    val packageManager = applicationContext.packageManager
                    val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    if(info !== null){
                        // if installed, open the app/activity
                        view.context.startActivity(intent)
                    }else{
                        // otherwise open a fallback
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        fallbackUrl?.let { view.loadUrl(fallbackUrl) } ?:view.loadUrl(url)
                    }
                    view.context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    return true
                }

            }
        }

        webView.webChromeClient = object: WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest) {
                // if the page is trying to e.g. access files, let it.
                // in reality you might want to display something to the user.
                Log.d("perms" , request.resources.toString())
                request.grant(request.resources)
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                // if the page tries to open a new window/tab, create a new WebView and load the page there
                val newWebView = WebView(this@WebViewActivity)
                val webSettings = newWebView.settings
                webSettings.javaScriptEnabled = true // and others as needed, like above..

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        // as above..
                        view.loadUrl(url)
                        return true
                    }
                }

                // open a new dialog and put a new WebView in it
                val dialog = Dialog(this@WebViewActivity)
                dialog.setContentView(newWebView)
                dialog.show()

                // match the dimensions to the parent activity
                val window = dialog.window
                window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dialog.dismiss()
                    }
                }

                (resultMsg.obj as WebView.WebViewTransport).webView = newWebView
                resultMsg.sendToTarget()
                return true
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                // when the page tries to open a browser file chooser, we have to create an Intent to open and manage an Android system chooser
                Log.d("file_chooser" , "cb called")
                fileUpload = filePathCallback
                val intent = fileChooserParams.createIntent()
                intent.type = "*/*"
                startActivityForResult(intent, OPEN_FILE_CHOOSER)
                return true
            }
        }

        webView.loadUrl(URL)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // handle getting the response from the Android file chooser back to the WebView page
        Log.d("file_chooser" , "activity result called")
        if (requestCode == OPEN_FILE_CHOOSER && fileUpload != null) {
            Log.d("file_chooser" , "handling called")
            fileUpload!!.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data!!))
        }
    }
}