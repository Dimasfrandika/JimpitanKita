package com.example.util

import java.util.Calendar

data class MonthInfo(
    val monthIndex: Int, // 1..12
    val monthName: String,
    val year: Int,
    val daysInMonth: Int,
    val monthKey: String // "YYYY-MM"
)

object CalendarHelper {
    val MONTH_NAMES = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun getDaysInMonth(year: Int, monthIndex: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthIndex - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthInfo(year: Int, monthIndex: Int): MonthInfo {
        val days = getDaysInMonth(year, monthIndex)
        val monthStr = String.format("%02d", monthIndex)
        val name = MONTH_NAMES.getOrElse(monthIndex - 1) { "Bulan $monthIndex" }
        return MonthInfo(
            monthIndex = monthIndex,
            monthName = name,
            year = year,
            daysInMonth = days,
            monthKey = "$year-$monthStr"
        )
    }

    fun getCurrentMonthIndex(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) + 1 // 1..12
    }

    fun getCurrentYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR)
    }

    fun getAvailableYears(): List<Int> {
        val current = getCurrentYear()
        return listOf(current - 1, current, current + 1)
    }
}
