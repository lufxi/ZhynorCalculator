package com.zhynorcalculator.app.logic

data class HistoryItem(val expression: String, val result: String)

data class CalcState(
    val expression: String = "",
    val preview: String = "",
    val error: Boolean = false,
    val justEvaluated: Boolean = false,
    val isDeg: Boolean = false,
    val isSecond: Boolean = false,
    val memory: Double? = null,
    val history: List<HistoryItem> = emptyList(),
)

sealed interface CalcAction {
    data class Digit(val value: String) : CalcAction
    data object Dot : CalcAction
    data class Op(val symbol: String) : CalcAction
    data class Insert(val text: String) : CalcAction
    data object Reciprocal : CalcAction
    data object Equals : CalcAction
    data object Clear : CalcAction
    data object Backspace : CalcAction
    data object Negate : CalcAction
    data object ToggleSecond : CalcAction
    data object ToggleAngle : CalcAction
    data object MemClear : CalcAction
    data object MemAdd : CalcAction
    data object MemSub : CalcAction
    data object MemRecall : CalcAction
    data object Random : CalcAction
    data class LoadHistory(val item: HistoryItem) : CalcAction
    data object ClearHistory : CalcAction
}
