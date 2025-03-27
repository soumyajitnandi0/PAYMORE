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
            // Use a verified UPI ID format
            val upiId = "7384427171-4@ybl"  // Replace with actual verified UPI ID

            val upiUri = Uri.parse(
                "upi://pay?" +
                        "pa=$upiId&" +
                        "pn=Soumyajit Nandi&" +
                        "am=$amount&" +
                        "cu=INR&" +
                        "tn=Test Transaction"
            )

            val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
            // Try multiple UPI apps if possible
            val apps = listOf(
                "com.google.android.apps.nbu.paisa.user",  // Google Pay
                "in.org.npci.upiapp",  // BHIM
                "com.phonepe.app"  // PhonePe
            )

            // Try different UPI apps
            for (app in apps) {
                upiIntent.setPackage(app)
                try {
                    startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST)
                    return
                } catch (e: Exception) {
                    Log.e("UPI_PAYMENT", "Failed with app $app: ${e.message}")
                }
            }

            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Payment initiation error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPI_PAYMENT_REQUEST) {
            if (data != null) {
                val responseStr = data.getStringExtra("response") ?: ""
                Log.e("UPI_FULL_RESPONSE", "Complete Response: $responseStr")

                // Detailed logging of all parameters
                responseStr.split("&").forEach { param ->
                    Log.d("UPI_PARAM", param)
                }

                val status = parseUpiResponse(responseStr, "Status") ?: "FAILED"
                val responseCode = parseUpiResponse(responseStr, "ResponseCode")
                val txnId = parseUpiResponse(responseStr, "TxnId")

                // More detailed error handling
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
                        // Log specific error details
                        Log.e("UPI_ERROR", "Status: $status, Response Code: $responseCode")
                        Toast.makeText(this, "Payment Failed: $status", Toast.LENGTH_LONG).show()
                        saveTransaction(txnId, amountInput.text.toString(), "FAILED")
                    }
                }
            } else {
                Toast.makeText(this, "Payment Cancelled or No Response", Toast.LENGTH_SHORT).show()
                saveTransaction(null, amountInput.text.toString(), "CANCELLED")
            }
        }
    }

    // Helper function to parse UPI response
    private fun parseUpiResponse(response: String, key: String): String? {
        return response.split("&")
            .map { it.split("=") }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.getOrNull(1)
    }

    private fun saveTransaction(txnId: String?, amount: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db.transactionDao().insertTransaction(
                Transaction(txnId = txnId, amount = amount, status = status)
            )
        }
    }
}