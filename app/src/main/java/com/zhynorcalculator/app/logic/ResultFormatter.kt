package com.zhynorcalculator.app.logic

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object ResultFormatter {
    /** Rounds to 12 significant digits (hides 0.1+0.2 style noise) and trims zeros. */
    fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) throw CalcException("Math error")
        if (v == 0.0) return "0"
        val bd = BigDecimal(v).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
        val exponent = bd.precision() - bd.scale() - 1
        val text = if (exponent >= 15 || exponent < -6) {
            bd.toString().replace("E+", "E")
        } else {
            bd.toPlainString()
        }
        return text.replace('-', '−')
    }
}
