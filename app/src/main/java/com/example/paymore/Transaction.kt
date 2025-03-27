package com.example.paymore

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val txnId: String?,
    val amount: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)



