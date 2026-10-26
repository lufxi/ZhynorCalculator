package com.zhynorcalculator.app.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhynorcalculator.app.logic.CalcAction
import com.zhynorcalculator.app.logic.CalcState
import com.zhynorcalculator.app.logic.CalculatorViewModel
import com.zhynorcalculator.app.logic.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel()) {
    val state = vm.state
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }

    val onAction: (CalcAction) -> Unit = { action ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        vm.onAction(action)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CalcColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        DisplayPanel(
            state = state,
            landscape = landscape,
            onHistory = { showHistory = true },
            onComingSoon = {
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(if (landscape) 0.8f else 1f),
        )

        HorizontalDivider(
            color = CalcColors.Divider,
            modifier = Modifier.padding(start = 24.dp),
        )
        Spacer(Modifier.height(8.dp))

        if (landscape) {
            LandscapeKeypad(state, onAction, Modifier.weight(1.6f))
        } else {
            PortraitKeypad(state, onAction, Modifier.weight(1.5f))
        }
    }

    if (showHistory) {
        HistorySheet(
            history = state.history,
            onPick = {
                vm.onAction(CalcAction.LoadHistory(it))
                showHistory = false
            },
            onClear = { vm.onAction(CalcAction.ClearHistory) },
            onDismiss = { showHistory = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Display
// ---------------------------------------------------------------------------

@Composable
private fun DisplayPanel(
    state: CalcState,
    landscape: Boolean,
    onHistory: () -> Unit,
    onComingSoon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHistory) {
                Icon(Icons.Outlined.History, "History", tint = Color.White)
            }
            IconButton(onClick = onComingSoon) {
                Icon(Icons.Outlined.Calculate, "Unit converter", tint = Color.White)
            }
            IconButton(onClick = onComingSoon) {
                Icon(Icons.Outlined.CurrencyExchange, "Currency", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (state.memory != null) {
                Text("M", color = CalcColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.size(12.dp))
            }
            Text(
                if (state.isDeg) "DEG" else "RAD",
                color = CalcColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(end = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val text = state.expression.ifEmpty { "0" }
                val size = expressionSize(text.length, landscape)
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState(), reverseScrolling = true)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = size.sp,
                        lineHeight = (size * 1.15f).sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                when {
                    state.error -> Text(
                        "Error", color = CalcColors.Error,
                        fontSize = if (landscape) 18.sp else 24.sp,
                    )
                    state.preview.isNotEmpty() -> Text(
                        state.preview, color = CalcColors.Muted,
                        fontSize = if (landscape) 18.sp else 28.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun expressionSize(length: Int, landscape: Boolean): Float {
    val s = when {
        length <= 9 -> 60f
        length <= 14 -> 46f
        length <= 22 -> 36f
        else -> 28f
    }
    return if (landscape) s * 0.6f else s
}

// ---------------------------------------------------------------------------
// Keypads
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortraitKeypad(
    state: CalcState,
    onAction: (CalcAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState(pageCount = { 2 })
    Column(modifier.padding(horizontal = 16.dp)) {
        // Page 0 = Standard, page 1 = Scientific (swipe left)
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), pageSpacing = 16.dp) { page ->
            val rows = if (page == 0) standardRows() else scientificRows(state.isSecond, state.isDeg)
            KeyGrid(rows, onAction, Modifier.fillMaxSize())
        }
        PageDots(pager.currentPage)
    }
}

@Composable
private fun LandscapeKeypad(
    state: CalcState,
    onAction: (CalcAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        KeyGrid(scientificRows(state.isSecond, state.isDeg), onAction, Modifier.weight(1f).fillMaxHeight())
        KeyGrid(standardRows(), onAction, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PageDots(current: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(2) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (i == current) CalcColors.Accent else Color(0xFF3A3A3A))
            )
        }
    }
}

@Composable
private fun KeyGrid(
    rows: List<List<Key>>,
    onAction: (CalcAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    CalcKey(key, onAction, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun CalcKey(key: Key, onAction: (CalcAction) -> Unit, modifier: Modifier = Modifier) {
    val (bg, fg) = when (key.style) {
        KeyStyle.Number -> CalcColors.NumberKey to CalcColors.NumberText
        KeyStyle.Function -> CalcColors.FunctionKey to CalcColors.FunctionText
        KeyStyle.Control -> CalcColors.ControlKey to CalcColors.ControlText
        KeyStyle.Primary -> CalcColors.PrimaryKey to CalcColors.PrimaryText
        KeyStyle.Toggle -> CalcColors.ToggleKey to CalcColors.ToggleText
    }
    val large = key.style != KeyStyle.Function && key.style != KeyStyle.Toggle

    BoxWithConstraints(
        modifier = modifier
            .clip(CircleShape) // fully rounded ends = pill shape
            .background(bg)
            .clickable(role = Role.Button) { onAction(key.action) },
        contentAlignment = Alignment.Center,
    ) {
        if (key.icon != null) {
            Icon(
                key.icon,
                contentDescription = key.description,
                tint = fg,
                modifier = Modifier.size((maxHeight.value * 0.42f).coerceIn(16f, 30f).dp),
            )
        } else {
            val fontSize = (maxHeight.value * if (large) 0.42f else 0.34f).coerceIn(13f, 34f)
            Text(
                text = key.label,
                color = fg,
                fontSize = fontSize.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    history: List<HistoryItem>,
    onPick: (HistoryItem) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF141414)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = onClear, enabled = history.isNotEmpty()) { Text("Clear") }
        }

        if (history.isEmpty()) {
            Text(
                "No history yet",
                color = CalcColors.Muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            )
        } else {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(history) { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(item) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(item.expression, color = CalcColors.Muted, fontSize = 16.sp)
                        Text("= ${item.result}", color = Color.White, fontSize = 26.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
