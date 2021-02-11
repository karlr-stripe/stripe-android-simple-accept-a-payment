package com.example.stripepaymentintegration

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.GsonBuilder
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardInputWidget
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    // these need to be set via gradle.properties
    private var backendUrl = BuildConfig.BACKEND_URL
    private var publishableKey = BuildConfig.PUBLISHABLE_KEY

    private lateinit var paymentIntentClientSecret: String
    private lateinit var stripe: Stripe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PaymentConfiguration.init(
            applicationContext,
            publishableKey
        )
        startCheckout()
    }

    private fun startCheckout() {
        // Request a PaymentIntent from your server and store its client secret in paymentIntentClientSecret

        // from https://developer.android.com/training/volley/simple :

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = backendUrl

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                { response ->
                    paymentIntentClientSecret = response
                    Log.d("startCheckout", "got PaymentIntent secret $paymentIntentClientSecret")
                    // Hook up the pay button to the card widget and stripe instance
                    val payButton: Button = findViewById(R.id.payButton)
                    val cardInputWidget:CardInputWidget = findViewById(R.id.cardInputWidget)
                    payButton.setOnClickListener {
                                            val params = cardInputWidget.paymentMethodCreateParams
                                            if (params != null) {
                                                val confirmParams = ConfirmPaymentIntentParams
                                                        .createWithPaymentMethodCreateParams(params, paymentIntentClientSecret)
                                                stripe = Stripe(applicationContext, PaymentConfiguration.getInstance(applicationContext).publishableKey)
                                                stripe.confirmPayment(this, confirmParams)
                                            }
                                        }
                },
                { error -> Log.e("startCheckout", "error loading PaymentIntent!") })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val weakActivity = WeakReference<Activity>(this)

        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                val paymentIntent = result.intent
                val status = paymentIntent.status
                val outcome = result.outcome
                Log.d("onPaymentResult", "got status $status")
                Log.d("onPaymentResult", "got outcome $outcome")
                if (status == StripeIntent.Status.Succeeded) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    displayAlert(weakActivity.get(), "Payment succeeded", gson.toJson(paymentIntent), restartDemo = true)
                } else {
                    displayAlert(weakActivity.get(), "Payment failed", paymentIntent.lastPaymentError?.message ?: "")
                }
            }

            override fun onError(e: Exception) {
                displayAlert(weakActivity.get(), "Payment failed", e.toString())
            }
        })
    }

    private fun displayAlert(activity: Activity?, title: String, message: String, restartDemo: Boolean = false) {
        if (activity == null) {
            return
        }
        runOnUiThread {
            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(title)
            builder.setMessage(message)
            if (restartDemo) {
                builder.setPositiveButton("Restart demo") { _, _ ->
                    val cardInputWidget =
                            findViewById<CardInputWidget>(R.id.cardInputWidget)
                    cardInputWidget.clear()
                    startCheckout()
                }
            }
            else {
                builder.setPositiveButton("Ok", null)
            }
            val dialog = builder.create()
            dialog.show()
        }
    }
}