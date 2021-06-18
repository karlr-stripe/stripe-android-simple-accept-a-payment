package com.example.stripepaymentintegration

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.*
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
        stripe = Stripe(applicationContext, PaymentConfiguration.getInstance(applicationContext).publishableKey)
        startCheckout()
    }

    private fun startCheckout() {
        // TODO Request a PaymentIntent from your server and store its client secret in paymentIntentClientSecret
        // here we hard-code one for demonstration
        // in reality, your backend server would create a PaymentIntent and return the client_secret
        paymentIntentClientSecret = "pi_1J3PR2Fqoz5F0tFRGcGaeDAI_secret_zUoXOWv4h7CF7gdLD774Z65ve"

        Log.d("startCheckout", "got PaymentIntent secret $paymentIntentClientSecret")
        // Hook up the pay button
        val payButton: Button = findViewById(R.id.payButton)
        payButton.setOnClickListener {
            val billingDetails = PaymentMethod.BillingDetails(name = "Jenny Rosen", email = "jenny.rosen@example.com")
            val ideal = PaymentMethodCreateParams.Ideal("abn_amro")
            val paymentMethodCreateParams = PaymentMethodCreateParams.create(ideal, billingDetails)
            val confirmParams = ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(paymentMethodCreateParams, paymentIntentClientSecret, returnUrl = "myapp://ideal-complete")
            
            stripe.confirmPayment(this, confirmParams)

        }

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