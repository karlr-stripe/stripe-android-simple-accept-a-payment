package com.example.stripepaymentintegration

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.GsonBuilder
import com.stripe.android.*
import com.stripe.android.model.*
import com.stripe.android.view.CardInputWidget
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    // these need to be set via gradle.properties
    private var backendUrl = BuildConfig.BACKEND_URL
    private var publishableKey = BuildConfig.PUBLISHABLE_KEY

    private lateinit var paymentIntentClientSecret: String
    private val stripe: Stripe by lazy {
        Stripe(
            applicationContext,
            PaymentConfiguration.getInstance(applicationContext).publishableKey
        )
    }

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
                    // Hook up the pay button
                    val payButton: Button = findViewById(R.id.payButton)
                    payButton.setOnClickListener {
                            val billingDetails = PaymentMethod.BillingDetails(name = "Jenny Rosen")
                            val paymentMethodCreateParams = PaymentMethodCreateParams.createGrabPay(billingDetails)
                            val confirmParams = ConfirmPaymentIntentParams
                                .createWithPaymentMethodCreateParams(
                                    paymentMethodCreateParams = paymentMethodCreateParams,
                                    clientSecret = paymentIntentClientSecret,
                                    returnUrl = "yourapp://checkout_complete"
                            )
                            stripe.confirmPayment(this, confirmParams)
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
                Log.d("onPaymentResult", "got status $status")
                if (status == StripeIntent.Status.Succeeded) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    displayAlert(
                        weakActivity.get(),
                        "Payment succeeded",
                        gson.toJson(paymentIntent),
                        restartDemo = true
                    )
                } else if (status == StripeIntent.Status.Processing) {
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        displayAlert(weakActivity.get(), "Payment is processing, please wait..", gson.toJson(paymentIntent), restartDemo = false)
                        // wait a few seconds for the payment to complete
                        // purely for demonstration — in a real integration you should robustly poll here instead
                        Handler(Looper.getMainLooper()).postDelayed(
                            {
                                stripe.retrievePaymentIntent(clientSecret = paymentIntent.clientSecret!!, callback = object : ApiResultCallback<PaymentIntent> {
                                    override fun onSuccess(retrievedPaymentIntent: PaymentIntent) {
                                        if (retrievedPaymentIntent.status == StripeIntent.Status.Succeeded) {
                                            val gson = GsonBuilder().setPrettyPrinting().create()
                                            displayAlert(
                                                weakActivity.get(),
                                                "Payment succeeded",
                                                gson.toJson(retrievedPaymentIntent),
                                                restartDemo = true
                                            )
                                        }else{
                                            Log.d("retrievePaymentIntent", "got status $status")
                                            // TODO : wait and poll again, etc..
                                        }
                                    }

                                    override fun onError(e: Exception) {
                                        Log.e("retrievePaymentIntent", "error retrieving PaymentIntent while processing ${e.message}")
                                    }
                                })
                            },
                            3000 // value in milliseconds
                        )
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