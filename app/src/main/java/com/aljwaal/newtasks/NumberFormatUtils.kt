package com.aljwaal.newtasks

import java.util.Calendar
import java.util.Locale

object NumberFormatUtils {
    private val arabicIndic = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
    private val easternArabicIndic = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
    private val months = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    private val weekdays = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

    fun latinDigits(value: String): String {
        val output = StringBuilder(value.length)
        value.forEach { char ->
            val first = arabicIndic.indexOf(char)
            val second = easternArabicIndic.indexOf(char)
            output.append(
                when {
                    first >= 0 -> ('0'.code + first).toChar()
                    second >= 0 -> ('0'.code + second).toChar()
                    else -> char
                }
            )
        }
        return output.toString()
    }

    fun number(value: Int): String = value.toString()
    fun twoDigits(value: Int): String = String.format(Locale.US, "%02d", value)

    fun formatTime(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return "${twoDigits(calendar.get(Calendar.HOUR_OF_DAY))}:${twoDigits(calendar.get(Calendar.MINUTE))}"
    }

    fun formatDate(millis: Long, includeYear: Boolean = true): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return if (includeYear) "$day $month $year" else "$day $month"
    }

    fun formatDateTime(millis: Long): String = "${formatDate(millis)} • ${formatTime(millis)}"

    fun formatWeekdayDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val weekday = weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        return "$weekday، ${formatDate(millis)}"
    }

    fun monthTitle(year: Int, month: Int): String = "${months[month]} $year"
    fun monthName(month: Int): String = months[month]

    fun sameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = startOfDay(millis) + 86_400_000L - 1L

    fun withDateAndTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
