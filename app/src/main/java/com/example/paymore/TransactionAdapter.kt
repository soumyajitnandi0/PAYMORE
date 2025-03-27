package com.example.paymore

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txnId: TextView = view.findViewById(R.id.txnId)
        val amount: TextView = view.findViewById(R.id.amount)
        val status: TextView = view.findViewById(R.id.status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.txnId.text = transaction.txnId ?: "N/A"
        holder.amount.text = "₹${transaction.amount}"
        holder.status.text = transaction.status

        holder.status.setTextColor(
            when (transaction.status) {
                "SUCCESS" -> Color.GREEN
                "PENDING" -> Color.YELLOW
                else -> Color.RED
            }
        )
    }

    override fun getItemCount() = transactions.size
}
