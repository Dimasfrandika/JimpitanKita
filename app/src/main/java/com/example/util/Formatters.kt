package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val rupiahLocale = Locale.forLanguageTag("id-ID")
    private val currencyFormat = NumberFormat.getCurrencyInstance(rupiahLocale).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return try {
            currencyFormat.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp $amount"
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", rupiahLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", rupiahLocale)
        return sdf.format(Date(timestamp)) + " WIB"
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", rupiahLocale)
        return sdf.format(Date(timestamp))
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
