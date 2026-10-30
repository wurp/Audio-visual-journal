package com.vibedev.app.journal

import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Calendar
import java.util.Locale

/** Friendly date and time labels for entries. */
object EntryDates {
    fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    fun dayHeading(time: Long): String = when {
        DateUtils.isToday(time) -> "Today"
        DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        else -> pattern("EEEEMMMMdyyyy", time)
    }

    fun fullDate(time: Long): String = pattern("EEEEMMMMdyyyy", time)

    fun time(time: Long): String = pattern("jmm", time)

    private fun pattern(skeleton: String, time: Long): String =
        DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), time).toString()
}
