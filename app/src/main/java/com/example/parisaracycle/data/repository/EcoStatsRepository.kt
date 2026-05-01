package com.example.parisaracycle.data.repository

import android.content.Context
import com.example.parisaracycle.data.model.EcoStats
import com.example.parisaracycle.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EcoStatsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("eco_stats", Context.MODE_PRIVATE)
    private val _stats = MutableStateFlow(readStats())

    val stats: StateFlow<EcoStats> = _stats.asStateFlow()

    @Synchronized
    fun addTrip(distanceKm: Double) {
        if (distanceKm <= 0.0) return

        val co2Grams = distanceKm * 120.0
        val day = TimeUtils.dayKey()
        val month = TimeUtils.monthKey()

        preferences.edit()
            .putFloat(dayDistanceKey(day), getFloat(dayDistanceKey(day)) + distanceKm.toFloat())
            .putFloat(dayCo2Key(day), getFloat(dayCo2Key(day)) + co2Grams.toFloat())
            .putFloat(monthDistanceKey(month), getFloat(monthDistanceKey(month)) + distanceKm.toFloat())
            .putFloat(monthCo2Key(month), getFloat(monthCo2Key(month)) + co2Grams.toFloat())
            .apply()

        _stats.value = readStats()
    }

    fun refresh() {
        _stats.value = readStats()
    }

    private fun readStats(): EcoStats {
        val day = TimeUtils.dayKey()
        val month = TimeUtils.monthKey()
        return EcoStats(
            todayDistanceKm = getFloat(dayDistanceKey(day)).toDouble(),
            todayCo2Grams = getFloat(dayCo2Key(day)).toDouble(),
            monthDistanceKm = getFloat(monthDistanceKey(month)).toDouble(),
            monthCo2Grams = getFloat(monthCo2Key(month)).toDouble()
        )
    }

    private fun getFloat(key: String): Float = preferences.getFloat(key, 0f)

    private fun dayDistanceKey(day: String) = "day_${day}_distance_km"
    private fun dayCo2Key(day: String) = "day_${day}_co2_g"
    private fun monthDistanceKey(month: String) = "month_${month}_distance_km"
    private fun monthCo2Key(month: String) = "month_${month}_co2_g"
}
