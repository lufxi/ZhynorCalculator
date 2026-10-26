package com.zhynorcalculator.app.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.Locale
import kotlin.random.Random

class CalculatorViewModel : ViewModel() {

    var state by mutableStateOf(CalcState())
        private set

    fun onAction(action: CalcAction) {
        val s = state
        state = when (action) {
            is CalcAction.Digit -> digit(s, action.value)
            CalcAction.Dot -> dot(s)
            is CalcAction.Op -> op(s, action.symbol)
            is CalcAction.Insert -> insert(s, action.text)
            CalcAction.Reciprocal ->
                if (s.expression.isEmpty()) insert(s, "1÷(") else update(s, "1÷(${s.expression})")
            CalcAction.Equals -> equals(s)
            CalcAction.Clear -> s.copy(expression = "", preview = "", error = false, justEvaluated = false)
            CalcAction.Backspace -> backspace(s)
            CalcAction.Negate -> negate(s)
            CalcAction.ToggleSecond -> s.copy(isSecond = !s.isSecond)
            CalcAction.ToggleAngle -> {
                val n = s.copy(isDeg = !s.isDeg)
                n.copy(preview = if (n.justEvaluated) "" else previewOf(n.expression, n.isDeg))
            }
            CalcAction.MemClear -> s.copy(memory = null)
            CalcAction.MemAdd -> currentValue(s)?.let { s.copy(memory = (s.memory ?: 0.0) + it) } ?: s
            CalcAction.MemSub -> currentValue(s)?.let { s.copy(memory = (s.memory ?: 0.0) - it) } ?: s
            CalcAction.MemRecall -> s.memory?.let {
                val f = ResultFormatter.format(it)
                insert(s, if (f.startsWith("−")) "($f)" else f)
            } ?: s
            CalcAction.Random ->
                insert(s, String.format(Locale.US, "%.6f", Random.nextDouble()).trimEnd('0').trimEnd('.'))
            is CalcAction.LoadHistory -> update(s, action.item.result, evaluated = true)
            CalcAction.ClearHistory -> s.copy(history = emptyList())
        }
    }

    // ---- state helpers ------------------------------------------------------

    private fun update(s: CalcState, expr: String, evaluated: Boolean = false) = s.copy(
        expression = expr,
        justEvaluated = evaluated,
        error = false,
        preview = if (evaluated) "" else previewOf(expr, s.isDeg),
    )

    private val plainNumber = Regex("""^−?\d[\d.E−+]*$""")
    private val trailingNumber = Regex("""\d*\.?\d*$""")

    private fun previewOf(expr: String, deg: Boolean): String {
        if (expr.isEmpty() || plainNumber.matches(expr)) return ""
        return try {
            ResultFormatter.format(Evaluator.evaluate(Evaluator.autoClose(expr), deg))
        } catch (_: Exception) {
            ""
        }
    }

    private fun currentValue(s: CalcState): Double? = try {
        if (s.expression.isEmpty()) null
        else Evaluator.evaluate(Evaluator.autoClose(s.expression), s.isDeg)
    } catch (_: Exception) {
        null
    }

    // ---- input handling -----------------------------------------------------

    private fun digit(s: CalcState, d: String): CalcState {
        var e = if (s.justEvaluated) "" else s.expression
        if (trailingNumber.find(e)?.value == "0") e = e.dropLast(1) // no leading zeros
        return update(s, e + d)
    }

    private fun dot(s: CalcState): CalcState {
        val e = if (s.justEvaluated) "" else s.expression
        val seg = trailingNumber.find(e)?.value.orEmpty()
        return when {
            seg.isEmpty() -> update(s, "$e" + "0.")
            seg.contains('.') -> s
            else -> update(s, "$e.")
        }
    }

    private fun op(s: CalcState, sym: String): CalcState {
        val e = s.expression
        if (e.isEmpty()) return if (sym == "−") update(s, "−") else s
        val last = e.last()
        if (last == '(') return if (sym == "−") update(s, e + sym) else s
        if (sym == "−" && last in "×÷^") return update(s, e + sym) // e.g. 5×−3
        val base = e.trimEnd { it in "+−×÷^" }
        if (base.isEmpty() || base.endsWith("(")) return s
        return update(s, base + sym)
    }

    private val continuations = setOf("^", "!", "²", "³", "ʸ√", "%", "E")

    private fun insert(s: CalcState, text: String): CalcState {
        val isContinuation = text in continuations
        if (text == ")") {
            val open = s.expression.count { it == '(' } - s.expression.count { it == ')' }
            if (open <= 0 || s.expression.endsWith("(")) return s
            return update(s, s.expression + text)
        }
        val base = if (s.justEvaluated && !isContinuation) "" else s.expression
        if (isContinuation) {
            val last = base.lastOrNull() ?: return s
            if (last in "+−×÷^(") return s
            if (text == "E" && !last.isDigit()) return s
        }
        // "5" then "10^(" must become 5×10^( , not 510^(
        val needsTimes = text.first().isDigit() && base.lastOrNull()?.let { it.isDigit() || it == '.' } == true
        return update(s, base + (if (needsTimes) "×" else "") + text)
    }

    private val deletable = listOf(
        "asinh(", "acosh(", "atanh(", "sinh(", "cosh(", "tanh(",
        "asin(", "acos(", "atan(", "sin(", "cos(", "tan(",
        "log₂(", "ln(", "lg(", "√(", "ʸ√",
    ).sortedByDescending { it.length }

    private fun backspace(s: CalcState): CalcState {
        val e = s.expression
        if (e.isEmpty()) return s
        val token = deletable.firstOrNull { e.endsWith(it) }
        return update(s, if (token != null) e.dropLast(token.length) else e.dropLast(1))
    }

    private fun negate(s: CalcState): CalcState {
        val e = s.expression
        if (e.isEmpty()) return update(s, "−")
        if (plainNumber.matches(e)) return update(s, if (e.startsWith("−")) e.drop(1) else "−$e")
        val wrapped = Regex("""\(−(\d*\.?\d+)\)$""").find(e)
        if (wrapped != null) return update(s, e.substring(0, wrapped.range.first) + wrapped.groupValues[1])
        val seg = trailingNumber.find(e)?.value.orEmpty()
        if (seg.isNotEmpty() && seg != ".") return update(s, e.dropLast(seg.length) + "(−" + seg + ")")
        return s
    }

    private fun equals(s: CalcState): CalcState {
        if (s.expression.isEmpty()) return s
        val closed = Evaluator.autoClose(s.expression)
        return try {
            val result = ResultFormatter.format(Evaluator.evaluate(closed, s.isDeg))
            val newHistory =
                if (result == s.expression) s.history
                else (listOf(HistoryItem(closed, result)) + s.history).take(100)
            s.copy(
                expression = result,
                preview = "",
                error = false,
                justEvaluated = true,
                history = newHistory,
            )
        } catch (_: Exception) {
            s.copy(error = true, preview = "")
        }
    }
}
