package com.example.stripepaymentintegration

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams

class PaymentActivity : AppCompatActivity() {

    private val paymentId by lazy { intent.getStringExtra(EXTRA_PAYMENT_ID) ?: "" }
    private val secret by lazy { intent.getStringExtra(EXTRA_SECRET) ?: "" }
    private val publicKey by lazy { intent.getStringExtra(EXTRA_PUBLIC_KEY) ?: "" }

    private lateinit var stripe: Stripe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        stripe = Stripe(applicationContext, publicKey)

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(paymentId, secret)
        stripe.confirmPayment(this, confirmParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onPaymentResult(requestCode, data, object: ApiResultCallback<PaymentIntentResult> {
            override fun onError(e: Exception) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onSuccess(result: PaymentIntentResult) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        })
    }

    companion object {
        private const val EXTRA_PAYMENT_ID = "EXTRA_PAYMENT_ID"
        private const val EXTRA_SECRET = "EXTRA_SECRET"
        private const val EXTRA_PUBLIC_KEY= "EXTRA_PUBLIC_KEY"

        fun getIntent(context: Context, paymentId: String, secret: String, publicKey: String) = Intent(context, PaymentActivity::class.java).apply {
            putExtra(EXTRA_PAYMENT_ID, paymentId)
            putExtra(EXTRA_SECRET, secret)
            putExtra(EXTRA_PUBLIC_KEY, publicKey)
        }
    }
}