package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

enum class LedgerType(val label: String, val banglaTerm: String) {
    RECEIVABLE("RECEIVABLE", "PAONA"),
    PAYABLE("PAYABLE", "DENA")
}

enum class DueStatus(val label: String) {
    SETTLED("SETTLED"),
    NO_DUE_DATE("DUE DATE UNKNOWN"),
    OVERDUE("OVERDUE"),
    DUE_TODAY("DUE TODAY"),
    DUE_SOON("DUE SOON"),
    UPCOMING("SCHEDULED")
}

enum class AgingBucket(val label: String) {
    CURRENT("CURRENT / NOT DUE"),
    DAYS_1_TO_7("1-7 DAYS OVERDUE"),
    DAYS_8_TO_30("8-30 DAYS OVERDUE"),
    DAYS_31_PLUS("31+ DAYS OVERDUE")
}

data class DueEvaluation(
    val status: DueStatus,
    val daysDelta: Long?, // Null when due date is unknown; negative when overdue, 0 when due today, positive when upcoming
    val badgeText: String,
    val agingBucket: AgingBucket
)

/**
 * Deterministic due-date and overdue calculator using Asia/Dhaka calendar-day normalization.
 */
object DueDateCalculator {
    const val DHAKA_TIMEZONE_ID = "Asia/Dhaka"
    const val DUE_SOON_THRESHOLD_DAYS = 3L
    private const val MILLIS_PER_DAY = 86_400_000L

    val dhakaTimeZone: TimeZone
        get() = TimeZone.getTimeZone(DHAKA_TIMEZONE_ID)

    /**
     * Normalizes an epoch millisecond timestamp to the start of the calendar day (00:00:00.000)
     * in the Asia/Dhaka timezone.
     */
    fun normalizeToStartOfDayDhaka(epochMillis: Long): Long {
        val cal = Calendar.getInstance(dhakaTimeZone, Locale.ENGLISH)
        cal.timeInMillis = epochMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Parses an ISO local date string "YYYY-MM-DD" to the start of that calendar day (00:00:00.000)
     * in the Asia/Dhaka timezone. Returns null if blank or invalid.
     */
    fun parseIsoDateDhaka(isoDate: String): Long? {
        val cleaned = isoDate.trim()
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null

        val cal = Calendar.getInstance(dhakaTimeZone, Locale.ENGLISH)
        cal.isLenient = false
        return try {
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats an epoch timestamp as "YYYY-MM-DD" in the Asia/Dhaka timezone.
     */
    fun formatIsoDateDhaka(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = dhakaTimeZone
        return sdf.format(Date(epochMillis))
    }

    /**
     * Formats an epoch timestamp as "DD MMM YYYY (YYYY-MM-DD)" in the Asia/Dhaka timezone.
     */
    fun formatDisplayDateDhaka(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = dhakaTimeZone
        return sdf.format(Date(epochMillis)).uppercase(Locale.ENGLISH)
    }

    /**
     * Evaluates the due status and overdue days for a ledger record in Asia/Dhaka calendar days.
     * Does not invent due dates when [dueDateEpochMillis] is null.
     */
    fun evaluate(
        dueDateEpochMillis: Long?,
        remainingMinor: Long,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): DueEvaluation {
        if (remainingMinor <= 0L) {
            return DueEvaluation(
                status = DueStatus.SETTLED,
                daysDelta = 0L,
                badgeText = "SETTLED",
                agingBucket = AgingBucket.CURRENT
            )
        }

        if (dueDateEpochMillis == null) {
            return DueEvaluation(
                status = DueStatus.NO_DUE_DATE,
                daysDelta = null,
                badgeText = "DUE DATE UNKNOWN",
                agingBucket = AgingBucket.CURRENT
            )
        }

        val todayStart = normalizeToStartOfDayDhaka(nowEpochMillis)
        val dueStart = normalizeToStartOfDayDhaka(dueDateEpochMillis)
        val diffMillis = dueStart - todayStart
        val daysDelta = (diffMillis.toDouble() / MILLIS_PER_DAY.toDouble()).roundToLong()

        return when {
            daysDelta < 0L -> {
                val overdueDays = kotlin.math.abs(daysDelta)
                val bucket = when {
                    overdueDays <= 7L -> AgingBucket.DAYS_1_TO_7
                    overdueDays <= 30L -> AgingBucket.DAYS_8_TO_30
                    else -> AgingBucket.DAYS_31_PLUS
                }
                DueEvaluation(
                    status = DueStatus.OVERDUE,
                    daysDelta = daysDelta,
                    badgeText = "OVERDUE BY $overdueDays ${if (overdueDays == 1L) "DAY" else "DAYS"}",
                    agingBucket = bucket
                )
            }
            daysDelta == 0L -> DueEvaluation(
                status = DueStatus.DUE_TODAY,
                daysDelta = 0L,
                badgeText = "DUE TODAY",
                agingBucket = AgingBucket.CURRENT
            )
            daysDelta <= DUE_SOON_THRESHOLD_DAYS -> DueEvaluation(
                status = DueStatus.DUE_SOON,
                daysDelta = daysDelta,
                badgeText = "DUE IN $daysDelta ${if (daysDelta == 1L) "DAY" else "DAYS"}",
                agingBucket = AgingBucket.CURRENT
            )
            else -> DueEvaluation(
                status = DueStatus.UPCOMING,
                daysDelta = daysDelta,
                badgeText = "DUE IN $daysDelta DAYS",
                agingBucket = AgingBucket.CURRENT
            )
        }
    }

    /**
     * Shifts a base timestamp by [days] calendar days in the Asia/Dhaka timezone.
     */
    fun shiftDays(baseEpochMillis: Long, days: Int): Long {
        val cal = Calendar.getInstance(dhakaTimeZone, Locale.ENGLISH)
        cal.timeInMillis = baseEpochMillis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }
}
