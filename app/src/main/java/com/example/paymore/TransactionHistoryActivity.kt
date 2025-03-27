package com.example.paymore

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        db = AppDatabase.getDatabase(this)
        val transactionList = findViewById<RecyclerView>(R.id.transactionList)
        transactionList.layoutManager = LinearLayoutManager(this)

        db.transactionDao().getAllTransactions().observe(this, { transactions ->
            transactionList.adapter = TransactionAdapter(transactions)
        })
    }
}
