package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exact monetary arithmetic utility for DENA-PAONA.
 *
 * All currency values are stored as exact integer minor units (paisa: 1 BDT = 100 minor units)
 * to eliminate IEEE 754 floating-point precision drift across partial repayments and net totals.
 */
object MoneyMath {
    const val MINOR_UNITS_PER_BDT = 100L
    private val HUNDRED = BigDecimal(100)

    /**
     * Converts whole BDT integer to exact minor units (paisa).
     */
    fun bdtToMinor(bdtWhole: Long): Long = Math.multiplyExact(bdtWhole, MINOR_UNITS_PER_BDT)

    /**
     * Parses a user-entered decimal string (e.g. "200", "110.50") into exact minor units.
     * Returns null if invalid, negative, or exceeds 2 decimal places.
     */
    fun parseBdtToMinor(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd < BigDecimal.ZERO) return null
            if (bd.scale() > 2) return null
            bd.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts minor units to an exact BigDecimal with scale 2 (or stripped trailing zeros when whole).
     */
    fun minorToBigDecimal(minorUnits: Long): BigDecimal =
        BigDecimal.valueOf(minorUnits).divide(HUNDRED, 2, RoundingMode.HALF_UP)

    /**
     * Computes remaining balance in minor units: max(0, principalMinor - repaidMinor).
     */
    fun remainingMinor(principalMinor: Long, repaidMinor: Long): Long =
        (principalMinor - repaidMinor).coerceAtLeast(0L)

    /**
     * Validates whether a proposed repayment in minor units is valid against current remaining balance.
     */
    fun isValidRepayment(repaymentMinor: Long, currentRemainingMinor: Long): Boolean =
        repaymentMinor > 0L && repaymentMinor <= currentRemainingMinor

    /**
     * Formats minor units as uppercase BDT string.
     * Whole amounts render cleanly (e.g., "BDT 200") while fractional amounts show 2 decimals ("BDT 200.50").
     */
    fun formatBdt(minorUnits: Long, showPlusSign: Boolean = false): String {
        val signPrefix = when {
            minorUnits < 0L -> "-"
            minorUnits > 0L && showPlusSign -> "+"
            else -> ""
        }
        val absMinor = kotlin.math.abs(minorUnits)
        val whole = absMinor / MINOR_UNITS_PER_BDT
        val fraction = absMinor % MINOR_UNITS_PER_BDT
        val formattedNumber = formatWithGrouping(whole)
        return if (fraction == 0L) {
            "BDT $signPrefix$formattedNumber"
        } else {
            val fractionPadded = fraction.toString().padStart(2, '0')
            "BDT $signPrefix$formattedNumber.$fractionPadded"
        }
    }

    /**
     * Formats minor units as plain input string for editing forms (e.g. "200" or "200.50").
     */
    fun formatPlainAmount(minorUnits: Long): String {
        val whole = minorUnits / MINOR_UNITS_PER_BDT
        val fraction = minorUnits % MINOR_UNITS_PER_BDT
        return if (fraction == 0L) {
            whole.toString()
        } else {
            "$whole.${fraction.toString().padStart(2, '0')}"
        }
    }

    private fun formatWithGrouping(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val sb = StringBuilder()
        var count = 0
        for (i in raw.length - 1 downTo 0) {
            sb.append(raw[i])
            count++
            if (count == 3 && i > 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString()
    }
}
