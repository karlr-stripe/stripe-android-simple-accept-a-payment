package com.example.stripepaymentintegration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // just launch the other activity immediately
        val intent = Intent(this, WebViewActivity::class.java).apply {

        }
        startActivity(intent)
    }
}