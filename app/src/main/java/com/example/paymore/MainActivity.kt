package com.example.paymore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val amountInput = findViewById<EditText>(R.id.amountInput)
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

    private val UPI_PAYMENT_REQUEST = 1  // ✅ Define this at the top

    private fun startUpiPayment(amount: String) {
        val upiUri = Uri.parse("upi://pay?pa=yourupi@upi&pn=Your Name&am=$amount&cu=INR")
        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
        startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST)  // ✅ Use request code
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPI_PAYMENT_REQUEST) {  // ✅ Handle response
            val response = data?.getStringExtra("response")
            val status = response?.substringAfter("Status=")?.substringBefore("&") ?: "FAILED"

            when (status.lowercase()) {
                "success" -> Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
                "pending" -> Toast.makeText(this, "Payment Pending", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Payment Failed or Canceled", Toast.LENGTH_SHORT).show()
            }
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
