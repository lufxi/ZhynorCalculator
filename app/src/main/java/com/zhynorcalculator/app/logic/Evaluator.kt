package com.zhynorcalculator.app.logic

import kotlin.math.*

class CalcException(message: String) : Exception(message)

/**
 * Recursive-descent expression evaluator.
 *
 * Grammar (lowest -> highest precedence):
 *   expr    := term (('+' | '−') term)*
 *   term    := unary (('×' | '÷' | implicit) unary)*
 *   unary   := ('−' | '+') unary | power
 *   power   := postfix ('^' unary | 'ʸ√' unary)?
 *   postfix := primary ('!' | '%' | '²' | '³')*
 *   primary := number | '(' expr ')' | '√' postfix | constant | fn '(' expr ')'
 */
object Evaluator {

    fun autoClose(expr: String): String {
        var open = 0
        for (c in expr) {
            if (c == '(') open++ else if (c == ')') open--
        }
        return if (open > 0) expr + ")".repeat(open) else expr
    }

    fun evaluate(expr: String, degrees: Boolean): Double {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) throw CalcException("Empty")
        val value = Parser(tokens, degrees).parse()
        if (value.isNaN() || value.isInfinite()) throw CalcException("Math error")
        return value
    }
}

private sealed interface Tok {
    data class Num(val v: Double) : Tok
    data class Sym(val c: Char) : Tok
    data class Ident(val name: String) : Tok
    data object Root : Tok // "ʸ√"  (y-th root of x, written  y ʸ√ x)
}

private fun tokenize(s: String): List<Tok> {
    val out = ArrayList<Tok>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c.isWhitespace()) { i++; continue }

        if (c.isDigit() || c == '.') {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (i < s.length && s[i] == 'E') { // scientific notation e.g. 1.5E-7
                var j = i + 1
                if (j < s.length && (s[j] == '+' || s[j] == '-' || s[j] == '−')) j++
                val digitsStart = j
                while (j < s.length && s[j].isDigit()) j++
                if (j == digitsStart) throw CalcException("Bad exponent")
                i = j
            }
            val text = s.substring(start, i).replace('−', '-')
            out.add(Tok.Num(text.toDoubleOrNull() ?: throw CalcException("Bad number")))
            continue
        }

        if (c == 'ʸ') {
            if (i + 1 < s.length && s[i + 1] == '√') { out.add(Tok.Root); i += 2; continue }
            throw CalcException("Syntax")
        }
        if (c == 'π') { out.add(Tok.Ident("π")); i++; continue }

        if (c.isLetter() || c == '₂') {
            val start = i
            while (i < s.length && (s[i].isLetter() || s[i] == '₂') && s[i] != 'π' && s[i] != 'ʸ') i++
            out.add(Tok.Ident(s.substring(start, i)))
            continue
        }

        val sym = when (c) { '-' -> '−'; '*' -> '×'; '/' -> '÷'; else -> c }
        if (sym in "+−×÷^()!%²³√") { out.add(Tok.Sym(sym)); i++; continue }

        throw CalcException("Unexpected '$c'")
    }
    return out
}

private class Parser(private val t: List<Tok>, private val deg: Boolean) {
    private var p = 0

    fun parse(): Double {
        val v = expr()
        if (p < t.size) throw CalcException("Syntax")
        return v
    }

    private fun peek(): Tok? = t.getOrNull(p)
    private fun isSym(c: Char) = (peek() as? Tok.Sym)?.c == c
    private fun prevIsPercent() = p > 0 && (t[p - 1] as? Tok.Sym)?.c == '%'

    private fun expr(): Double {
        var v = term()
        while (true) {
            if (isSym('+')) {
                p++
                val r = term()
                v += if (prevIsPercent()) v * r else r   // 200 + 10%  ->  220
            } else if (isSym('−')) {
                p++
                val r = term()
                v -= if (prevIsPercent()) v * r else r   // 200 − 10%  ->  180
            } else break
        }
        return v
    }

    private fun term(): Double {
        var v = unary()
        while (true) {
            if (isSym('×')) {
                p++; v *= unary()
            } else if (isSym('÷')) {
                p++
                val d = unary()
                if (d == 0.0) throw CalcException("Divide by zero")
                v /= d
            } else if (startsOperand()) {
                v *= unary() // implicit multiplication: 2π, 3(4+1), 2√9 …
            } else break
        }
        return v
    }

    private fun startsOperand(): Boolean = when (val k = peek()) {
        is Tok.Num, is Tok.Ident -> true
        is Tok.Sym -> k.c == '(' || k.c == '√'
        else -> false
    }

    private fun unary(): Double = when {
        isSym('−') -> { p++; -unary() }
        isSym('+') -> { p++; unary() }
        else -> power()
    }

    private fun power(): Double {
        val base = postfix()
        return when {
            isSym('^') -> { p++; base.pow(unary()) }
            peek() is Tok.Root -> { p++; root(base, unary()) }
            else -> base
        }
    }

    private fun postfix(): Double {
        var v = primary()
        while (true) {
            val k = peek()
            if (k is Tok.Sym && k.c in "!%²³") {
                p++
                v = when (k.c) {
                    '!' -> factorial(v)
                    '%' -> v / 100.0
                    '²' -> v * v
                    else -> v * v * v
                }
            } else break
        }
        return v
    }

    private fun primary(): Double {
        val k = peek() ?: throw CalcException("Unexpected end")
        p++
        return when (k) {
            is Tok.Num -> k.v
            is Tok.Sym -> when (k.c) {
                '(' -> { val v = expr(); closeParen(); v }
                '√' -> {
                    val v = postfix()
                    if (v < 0) throw CalcException("Math error")
                    sqrt(v)
                }
                else -> throw CalcException("Syntax")
            }
            is Tok.Ident -> identifier(k.name)
            is Tok.Root -> throw CalcException("Syntax")
        }
    }

    private fun closeParen() {
        if (isSym(')')) p++ else if (p < t.size) throw CalcException("Syntax")
    }

    private fun identifier(name: String): Double {
        if (name == "π") return PI
        if (name == "e") return E
        if (!isSym('(')) throw CalcException("Syntax")
        p++
        val arg = expr()
        closeParen()
        return applyFn(name, arg)
    }

    // ---- math helpers -------------------------------------------------------

    private fun toRad(x: Double) = if (deg) Math.toRadians(x) else x
    private fun fromRad(x: Double) = if (deg) Math.toDegrees(x) else x
    private fun snap(v: Double) = if (abs(v) < 1e-15) 0.0 else v

    private fun applyFn(name: String, x: Double): Double = when (name) {
        "sin" -> snap(sin(toRad(x)))
        "cos" -> snap(cos(toRad(x)))
        "tan" -> {
            if (deg && (x - 90.0) % 180.0 == 0.0) throw CalcException("Math error")
            snap(tan(toRad(x)))
        }
        "asin" -> { if (abs(x) > 1) throw CalcException("Math error"); fromRad(asin(x)) }
        "acos" -> { if (abs(x) > 1) throw CalcException("Math error"); fromRad(acos(x)) }
        "atan" -> fromRad(atan(x))
        "sinh" -> sinh(x)
        "cosh" -> cosh(x)
        "tanh" -> tanh(x)
        "asinh" -> ln(x + sqrt(x * x + 1))
        "acosh" -> { if (x < 1) throw CalcException("Math error"); ln(x + sqrt(x * x - 1)) }
        "atanh" -> { if (abs(x) >= 1) throw CalcException("Math error"); 0.5 * ln((1 + x) / (1 - x)) }
        "ln" -> { if (x <= 0) throw CalcException("Math error"); ln(x) }
        "lg" -> { if (x <= 0) throw CalcException("Math error"); log10(x) }
        "log₂" -> { if (x <= 0) throw CalcException("Math error"); log2(x) }
        else -> throw CalcException("Unknown function $name")
    }

    private fun factorial(v: Double): Double {
        if (v < 0 || v != floor(v) || v > 170) throw CalcException("Math error")
        var r = 1.0
        for (i in 2..v.toInt()) r *= i
        return r
    }

    /** y-th root of x */
    private fun root(y: Double, x: Double): Double {
        if (y == 0.0) throw CalcException("Math error")
        if (x < 0) {
            if (y == floor(y) && y % 2.0 != 0.0) return -((-x).pow(1.0 / y))
            throw CalcException("Math error")
        }
        return x.pow(1.0 / y)
    }
}
