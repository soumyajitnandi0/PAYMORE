package com.example.paymore

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        db = AppDatabase.getDatabase(this)
        val transactionList = findViewById<RecyclerView>(R.id.transactionList)
        val noTransactionsText = findViewById<TextView>(R.id.historyTextView)
        transactionList.layoutManager = LinearLayoutManager(this)

        db.transactionDao().getAllTransactions().observe(this) { transactions ->
            if (transactions.isEmpty()) {
                noTransactionsText.visibility = View.VISIBLE
                transactionList.visibility = View.GONE
            } else {
                noTransactionsText.visibility = View.GONE
                transactionList.visibility = View.VISIBLE
                transactionList.adapter = TransactionAdapter(transactions)
            }
        }
    }
}