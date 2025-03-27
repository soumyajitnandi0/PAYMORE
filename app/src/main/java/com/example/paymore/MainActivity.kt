package com.example.paymore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var amountInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        amountInput = findViewById(R.id.amountInput)
        val payButton = findViewById<Button>(R.id.payButton)

        payButton.setOnClickListener {
            val amount = amountInput.text.toString().trim()
            if (amount.isNotEmpty()) {
                startUpiPayment(amount)
            } else {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show()
            }
        }

        val historyButton = findViewById<Button>(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    private val UPI_PAYMENT_REQUEST = 1

    private fun startUpiPayment(amount: String) {
        try {
            // Validate amount
            val parsedAmount = amount.toDoubleOrNull()
            if (parsedAmount == null || parsedAmount <= 0) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return
            }

            val upiId = "7384427171-4@ybl"  // Your verified UPI ID
            val merchantName = "PAYMORE"

            val upiUri = Uri.parse(
                "upi://pay?" +
                        "pa=$upiId&" +
                        "pn=$merchantName&" +
                        "am=$amount&" +
                        "cu=INR&" +
                        "tn=Detailed Payment Debug"
            )

            val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)

            val upiApps = listOf(
                "com.google.android.apps.nbu.paisa.user",  // Google Pay
                "in.org.npci.upiapp",                      // BHIM
                "com.phonepe.app",                         // PhonePe
                "com.paytm.wallet",                        // Paytm
                "net.one97.paytm"                          // Paytm Alternate
            )

            var paymentAttempted = false
            for (app in upiApps) {
                upiIntent.setPackage(app)
                try {
                    startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST)
                    paymentAttempted = true
                    break
                } catch (e: Exception) {
                    Log.w("UPI_PAYMENT_DEBUG", "App attempt failed: $app, Error: ${e.message}")
                }
            }

            if (!paymentAttempted) {
                Toast.makeText(this, "No compatible UPI app found", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("UPI_PAYMENT_DEBUG", "Payment initiation error", e)
            Toast.makeText(this, "Payment setup error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPI_PAYMENT_REQUEST) {
            // Comprehensive logging of full response
            val responseStr = data?.getStringExtra("response") ?: ""
            Log.e("UPI_FULL_RESPONSE", "Complete Raw Response: $responseStr")

            // Detailed parameter logging
            responseStr.split("&").forEach { param ->
                Log.d("UPI_PARAM_DEBUG", "Param: $param")
            }

            val status = parseUpiResponse(responseStr, "Status") ?: "FAILED"
            val txnId = parseUpiResponse(responseStr, "TxnId")
            val responseCode = parseUpiResponse(responseStr, "ResponseCode")
            val approvalRefNo = parseUpiResponse(responseStr, "ApprovalRefNo")

            // Enhanced logging for debugging
            Log.e("UPI_PAYMENT_DEBUG", """
            Status: $status
            Transaction ID: $txnId
            Response Code: $responseCode
            Approval Ref No: $approvalRefNo
        """.trimIndent())

            when {
                status.equals("SUCCESS", true) -> {
                    Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
                    saveTransaction(txnId, amountInput.text.toString(), "SUCCESS")
                }
                status.equals("PENDING", true) -> {
                    Toast.makeText(this, "Payment Pending", Toast.LENGTH_SHORT).show()
                    saveTransaction(txnId, amountInput.text.toString(), "PENDING")
                }
                else -> {
                    val errorDetails = """
                    Detailed Error Information:
                    Status: $status
                    Response Code: $responseCode
                    Transaction ID: $txnId
                """.trimIndent()

                    Log.e("UPI_PAYMENT_ERROR", errorDetails)

                    val errorMessage = when {
                        status == "FAILURE" && responseCode == "Z302" ->
                            "Transaction Limit Exceeded. Please check with your bank."
                        responseCode == "Z301" ->
                            "Insufficient Balance"
                        responseCode == "U40" ->
                            "Incorrect UPI PIN"
                        else ->
                            "Payment Failed: $status (Code: $responseCode)"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    saveTransaction(txnId, amountInput.text.toString(), "FAILED")
                }
            }
        }
    }

    // Enhanced parsing to handle more complex responses
    private fun parseUpiResponse(response: String, key: String): String? {
        return try {
            response.split("&")
                .map { it.split("=") }
                .firstOrNull { it.size == 2 && it[0].equals(key, ignoreCase = true) }
                ?.getOrNull(1)
                ?.let { Uri.decode(it) }
        } catch (e: Exception) {
            Log.e("UPI_RESPONSE_PARSE", "Error parsing $key", e)
            null
        }
    }

    private fun saveTransaction(txnId: String?, amount: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db.transactionDao().insertTransaction(
                Transaction(txnId = txnId, amount = amount, status = status)
            )
        }
    }
}