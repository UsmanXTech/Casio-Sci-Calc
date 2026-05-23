package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.CalculatorMode
import com.example.calculator.CalculatorViewModel
import com.example.calculator.AngleUnit
import com.example.data.HistoryItem
import java.util.Locale

@Composable
fun CalculatorMainScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMode by viewModel.currentMode.collectAsState()
    val isShift by viewModel.isShiftActive.collectAsState()
    val isAlpha by viewModel.isAlphaActive.collectAsState()
    val angleUnit by viewModel.angleUnit.collectAsState()
    val exprInput by viewModel.expressionInput.collectAsState()
    val evalResult by viewModel.evaluationResult.collectAsState()

    var showHistory by remember { mutableStateOf(false) }

    val tabModes = listOf(
        Pair(CalculatorMode.COMP, "COMP"),
        Pair(CalculatorMode.MATRIX, "MATRIX"),
        Pair(CalculatorMode.GRAPH_2D, "GRAPH 2D"),
        Pair(CalculatorMode.GRAPH_3D, "GRAPH 3D"),
        Pair(CalculatorMode.TUTORIAL, "TUTORIAL")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFBFF))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector and Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CASIO",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF1C1B1F),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "cl-991EX Premium (Material)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Rad/Deg selector
                Button(
                    onClick = { viewModel.toggleAngleUnit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("rad_deg_toggle")
                ) {
                    Text(
                        text = angleUnit.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // History Drawer Toggle
                IconButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFE1E2EC), RoundedCornerShape(8.dp))
                        .testTag("history_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Show history logs",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF1C1B1F)
                    )
                }

                // Export share action
                IconButton(
                    onClick = { viewModel.exportHistoryToText(context) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFC4EED0), RoundedCornerShape(8.dp))
                        .testTag("export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export backup",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF00210E)
                    )
                }
            }
        }

        // Mode Slider Selector Tabs
        ScrollableTabRow(
            selectedTabIndex = tabModes.indexOfFirst { it.first == currentMode },
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            divider = {},
            indicator = {},
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            tabModes.forEachIndexed { index, (mode, name) ->
                val selected = currentMode == mode
                Tab(
                    selected = selected,
                    onClick = {
                        viewModel.setMode(mode)
                        showHistory = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE1E2EC)
                        )
                        .height(36.dp)
                        .testTag("tab_${name.lowercase()}"),
                    text = {
                        Text(
                            text = name,
                            color = if (selected) Color.White else Color(0xFF1C1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Expanded Drawer or History Panel
        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            HistoryPanel(viewModel = viewModel)
        }

        // MAIN ADAPTIVE VIEW BASED ON SELECT MODE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentMode) {
                CalculatorMode.COMP -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // High Legibility LCD M3 Screen
                        LcdDisplay(
                            expr = exprInput,
                            res = evalResult,
                            isShift = isShift,
                            isAlpha = isAlpha,
                            angleUnit = angleUnit,
                            viewModel = viewModel
                        )

                        // Custom scientific key matrix (all-in-one spacious layout)
                        KeyboardScientificGrid(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CalculatorMode.MATRIX -> {
                    MatrixCalculatorView(viewModel = viewModel)
                }
                CalculatorMode.GRAPH_2D -> {
                    Graph2DView(viewModel = viewModel)
                }
                CalculatorMode.GRAPH_3D -> {
                    Graph3DView(viewModel = viewModel)
                }
                CalculatorMode.TUTORIAL -> {
                    EducationalTutorialView()
                }
            }
        }
    }
}

@Composable
fun LcdDisplay(
    expr: String,
    res: String,
    isShift: Boolean,
    isAlpha: Boolean,
    angleUnit: AngleUnit,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.historyState.collectAsState()
    val memoryVal by viewModel.memoryValue.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F3F7)), // elevation-1 card
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status bar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modifiers and Memory markers
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isShift) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFC4EED0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SHIFT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00210E))
                        }
                    }
                    if (isAlpha) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFC2D9FF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ALPHA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF031D38))
                        }
                    }
                    if (memoryVal != 0.0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE1E2EC), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101316))
                        }
                    }
                }

                // Angle Unit mode pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD3E3FD), RoundedCornerShape(10.dp))
                        .clickable { viewModel.toggleAngleUnit() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = angleUnit.name,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = Color(0xFF0A1C35),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1st tier (History Row in outline muted)
            Text(
                text = if (historyList.isNotEmpty()) "${historyList.last().expression} = ${historyList.last().result}" else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF74777F),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            // 2nd tier (Top Expression Row in on-surface-variant)
            Text(
                text = expr.ifEmpty { "0" },
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                color = Color(0xFF44474F), // on-surface-variant
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            // 3rd tier (Current Input/Result Row in on-surface bold)
            Text(
                text = res,
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F), // on-surface
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lcd_result_output")
            )
        }
    }
}

fun getSecondaryLabel(key: String): String {
    return when (key) {
        "sin" -> "sin⁻¹ | sinh"
        "cos" -> "cos⁻¹ | cosh"
        "tan" -> "tan⁻¹ | tanh"
        "ln" -> "eˣ"
        "log" -> "10ˣ"
        "log2" -> "logₙ"
        "^" -> "ⁿ√"
        "sqrt" -> "∛"
        "x²" -> "x³"
        "1/x" -> "n!"
        "pi" -> "φ"
        "e" -> "c"
        "Ans" -> "RND"
        "EXP" -> "mod"
        "%" -> "±"
        "floor" -> "ceil"
        "round" -> "RAND"
        else -> ""
    }
}

fun getPrimaryDisplayLabel(key: String): String {
    return when (key) {
        "pi" -> "π"
        "x²" -> "x²"
        "^" -> "xⁿ"
        "sqrt" -> "√x"
        "floor" -> "floor"
        "round" -> "rnd"
        else -> key
    }
}

@Composable
fun KeyboardScientificGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val keyRows = listOf(
        "SHIFT", "ALPHA", "MC", "MR", "MS", "M+",
        "M-", "(", ")", "mod", "abs", "1/x",
        "x²", "^", "sqrt", "pi", "e", "EXP",
        "sin", "cos", "tan", "log", "ln", "log2",
        "7", "8", "9", "DEL", "AC", "%",
        "4", "5", "6", "*", "/", "nPr",
        "1", "2", "3", "+", "-", "nCr",
        "0", ".", "±", "floor", "round", "="
    )

    val isShiftActive by viewModel.isShiftActive.collectAsState()
    val isAlphaActive by viewModel.isAlphaActive.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(keyRows) { k ->
            // Determine button tier and styling of Material 3 layout
            val isNumber = k in setOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".")
            val isOperator = k in setOf("+", "-", "*", "/", "=")
            val isAction = k in setOf("SHIFT", "ALPHA", "MC", "MR", "MS", "M+", "M-", "DEL", "AC", "%", "±", "CE", "C")
            
            val containerColor = when {
                isNumber -> Color(0xFFE1E2EC) // surface-variant
                isOperator -> Color(0xFFD3E3FD) // primary-container
                isAction -> {
                    if (k == "SHIFT" && isShiftActive) {
                        Color(0xFFFFB300) // active Shift accent
                    } else if (k == "ALPHA" && isAlphaActive) {
                        Color(0xFF00FFCC) // active Alpha accent
                    } else {
                        Color(0xFFC4EED0) // tertiary-container
                    }
                }
                else -> Color(0xFFC2D9FF) // secondary-container (Functions)
            }

            val textColor = when {
                isNumber -> Color(0xFF101316)
                isOperator -> Color(0xFF0A1C35)
                isAction -> Color(0xFF00210E)
                else -> Color(0xFF031D38)
            }

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .clickable { viewModel.onKeyPressed(k) }
                    .testTag("key_$k"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val secLabel = getSecondaryLabel(k)
                    if (secLabel.isNotEmpty()) {
                        Text(
                            text = secLabel,
                            fontSize = 8.sp,
                            color = Color(0xFF74777F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = getPrimaryDisplayLabel(k),
                        fontSize = if (k.length > 3) 11.sp else 13.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryPanel(viewModel: CalculatorViewModel) {
    val historyList by viewModel.historyState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOMIZABLE CALC HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.size(24.dp).testTag("clear_all_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all history",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "History empty. Do some math!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(historyList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .clickable { viewModel.selectHistoryExpression(item) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.expression,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "= ${item.result}",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00FFCC),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Star calculation",
                                        tint = if (item.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixCalculatorView(viewModel: CalculatorViewModel) {
    val mRowsA by viewModel.matrixRowsA.collectAsState()
    val mColsA by viewModel.matrixColsA.collectAsState()
    val mRowsB by viewModel.matrixRowsB.collectAsState()
    val mColsB by viewModel.matrixColsB.collectAsState()
    val mRowsC by viewModel.matrixRowsC.collectAsState()
    val mColsC by viewModel.matrixColsC.collectAsState()

    val activeSlot by viewModel.activeMatrixSlot.collectAsState()
    val currentMode by viewModel.displayMode.collectAsState()
    val scalarFactor by viewModel.scalarFactor.collectAsState()

    val manualRowI by viewModel.manualRowI.collectAsState()
    val manualRowJ by viewModel.manualRowJ.collectAsState()
    val manualScalarK by viewModel.manualScalarK.collectAsState()

    val matrixErrorMsg by viewModel.matrixErrorMsg.collectAsState()
    val matrixResult by viewModel.matrixResultText.collectAsState()

    var showDimDialog by remember { mutableStateOf(false) }

    var tempRows by remember { mutableStateOf(3) }
    var tempCols by remember { mutableStateOf(3) }

    val activeRows = when (activeSlot) {
        "A" -> mRowsA
        "B" -> mRowsB
        else -> mRowsC
    }
    val activeCols = when (activeSlot) {
        "A" -> mColsA
        "B" -> mColsB
        else -> mColsC
    }
    val activeValues = when (activeSlot) {
        "A" -> viewModel.matrixValuesA
        "B" -> viewModel.matrixValuesB
        else -> viewModel.matrixValuesC
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Matrix Slot Selection",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val slots = listOf("A", "B", "C")
                    slots.forEach { slot ->
                        val isSelected = activeSlot == slot
                        val dims = when (slot) {
                            "A" -> "$mRowsA×$mColsA"
                            "B" -> "$mRowsB×$mColsB"
                            else -> "$mRowsC×$mColsC"
                        }
                        
                        val chipBg = when {
                            matrixErrorMsg != null && isSelected -> Color(0xFFF9DEDC)
                            isSelected -> Color(0xFFD3E3FD)
                            else -> Color(0xFFE1E2EC)
                        }
                        val chipTextCol = when {
                            matrixErrorMsg != null && isSelected -> Color(0xFFB3261E)
                            isSelected -> Color(0xFF001D47)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected && matrixErrorMsg != null) Color(0xFFB3261E) else if (isSelected) Color(0xFF1A73E8) else Color(0xFF74747F),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(chipBg, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setActiveMatrixSlot(slot)
                                    viewModel.clearMatrixError()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "[$slot]",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = chipTextCol
                                )
                                Text(
                                    text = dims,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipTextCol.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (matrixErrorMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF9DEDC),
                        contentColor = Color(0xFFB3261E)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Column {
                            Text(
                                text = "Matrix Operation Alert",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = matrixErrorMsg ?: "Invalid operation.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { viewModel.clearMatrixError() }
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editor: Matrix [$activeSlot]",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                tempRows = activeRows
                                tempCols = activeCols
                                showDimDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("DIM", fontSize = 11.sp, color = Color(0xFF1E2421), fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        for (i in 0 until activeRows) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (j in 0 until activeCols) {
                                    val cellState = activeValues[i][j].collectAsState()
                                    OutlinedTextField(
                                        value = cellState.value,
                                        onValueChange = { activeValues[i][j].value = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("cell_slot_${activeSlot}_${i}_${j}"),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color(0xFF74747F),
                                            focusedBorderColor = Color(0xFF1A73E8),
                                            focusedContainerColor = Color(0xFF1A73E8).copy(alpha = 0.12f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.computeMatrixOperation("clear") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9DEDC)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CLR[$activeSlot]", color = Color(0xFFB3261E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.computeMatrixOperation("ans") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ANS[ ]", color = Color(0xFF1F2421), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        listOf("DEC", "FRAC", "SCI").forEach { mode ->
                            val isSel = currentMode == mode
                            Button(
                                onClick = { viewModel.setDisplayMode(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFFD3E3FD) else Color(0xFFFAFBFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color(0xFF1A73E8) else Color(0xFFE1E2EC),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(mode, color = if (isSel) Color(0xFF001D47) else Color(0xFF74747F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Basic Arithmetic (Core Ops)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Scalar Factor / Power (k / n):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = scalarFactor,
                        onValueChange = { viewModel.setScalarFactor(it) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(40.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFF74747F),
                            focusedBorderColor = Color(0xFF1A73E8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val basicOps = listOf(
                    Pair("A+B", "[A] + [B]"),
                    Pair("A-B", "[A] - [B]"),
                    Pair("A*B", "[A] × [B]"),
                    Pair("scalar*A", "k × [$activeSlot]"),
                    Pair("A/scalar", "[$activeSlot] ÷ k")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    basicOps.forEach { (opKey, label) ->
                        Button(
                            onClick = { viewModel.computeMatrixOperation(opKey) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E3FD)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 9.sp, color = Color(0xFF001D47), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Matrix Transformations & Properties",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                val transOps = listOf(
                    listOf(Pair("det", "det()"), Pair("inv", "inv()"), Pair("trans", "Tᵀ"), Pair("tr", "tr()")),
                    listOf(Pair("rank", "rank()"), Pair("ref", "ref()"), Pair("rref", "rref()"), Pair("span", "span()"))
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    transOps.forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowList.forEach { (opKey, label) ->
                                Button(
                                    onClick = { viewModel.computeMatrixOperation(opKey) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2D9FF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, color = Color(0xFF001D47), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Powers & Exponents",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pOps = listOf(
                        Pair("A2", "[$activeSlot]²"),
                        Pair("An", "[$activeSlot]ⁿ"),
                        Pair("A_neg_1", "[$activeSlot]⁻¹"),
                        Pair("A_neg_n", "[$activeSlot]⁻ⁿ")
                    )
                    pOps.forEach { (opKey, label) ->
                        Button(
                            onClick = { viewModel.computeMatrixOperation(opKey) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2D9FF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = Color(0xFF001D47), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Decompositions & Solvers (Advanced)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                val advOps = listOf(
                    listOf(Pair("LU", "LU"), Pair("QR", "QR"), Pair("SVD", "SVD"), Pair("eigen", "EIGEN")),
                    listOf(Pair("solve", "SOLVE [A]x=[B]"), Pair("cramer", "CRAMER [A]x=[B]"), Pair("sym", "SYM?"), Pair("orth", "ORTH?"))
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    advOps.forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowList.forEach { (opKey, label) ->
                                Button(
                                    onClick = { viewModel.computeMatrixOperation(opKey) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4EED0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 9.sp, color = Color(0xFF072711), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Manual Row Operations",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rᵢ:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = manualRowI,
                            onValueChange = { viewModel.setManualRowI(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Text("Rⱼ:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = manualRowJ,
                            onValueChange = { viewModel.setManualRowJ(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Text("k:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = manualScalarK,
                            onValueChange = { viewModel.setManualScalarK(it) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.computeMatrixOperation("row_swap") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Rᵢ ↔ Rⱼ", fontSize = 10.sp, color = Color(0xFF1E2421), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.computeMatrixOperation("row_scale") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("kRᵢ", fontSize = 10.sp, color = Color(0xFF1E2421), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.computeMatrixOperation("row_add_scaled") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Rⱼ + kRᵢ", fontSize = 10.sp, color = Color(0xFF1E2421), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.computeMatrixOperation("clear_all") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9DEDC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CLEAR ALL SLOTS (A, B, C)", color = Color(0xFFB3261E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFF1A73E8).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "MATRIX ALGEBRAIC OUTPUT",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = matrixResult.ifEmpty { "Configure Slots, select core ops, transformations or decomposition filters" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("matrix_output_result")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDimDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDimDialog = false }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DIM Selector — Matrix [$activeSlot]",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Rows: $tempRows",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        androidx.compose.material3.Slider(
                            value = tempRows.toFloat(),
                            onValueChange = { tempRows = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Columns: $tempCols",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        androidx.compose.material3.Slider(
                            value = tempCols.toFloat(),
                            onValueChange = { tempCols = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    Text(
                        text = "Grid Preview ($tempRows × $tempCols):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                        modifier = Modifier.padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (r in 0 until tempRows) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (c in 0 until tempCols) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDimDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2EC)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF1E2421))
                        }

                        Button(
                            onClick = {
                                if (activeSlot == "A") {
                                    viewModel.setMatrixDimensionA(tempRows, tempCols)
                                } else if (activeSlot == "B") {
                                    viewModel.setMatrixDimensionB(tempRows, tempCols)
                                } else {
                                    viewModel.setMatrixDimensionC(tempRows, tempCols)
                                }
                                showDimDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set Dimensions", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Graph2DView(viewModel: CalculatorViewModel) {
    val angleUnit by viewModel.angleUnit.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Graph2DComposable(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            angleUnit = angleUnit
        )
    }
}

@Composable
fun Graph3DView(viewModel: CalculatorViewModel) {
    val angleUnit by viewModel.angleUnit.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Graph3DComposable(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            angleUnit = angleUnit
        )
    }
}

@Composable
fun EducationalTutorialView() {
    val guidelines = listOf(
        Pair("📐 SCIENTIFIC COMP MODE", "Allows calculating complex formula operations. Double click Shift/Alpha to input operations.\n- Shift values: sin⁻¹ is asin(, cos⁻¹ is acos( and tan⁻¹ is atan(.\n- Auto multiplication transforms '2pi' into implicit multiplier math.\n- Press RAD/DEG button top-right to swap angles modes."),
        Pair("🔢 EXTREME MATRIX MULTI-SOLVER", "Provides linear numerical solver for dimensions up to 4x4. Enter matrix cell figures then tap computations:\n- A+B or A-B: Calculates sum/sub.\n- A*B: Multiplies rows/cols.\n- det(A): Expand determinant.\n- Inv A: Invert square matrices."),
        Pair("📈 INTERACTIVE 2D VISUAL PLOTTER", "Draft trigonometric equations in Cartesian coordinates plane.\n- Type: sin(x) or cos(2x)^2 and press plot.\n- Zoom Limit buttons change viewport grid dynamically.\n- Touch or tap inside plot to highlight local cartesian values on screen!"),
        Pair("🧊 RETRO-MODERN Isometric 3D RENDER", "Draw three-dimensional algebraic shapes by typing z = f(x, y).\n- Enter: e^(-(x^2+y^2)) or sin(x)*cos(y).\n- Use rotational Pitch & Yaw sliders below to rotate 3D mesh wireframe dynamically!")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "STUDENT MASTERY MANUAL",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(guidelines) { (title, text) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E242C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), lineHeight = 18.sp)
                }
            }
        }
    }
}
