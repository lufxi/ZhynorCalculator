package com.zhynorcalculator.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.zhynorcalculator.app.logic.CalcAction

enum class KeyStyle { Number, Function, Control, Primary, Toggle }

data class Key(
    val label: AnnotatedString,
    val style: KeyStyle,
    val action: CalcAction,
    val icon: ImageVector? = null,
    val description: String? = null,
)

private fun k(text: String, style: KeyStyle, action: CalcAction) =
    Key(AnnotatedString(text), style, action)

private fun digit(n: String) = k(n, KeyStyle.Number, CalcAction.Digit(n))

private fun sup(base: String, exp: String, action: CalcAction) = Key(
    buildAnnotatedString {
        append(base)
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)) { append(exp) }
    },
    KeyStyle.Function,
    action,
)

/** Right-hand block: memory row + digits + basic operators (the "Standard" page). */
fun standardRows(): List<List<Key>> = listOf(
    listOf(
        k("mc", KeyStyle.Function, CalcAction.MemClear),
        k("m+", KeyStyle.Function, CalcAction.MemAdd),
        k("m−", KeyStyle.Function, CalcAction.MemSub),
        k("mr", KeyStyle.Function, CalcAction.MemRecall),
    ),
    listOf(
        k("AC", KeyStyle.Control, CalcAction.Clear),
        Key(
            AnnotatedString("⌫"), KeyStyle.Control, CalcAction.Backspace,
            icon = Icons.AutoMirrored.Outlined.Backspace, description = "Backspace",
        ),
        k("+/−", KeyStyle.Control, CalcAction.Negate),
        k("÷", KeyStyle.Control, CalcAction.Op("÷")),
    ),
    listOf(digit("7"), digit("8"), digit("9"), k("×", KeyStyle.Control, CalcAction.Op("×"))),
    listOf(digit("4"), digit("5"), digit("6"), k("−", KeyStyle.Control, CalcAction.Op("−"))),
    listOf(digit("1"), digit("2"), digit("3"), k("+", KeyStyle.Control, CalcAction.Op("+"))),
    listOf(
        k("%", KeyStyle.Number, CalcAction.Insert("%")),
        digit("0"),
        k(".", KeyStyle.Number, CalcAction.Dot),
        k("=", KeyStyle.Primary, CalcAction.Equals),
    ),
)

/** Left-hand block: scientific functions (the "Scientific" page). */
fun scientificRows(second: Boolean, deg: Boolean): List<List<Key>> {
    val f = KeyStyle.Function

    // sin / cos / … become sin⁻¹ / cos⁻¹ / … when 2nd is active
    fun fn(name: String): Key =
        if (second) sup(name, "−1", CalcAction.Insert("a$name("))
        else k(name, f, CalcAction.Insert("$name("))

    val yRoot = Key(
        buildAnnotatedString {
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)) { append("y") }
            append("√x")
        },
        f,
        CalcAction.Insert("ʸ√"),
    )

    return listOf(
        listOf(
            k("2nd", if (second) KeyStyle.Toggle else f, CalcAction.ToggleSecond),
            k("(", f, CalcAction.Insert("(")),
            k(")", f, CalcAction.Insert(")")),
            if (second) sup("2", "x", CalcAction.Insert("2^(")) else sup("10", "x", CalcAction.Insert("10^(")),
        ),
        listOf(
            k("1/x", f, CalcAction.Reciprocal),
            sup("x", "2", CalcAction.Insert("²")),
            sup("x", "3", CalcAction.Insert("³")),
            sup("x", "y", CalcAction.Insert("^")),
        ),
        listOf(
            k("x!", f, CalcAction.Insert("!")),
            k("√", f, CalcAction.Insert("√(")),
            yRoot,
            if (second) k("log₂", f, CalcAction.Insert("log₂(")) else k("lg", f, CalcAction.Insert("lg(")),
        ),
        listOf(fn("sin"), fn("cos"), fn("tan"), k("ln", f, CalcAction.Insert("ln("))),
        listOf(fn("sinh"), fn("cosh"), fn("tanh"), sup("e", "x", CalcAction.Insert("e^("))),
        listOf(
            k(if (deg) "Deg" else "Rad", f, CalcAction.ToggleAngle),
            k("π", f, CalcAction.Insert("π")),
            k("EE", f, CalcAction.Insert("E")),
            k("Rand", f, CalcAction.Random),
        ),
    )
}
