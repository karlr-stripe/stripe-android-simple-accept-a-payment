package com.example.stripepaymentintegration

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private var backendUrl = BuildConfig.BACKEND_URL
    private var publishableKey = BuildConfig.PUBLISHABLE_KEY

    private lateinit var paymentIntentClientSecret: String
    private lateinit var stripe: Stripe


    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        toast(throwable.message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PaymentConfiguration.init(
            applicationContext,
            publishableKey
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                toast("Success!")
            } else {
                toast("Fail!")
            }
        }
    }

    fun onPay(view: View) {
        stripe = Stripe(applicationContext, PaymentConfiguration.getInstance(applicationContext).publishableKey)
        val card = PaymentMethodCreateParams.Card.Builder()
            .setNumber("4242424242424242")
            .setExpiryMonth(12)
            .setExpiryYear(2027)
            .setCvc("123")
            .build()
        val params = PaymentMethodCreateParams.create(card)
        stripe.createPaymentMethod(
            params,
            null,
            null,
            object : ApiResultCallback<PaymentMethod> {
                override fun onError(e: Exception) {
                    toast("Error getting payment method")
                }

                override fun onSuccess(result: PaymentMethod) {
                    val paymentMethodId = result.id ?: run {
                        toast("No payment method id")
                        return
                    }
                    getPaymentIntentAndStartActivity(result)
                }

            })
    }

    private fun getPaymentIntentAndStartActivity(result: PaymentMethod) {
        // Request a PaymentIntent from your server and store its client secret in paymentIntentClientSecret

        // from https://developer.android.com/training/volley/simple :

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = backendUrl

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                paymentIntentClientSecret = response
                Log.d("getPaymentIntentAndStartActivity", "got PaymentIntent secret $paymentIntentClientSecret")

                lifecycleScope.launch(coroutineExceptionHandler) {
                    val intent = PaymentActivity.getIntent(this@MainActivity,
                        result.id!!, paymentIntentClientSecret, publishableKey)
                    this@MainActivity.startActivityForResult(intent, REQUEST_PAYMENT)
                }
            },
            { error -> Log.e("getPaymentIntentAndStartActivity", "error loading PaymentIntent!") })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_PAYMENT = 231
    }
}
