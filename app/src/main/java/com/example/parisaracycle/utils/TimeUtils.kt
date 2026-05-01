package com.example.parisaracycle.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyyMM", Locale.US)

    fun dayKey(timeMillis: Long = System.currentTimeMillis()): String =
        dayFormat.format(Date(timeMillis))

    fun monthKey(timeMillis: Long = System.currentTimeMillis()): String =
        monthFormat.format(Date(timeMillis))
}
