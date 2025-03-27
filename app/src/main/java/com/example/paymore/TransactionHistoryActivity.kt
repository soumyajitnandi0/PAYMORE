package com.example.paymore

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var transactionList: RecyclerView
    private lateinit var noTransactionsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        try {
            db = AppDatabase.getDatabase(this)
            transactionList = findViewById(R.id.transactionList)
            noTransactionsText = findViewById(R.id.historyTextView)

            transactionList.layoutManager = LinearLayoutManager(this)

            // More robust LiveData observation
            db.transactionDao().getAllTransactions().observe(this, Observer { transactions ->
                try {
                    if (transactions == null) {
                        Log.e("TransactionHistory", "Transactions list is null")
                        noTransactionsText.text = "Error loading transactions"
                        noTransactionsText.visibility = View.VISIBLE
                        transactionList.visibility = View.GONE
                    } else if (transactions.isEmpty()) {
                        noTransactionsText.text = "No Transactions Found"
                        noTransactionsText.visibility = View.VISIBLE
                        transactionList.visibility = View.GONE
                    } else {
                        noTransactionsText.visibility = View.GONE
                        transactionList.visibility = View.VISIBLE
                        transactionList.adapter = TransactionAdapter(transactions)
                    }
                } catch (e: Exception) {
                    Log.e("TransactionHistory", "Error processing transactions", e)
                    noTransactionsText.text = "Error loading transactions"
                    noTransactionsText.visibility = View.VISIBLE
                }
            })
        } catch (e: Exception) {
            Log.e("TransactionHistory", "Initialization error", e)
        }
    }
}