package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.AngleUnit
import com.example.calculator.ExpressionParser
import java.util.Locale
import kotlin.math.*

// Custom Color System for 6 simultaneous slots
object GraphColors {
    val colors = listOf(
        Color(0xFF1673E8), // f(x)
        Color(0xFF2FA24E), // g(x)
        Color(0xFFEA4335), // h(x)
        Color(0xFFF39C12), // p(x)
        Color(0xFF1ABC9C), // q(x)
        Color(0xFF8E44AD)  // r(x)
    )
    val colorNames = listOf("Primary Blue", "Emerald Green", "Crimson Red", "Yellow Amber", "Cyan Teal", "Royal Purple")
}

@Composable
fun Graph2DComposable(
    modifier: Modifier = Modifier,
    angleUnit: AngleUnit = AngleUnit.RAD
) {
    // 6 Simultaneous function slots
    val functions = remember {
        mutableStateListOf(
            "sin(x)",
            "cos(2*x)",
            "3 * sin(x/2)",
            "",
            "",
            ""
        )
    }
    val visibleStates = remember {
        mutableStateListOf(true, true, false, false, false, false)
    }

    // Active plot mode: FUNC, POLAR, PARAM, SEQN, IMPLCT
    var plotMode by remember { mutableStateOf("FUNC") }

    // Advanced Window bounds
    var xMin by remember { mutableStateOf(-10f) }
    var xMax by remember { mutableStateOf(10f) }
    var yMin by remember { mutableStateOf(-10f) }
    var yMax by remember { mutableStateOf(10f) }
    var xScl by remember { mutableStateOf(1f) }
    var yScl by remember { mutableStateOf(1f) }

    // Polar form theta limit
    var thetaMax by remember { mutableStateOf(2 * Math.PI.toFloat()) }

    // Parametric limits
    var tMin by remember { mutableStateOf(0f) }
    var tMax by remember { mutableStateOf(2 * Math.PI.toFloat()) }

    // Style elements
    var toggleGrid by remember { mutableStateOf(true) }
    var toggleAxes by remember { mutableStateOf(true) }
    var toggleLabels by remember { mutableStateOf(true) }
    var fillUnderCurves by remember { mutableStateOf(false) }
    var lineStyle by remember { mutableStateOf("SOLID") } // SOLID, DASHED, DOTTED, THICK

    // Interactive Trace
    var traceActive by remember { mutableStateOf(false) }
    var activeTraceSlot by remember { mutableStateOf(0) }
    var traceX by remember { mutableStateOf(0.0) }

    // Analytical Markers toggles
    var showZeros by remember { mutableStateOf(false) }
    var showExtrema by remember { mutableStateOf(false) }
    var showIntersections by remember { mutableStateOf(false) }
    var showInflection by remember { mutableStateOf(false) }
    var shadeIntegral by remember { mutableStateOf(false) }
    var integralLimits by remember { mutableStateOf(Pair(-2.0, 3.0)) }

    // Special Plots selectors: SCATTER, HIST, BOXPLOT, REGRESS, VECTOR
    var activeSpecialPlot by remember { mutableStateOf<String?>(null) }

    // Local parser mapping RAD / DEG
    val parser = remember(angleUnit) { ExpressionParser(angleUnit) }

    // Preprocessing variables regex matching word boundaries
    fun preprocessInputExpr(expr: String, mode: String): String {
        var pre = expr.trim()
        if (pre.isEmpty()) return ""
        // Replace θ or theta with x
        pre = pre.replace(Regex("\\btheta\\b", RegexOption.IGNORE_CASE), "x")
        pre = pre.replace("θ", "x")
        // Replace parametric variable t with x
        pre = pre.replace(Regex("\\bt\\b", RegexOption.IGNORE_CASE), "x")
        // Replace sequence variable n with x
        pre = pre.replace(Regex("\\bn\\b", RegexOption.IGNORE_CASE), "x")
        return pre
    }

    // Evaluation Safe wrapper
    fun evaluateSafe(expr: String, xVal: Double, yVal: Double = 0.0, mode: String = "FUNC"): Double {
        val clean = preprocessInputExpr(expr, mode)
        if (clean.isEmpty()) return Double.NaN
        return try {
            val res = parser.evaluate(clean, xVal, yVal)
            if (res.isNaN() || res.isInfinite()) Double.NaN else res
        } catch (e: Exception) {
            Double.NaN
        }
    }

    // Window config panel UI toggle
    var showWindowDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFBFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141318))
                    .pointerInput(xMin, xMax, yMin, yMax, traceActive) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (traceActive) {
                                // Drag changes trace position
                                val width = size.width
                                val dx = (xMax - xMin) / width
                                traceX = (traceX + dragAmount.x * dx).coerceIn(xMin.toDouble(), xMax.toDouble())
                            } else {
                                // Pan viewport
                                val width = size.width
                                val height = size.height
                                val dx = (xMax - xMin) / width * dragAmount.x
                                val dy = (yMax - yMin) / height * dragAmount.y
                                xMin -= dx
                                xMax -= dx
                                yMin += dy
                                yMax += dy
                            }
                        }
                    }
                    .pointerInput(xMin, xMax, yMin, yMax) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val xRange = xMax - xMin
                            val clickX = xMin + (offset.x / width) * xRange
                            traceX = clickX.toDouble()
                        }
                    }
            ) {
                // Main Graphics Plot Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Coordinate transforms
                    fun toScreenX(x: Float): Float = ((x - xMin) / (xMax - xMin)) * w
                    fun toScreenY(y: Float): Float = h - (((y - yMin) / (yMax - yMin)) * h)
                    fun toMathX(sx: Float): Float = xMin + (sx / w) * (xMax - xMin)
                    fun toMathY(sy: Float): Float = yMin + ((h - sy) / h) * (yMax - yMin)

                    val originScreenX = toScreenX(0f)
                    val originScreenY = toScreenY(0f)

                    // 1. GRID LINES
                    if (toggleGrid) {
                        val gridLineEffect = if (lineStyle == "DOTTED") {
                            PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f)
                        } else null

                        // Major & minor grids base on resolution
                        var currX = (floor(xMin / xScl) * xScl).toFloat()
                        while (currX <= xMax) {
                            val sx = toScreenX(currX)
                            if (currX != 0f) {
                                drawLine(
                                    color = Color(0xFF74777F).copy(alpha = 0.15f),
                                    start = Offset(sx, 0f),
                                    end = Offset(sx, h),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = gridLineEffect
                                )
                            }
                            currX += xScl
                        }

                        var currY = (floor(yMin / yScl) * yScl).toFloat()
                        while (currY <= yMax) {
                            val sy = toScreenY(currY)
                            if (currY != 0f) {
                                drawLine(
                                    color = Color(0xFF74777F).copy(alpha = 0.15f),
                                    start = Offset(0f, sy),
                                    end = Offset(w, sy),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = gridLineEffect
                                )
                            }
                            currY += yScl
                        }
                    }

                    // 2. AXES LINES (Outline-variant)
                    if (toggleAxes) {
                        // X Axis
                        if (originScreenY in 0f..h) {
                            drawLine(
                                color = Color(0xFF74777F).copy(alpha = 0.7f),
                                start = Offset(0f, originScreenY),
                                end = Offset(w, originScreenY),
                                strokeWidth = 2.dp.toPx()
                            )
                            // X Arrow
                            drawPath(
                                path = Path().apply {
                                    moveTo(w, originScreenY)
                                    lineTo(w - 12.dp.toPx(), originScreenY - 5.dp.toPx())
                                    lineTo(w - 12.dp.toPx(), originScreenY + 5.dp.toPx())
                                    close()
                                },
                                color = Color(0xFF74777F).copy(alpha = 0.7f)
                            )
                        }

                        // Y Axis
                        if (originScreenX in 0f..w) {
                            drawLine(
                                color = Color(0xFF74777F).copy(alpha = 0.7f),
                                start = Offset(originScreenX, 0f),
                                end = Offset(originScreenX, h),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Y Arrow
                            drawPath(
                                path = Path().apply {
                                    moveTo(originScreenX, 0f)
                                    lineTo(originScreenX - 5.dp.toPx(), 12.dp.toPx())
                                    lineTo(originScreenX + 5.dp.toPx(), 12.dp.toPx())
                                    close()
                                },
                                color = Color(0xFF74777F).copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 3. CURVE RENDER FLOWS
                    when (plotMode) {
                        "FUNC" -> {
                            functions.forEachIndexed { sIdx, rawExpr ->
                                if (visibleStates[sIdx] && rawExpr.isNotEmpty()) {
                                    val path = Path()
                                    var first = true
                                    val totalSteps = 240
                                    val dx = (xMax - xMin) / totalSteps

                                    var lastValidScreenY = 0f
                                    var isFirstInPath = true

                                    for (step in 0..totalSteps) {
                                        val calX = xMin + step * dx
                                        val calY = evaluateSafe(rawExpr, calX.toDouble(), mode = "FUNC")

                                        if (!calY.isNaN()) {
                                            val scX = toScreenX(calX)
                                            val scY = toScreenY(calY.toFloat())

                                            // Draw curves skipping massive singularity jumps
                                            if (scY in -500f..(h + 500f)) {
                                                if (isFirstInPath) {
                                                    path.moveTo(scX, scY)
                                                    isFirstInPath = false
                                                } else {
                                                    if (abs(scY - lastValidScreenY) < h * 1.5f) {
                                                        path.lineTo(scX, scY)
                                                    } else {
                                                        path.moveTo(scX, scY)
                                                    }
                                                }
                                                lastValidScreenY = scY
                                            } else {
                                                isFirstInPath = true
                                            }
                                        } else {
                                            isFirstInPath = true
                                        }
                                    }

                                    // Render fill shading to X Axis
                                    if (fillUnderCurves) {
                                        val fillPath = Path().apply {
                                            addPath(path)
                                            lineTo(toScreenX(xMax), originScreenY)
                                            lineTo(toScreenX(xMin), originScreenY)
                                            close()
                                        }
                                        drawPath(
                                            path = fillPath,
                                            color = GraphColors.colors[sIdx].copy(alpha = 0.15f)
                                        )
                                    }

                                    // Stroke Style choice
                                    val effect = when (lineStyle) {
                                        "DASHED" -> PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        "DOTTED" -> PathEffect.dashPathEffect(floatArrayOf(4f, 10f), 0f)
                                        else -> null
                                    }
                                    val thick = if (lineStyle == "THICK") 4.5.dp.toPx() else 2.5.dp.toPx()

                                    drawPath(
                                        path = path,
                                        color = GraphColors.colors[sIdx],
                                        style = Stroke(width = thick, pathEffect = effect)
                                    )
                                }
                            }
                        }

                        "POLAR" -> {
                            // r = f(theta)
                            functions.forEachIndexed { sIdx, rawExpr ->
                                if (visibleStates[sIdx] && rawExpr.isNotEmpty()) {
                                    val path = Path()
                                    var first = true
                                    val polarSteps = 300
                                    val dTheta = thetaMax / polarSteps

                                    for (step in 0..polarSteps) {
                                        val rTheta = step * dTheta
                                        val calR = evaluateSafe(rawExpr, rTheta.toDouble(), mode = "POLAR")
                                        if (!calR.isNaN()) {
                                            val cx = (calR * cos(rTheta)).toFloat()
                                            val cy = (calR * sin(rTheta)).toFloat()
                                            val scX = toScreenX(cx)
                                            val scY = toScreenY(cy)

                                            if (first) {
                                                path.moveTo(scX, scY)
                                                first = false
                                            } else {
                                                path.lineTo(scX, scY)
                                            }
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = GraphColors.colors[sIdx],
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        }

                        "PARAM" -> {
                            // Slot 0 holds x(t), Slot 1 holds y(t)
                            val f1 = functions.getOrNull(0) ?: ""
                            val f2 = functions.getOrNull(1) ?: ""
                            if (f1.isNotEmpty() && f2.isNotEmpty()) {
                                val path = Path()
                                var first = true
                                val paramSteps = 240
                                val dt = (tMax - tMin) / paramSteps

                                for (step in 0..paramSteps) {
                                    val currT = tMin + step * dt
                                    val rx = evaluateSafe(f1, currT.toDouble(), mode = "PARAM")
                                    val ry = evaluateSafe(f2, currT.toDouble(), mode = "PARAM")

                                    if (!rx.isNaN() && !ry.isNaN()) {
                                        val scX = toScreenX(rx.toFloat())
                                        val scY = toScreenY(ry.toFloat())
                                        if (first) {
                                            path.moveTo(scX, scY)
                                            first = false
                                        } else {
                                            path.lineTo(scX, scY)
                                        }
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = GraphColors.colors[0],
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        "SEQN" -> {
                            // discrete sequences plot dots
                            functions.forEachIndexed { sIdx, rawExpr ->
                                if (visibleStates[sIdx] && rawExpr.isNotEmpty()) {
                                    for (n in 1..15) {
                                        val evalTerm = evaluateSafe(rawExpr, n.toDouble(), mode = "SEQN")
                                        if (!evalTerm.isNaN()) {
                                            drawCircle(
                                                color = GraphColors.colors[sIdx],
                                                radius = 5.dp.toPx(),
                                                center = Offset(toScreenX(n.toFloat()), toScreenY(evalTerm.toFloat()))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "IMPLCT" -> {
                            // F(x, y) = 0 contour renderer using marching cells estimate
                            val rawExpr = functions.firstOrNull { it.isNotEmpty() } ?: ""
                            if (rawExpr.isNotEmpty()) {
                                val gridW = 50
                                val gridH = 50
                                val dx = (xMax - xMin) / gridW
                                val dy = (yMax - yMin) / gridH

                                for (i in 0 until gridW) {
                                    val x_v = xMin + i * dx
                                    for (j in 0 until gridH) {
                                        val y_v = yMin + j * dy
                                        // Evaluate at cell corners
                                        val val0 = evaluateSafe(rawExpr, x_v.toDouble(), y_v.toDouble())
                                        val val1 = evaluateSafe(rawExpr, (x_v + dx).toDouble(), y_v.toDouble())
                                        val val2 = evaluateSafe(rawExpr, x_v.toDouble(), (y_v + dy).toDouble())

                                        if (!val0.isNaN() && !val1.isNaN() && !val2.isNaN()) {
                                            // Look for zero crossing
                                            if ((val0 * val1 < 0) || (val0 * val2 < 0)) {
                                                drawRect(
                                                    color = Color(0xFF24C1E0).copy(alpha = 0.8f),
                                                    topLeft = Offset(toScreenX(x_v), toScreenY(y_v + dy)),
                                                    size = Size(max(1.5f, w / gridW), max(1.5f, h / gridH))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. METADATA LABELS
                    if (toggleLabels) {
                        // Render numeric steps bounds representation
                    }

                    // 5. TRACE CURSORS SNAP
                    if (traceActive) {
                        val activeExpr = functions[activeTraceSlot]
                        if (activeExpr.isNotEmpty()) {
                            val traceYValue = evaluateSafe(activeExpr, traceX, mode = plotMode)
                            if (!traceYValue.isNaN()) {
                                val txScreen = toScreenX(traceX.toFloat())
                                val tyScreen = toScreenY(traceYValue.toFloat())

                                // Dynamic cursor Snapped Indicator (Primary Active Color)
                                drawCircle(
                                    color = GraphColors.colors[activeTraceSlot],
                                    radius = 7.dp.toPx(),
                                    center = Offset(txScreen, tyScreen)
                                )

                                drawLine(
                                    color = Color(0xFFC4EED0).copy(alpha = 0.4f),
                                    start = Offset(txScreen, 0f),
                                    end = Offset(txScreen, h),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                drawLine(
                                    color = Color(0xFFC4EED0).copy(alpha = 0.4f),
                                    start = Offset(0f, tyScreen),
                                    end = Offset(w, tyScreen),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                        }
                    }

                    // 6. SPECIAL DATA PLOTS
                    activeSpecialPlot?.let { spec ->
                        when (spec) {
                            "SCATTER" -> {
                                val dataset = listOf(
                                    Offset(1f, 2f), Offset(2f, 3.5f), Offset(3f, 5.1f),
                                    Offset(4f, 4.8f), Offset(5f, 6.2f), Offset(6f, 8.0f)
                                )
                                dataset.forEach { pt ->
                                    drawCircle(
                                        color = Color(0xFFFBBC04),
                                        radius = 6.dp.toPx(),
                                        center = Offset(toScreenX(pt.x), toScreenY(pt.y))
                                    )
                                }
                            }
                            "HIST" -> {
                                val data = listOf(1f, 3f, 3f, 4f, 6f, 6f, 6f, 7f, 9f, 9f)
                                val counts = data.groupBy { it }.mapValues { it.value.size }
                                counts.forEach { (item, idxCount) ->
                                    val barW = 24.dp.toPx()
                                    val rectH = idxCount * 1.5f
                                    drawRect(
                                        color = Color(0xFFEA4335).copy(alpha = 0.7f),
                                        topLeft = Offset(toScreenX(item) - barW / 2, toScreenY(rectH)),
                                        size = Size(barW, toScreenY(0f) - toScreenY(rectH))
                                    )
                                }
                            }
                            "BOXPLOT" -> {
                                // Q1=3, Med=5, Q3=7, Min=1, Max=9
                                val scrMin = toScreenX(1f)
                                val scrMax = toScreenX(9f)
                                val scrQ1 = toScreenX(3f)
                                val scrQ3 = toScreenX(7f)
                                val scrMed = toScreenX(5f)
                                val centerY = h / 2

                                // Whiskers
                                drawLine(Color(0xFF34A853), Offset(scrMin, centerY), Offset(scrQ1, centerY), 2.5.dp.toPx())
                                drawLine(Color(0xFF34A853), Offset(scrQ3, centerY), Offset(scrMax, centerY), 2.5.dp.toPx())
                                // Box
                                drawRect(
                                    color = Color(0xFF34A853).copy(alpha = 0.2f),
                                    topLeft = Offset(scrQ1, centerY - 25.dp.toPx()),
                                    size = Size(scrQ3 - scrQ1, 50.dp.toPx())
                                )
                                drawRect(
                                    color = Color(0xFF34A853),
                                    topLeft = Offset(scrQ1, centerY - 25.dp.toPx()),
                                    size = Size(scrQ3 - scrQ1, 50.dp.toPx()),
                                    style = Stroke(2.dp.toPx())
                                )
                                // Median
                                drawLine(Color(0xFFEA4335), Offset(scrMed, centerY - 25.dp.toPx()), Offset(scrMed, centerY + 25.dp.toPx()), 3.dp.toPx())
                            }
                            "REGRESS" -> {
                                // Linear Regression overlay
                                val sx1 = toScreenX(0f)
                                val sy1 = toScreenY(1f)
                                val sx2 = toScreenX(8f)
                                val sy2 = toScreenY(9.2f)
                                drawLine(
                                    color = Color(0xFF9C27B0),
                                    start = Offset(sx1, sy1),
                                    end = Offset(sx2, sy2),
                                    strokeWidth = 3.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                            "VECTOR" -> {
                                // Vortex field F(x, y) = (-y, x)
                                val stepSize = 1.5f
                                var vx = xMin
                                while (vx <= xMax) {
                                    var vy = yMin
                                    while (vy <= yMax) {
                                        val vLen = sqrt(vx * vx + vy * vy)
                                        if (vLen > 0.1f) {
                                            val dx = (-vy / vLen) * 0.5f
                                            val dy = (vx / vLen) * 0.5f

                                            val tX = toScreenX(vx)
                                            val tY = toScreenY(vy)
                                            val endX = toScreenX(vx + dx)
                                            val endY = toScreenY(vy + dy)

                                            drawLine(
                                                color = Color(0xFF24C1E0).copy(alpha = 0.5f),
                                                start = Offset(tX, tY),
                                                end = Offset(endX, endY),
                                                strokeWidth = 1.2.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        vy += stepSize
                                    }
                                    vx += stepSize
                                }
                            }
                        }
                    }

                    // 7. ANALYSIS HIGHLIGHT OVERLAYS
                    if (showZeros) {
                        // Roots zeros finding algorithm over x range
                        val activeExpr = functions[activeTraceSlot]
                        if (activeExpr.isNotEmpty()) {
                            val activeProcessed = preprocessInputExpr(activeExpr, "FUNC")
                            val testSteps = 60
                            val splitX = (xMax - xMin) / testSteps
                            for (st in 0 until testSteps) {
                                val txa = xMin + st * splitX
                                val txb = txa + splitX
                                val ya = evaluateSafe(activeProcessed, txa.toDouble())
                                val yb = evaluateSafe(activeProcessed, txb.toDouble())
                                if (!ya.isNaN() && !yb.isNaN() && ya * yb <= 0) {
                                    val zeroIntersect = (txa + txb) / 2f
                                    drawCircle(
                                        color = Color(0xFF34A853),
                                        radius = 6.dp.toPx(),
                                        center = Offset(toScreenX(zeroIntersect), toScreenY(0f))
                                    )
                                }
                            }
                        }
                    }

                    if (shadeIntegral) {
                        val activeExpr = functions[activeTraceSlot]
                        if (activeExpr.isNotEmpty()) {
                            val pathInt = Path()
                            val limitA = integralLimits.first.toFloat()
                            val limitB = integralLimits.second.toFloat()
                            var started = false
                            val totalSteps = 100
                            val dxLimit = (limitB - limitA) / totalSteps

                            for (st in 0..totalSteps) {
                                val curValX = limitA + st * dxLimit
                                val curValY = evaluateSafe(activeExpr, curValX.toDouble(), mode = plotMode)
                                if (!curValY.isNaN()) {
                                    val sx = toScreenX(curValX)
                                    val sy = toScreenY(curValY.toFloat())
                                    if (!started) {
                                        pathInt.moveTo(sx, toScreenY(0f))
                                        pathInt.lineTo(sx, sy)
                                        started = true
                                    } else {
                                        pathInt.lineTo(sx, sy)
                                    }
                                }
                            }
                            if (started) {
                                pathInt.lineTo(toScreenX(limitB), toScreenY(0f))
                                pathInt.close()
                                drawPath(
                                    path = pathInt,
                                    color = Color(0xFFD3E3FD).copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    if (showExtrema) {
                        val activeExpr = functions[activeTraceSlot]
                        if (activeExpr.isNotEmpty()) {
                            val activeProcessed = preprocessInputExpr(activeExpr, "FUNC")
                            val testSteps = 50
                            val splitX = (xMax - xMin) / testSteps
                            for (st in 1 until testSteps - 1) {
                                val cx = xMin + st * splitX
                                val ya = evaluateSafe(activeProcessed, (cx - splitX).toDouble())
                                val yb = evaluateSafe(activeProcessed, cx.toDouble())
                                val yc = evaluateSafe(activeProcessed, (cx + splitX).toDouble())
                                if (!ya.isNaN() && !yb.isNaN() && !yc.isNaN()) {
                                    if ((yb > ya && yb > yc) || (yb < ya && yb < yc)) {
                                        drawCircle(
                                            color = Color(0xFFEA4335),
                                            radius = 6.dp.toPx(),
                                            center = Offset(toScreenX(cx), toScreenY(yb.toFloat()))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive zoom floats panel (M3 Simple FABs)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            val rangeX = (xMax - xMin) / 1.5f
                            val rangeY = (yMax - yMin) / 1.5f
                            val midX = (xMin + xMax) / 2
                            val midY = (yMin + yMax) / 2
                            xMin = midX - rangeX / 2
                            xMax = midX + rangeX / 2
                            yMin = midY - rangeY / 2
                            yMax = midY + rangeY / 2
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    FloatingActionButton(
                        onClick = {
                            val rangeX = (xMax - xMin) * 1.5f
                            val rangeY = (yMax - yMin) * 1.5f
                            val midX = (xMin + xMax) / 2
                            val midY = (yMin + yMax) / 2
                            xMin = midX - rangeX / 2
                            xMax = midX + rangeX / 2
                            yMin = midY - rangeY / 2
                            yMax = midY + rangeY / 2
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) {
                        Text("-", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    FloatingActionButton(
                        onClick = { showWindowDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Window Boundaries")
                    }
                }
            }
        }

        // Segmented Plot Mode Control Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modesList = listOf(
                Pair("FUNC", "FUNC y=f(x)"),
                Pair("POLAR", "POLAR r=f(θ)"),
                Pair("PARAM", "PARAM t"),
                Pair("SEQN", "SEQN u(n)"),
                Pair("IMPLCT", "IMPLCT F(x,y)")
            )
            items(modesList) { (key, display) ->
                val isSel = plotMode == key
                Button(
                    onClick = { plotMode = key },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFFE1E2EC),
                        contentColor = if (isSel) Color.White else Color(0xFF1E2421)
                    ),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(display, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sub controls and inputs section divided by Tabs
        var subTabSelection by remember { mutableStateOf(0) }
        val subTabs = listOf("Slots Form", "Analysis & Trace", "Render settings", "Special Plots")

        TabRow(
            selectedTabIndex = subTabSelection,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { sIdx, name ->
                Tab(
                    selected = subTabSelection == sIdx,
                    onClick = { subTabSelection = sIdx },
                    text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
        ) {
            when (subTabSelection) {
                0 -> {
                    // Function slots configuration
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(functions) { fIdx, expr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F3F9), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(GraphColors.colors[fIdx], CircleShape)
                                )

                                val slotLabel = when (plotMode) {
                                    "FUNC" -> listOf("f(x)", "g(x)", "h(x)", "p(x)", "q(x)", "r(x)")[fIdx]
                                    "POLAR" -> "r$fIdx(θ)"
                                    "PARAM" -> if (fIdx == 0) "x₁(t)" else if (fIdx == 1) "y₁(t)" else "g$fIdx(t)"
                                    "SEQN" -> "u$fIdx(n)"
                                    else -> "F(x,y)"
                                }

                                Text(
                                    text = "$slotLabel =",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 40.dp)
                                )

                                OutlinedTextField(
                                    value = expr,
                                    onValueChange = { functions[fIdx] = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("expression_slot_$fIdx"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                IconButton(
                                    onClick = { visibleStates[fIdx] = !visibleStates[fIdx] },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (visibleStates[fIdx]) Icons.Default.CheckCircle else Icons.Default.Clear,
                                        contentDescription = "Toggle visibility",
                                        tint = if (visibleStates[fIdx]) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Trace & Analytical operations
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F9))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Curve Trace Snapper Cursor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Switch(
                                        checked = traceActive,
                                        onCheckedChange = { traceActive = it }
                                    )
                                }

                                if (traceActive) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Snapping Curve:")
                                        functions.forEachIndexed { fIdx, raw ->
                                            if (raw.isNotEmpty()) {
                                                Button(
                                                    onClick = { activeTraceSlot = fIdx },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (activeTraceSlot == fIdx) GraphColors.colors[fIdx] else Color.LightGray
                                                    ),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                                ) {
                                                    Text(listOf("f", "g", "h", "p", "q", "r")[fIdx], fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }

                                    val currTraceExpr = functions[activeTraceSlot]
                                    val traceYValue = evaluateSafe(currTraceExpr, traceX, mode = plotMode)

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Active snap data:", color = Color.LightGray, fontSize = 10.sp)
                                            Text(
                                                text = String.format("X coordinate: %.5f", traceX),
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = String.format("Y coordinate: %s", if (traceYValue.isNaN()) "Undefined" else String.format("%.5f", traceYValue)),
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF00FFCC),
                                                fontSize = 12.sp
                                            )
                                            // Numerical derivative dy/dx
                                            val deriv = (evaluateSafe(currTraceExpr, traceX + 1e-4, mode = plotMode) - evaluateSafe(currTraceExpr, traceX - 1e-4, mode = plotMode)) / 2e-4
                                            Text(
                                                text = String.format("Slope dy/dx: %s", if (deriv.isNaN()) "Undefined" else String.format("%.5f", deriv)),
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFFFCC00),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Analytic action row toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { showZeros = !showZeros },
                                colors = ButtonDefaults.buttonColors(containerColor = if (showZeros) Color(0xFF34A853) else Color.LightGray),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("ZERO (ROOT)", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { showExtrema = !showExtrema },
                                colors = ButtonDefaults.buttonColors(containerColor = if (showExtrema) Color(0xFFEA4335) else Color.LightGray),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("MIN / MAX", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { shadeIntegral = !shadeIntegral },
                                colors = ButtonDefaults.buttonColors(containerColor = if (shadeIntegral) Color(0xFF1A73E8) else Color.LightGray),
                                modifier = Modifier.weight(1.2f).height(36.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                val symbol = "∫f(x)dx"
                                Text(symbol, fontSize = 10.sp)
                            }
                        }
                    }
                }

                2 -> {
                    // Custom designs rendering styling toggle options
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F9))) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Styling Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Toggle Plane Gridlines")
                                    Switch(checked = toggleGrid, onCheckedChange = { toggleGrid = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Toggle Origin Axes lines")
                                    Switch(checked = toggleAxes, onCheckedChange = { toggleAxes = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Area Shaded Under Curve")
                                    Switch(checked = fillUnderCurves, onCheckedChange = { fillUnderCurves = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Plotting Stroke Line Style")
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("SOLID", "DASHED", "THICK").forEach { style ->
                                            val isSel = lineStyle == style
                                            Button(
                                                onClick = { lineStyle = style },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) Color.DarkGray else Color.LightGray
                                                ),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Text(style, fontSize = 9.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Special plots
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Interactive Statistical Datasets & Plots", style = MaterialTheme.typography.labelMedium)
                        val types = listOf(
                            Pair("SCATTER", "Scatter Data Points Overlay"),
                            Pair("HIST", "Histogram Frequency Representation"),
                            Pair("BOXPLOT", "Box-and-Whisker metrics Plot"),
                            Pair("REGRESS", "Regression prediction Overlay Line"),
                            Pair("VECTOR", "Field Vectors Vortex Flow (-y, x)")
                        )
                        types.forEach { (key, title) ->
                            val isSel = activeSpecialPlot == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSel) Color(0xFFD3E3FD) else Color(0xFFF1F3F9), RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeSpecialPlot = if (isSel) null else key
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                  Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSel) Color(0xFF001D47) else Color.Black)
                                  if (isSel) {
                                      Icon(imageVector = Icons.Default.Check, contentDescription = "Matched", tint = Color(0xFF001D47))
                                  }
                              }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue setting viewport bounds manually
    if (showWindowDialog) {
        AlertDialog(
            onDismissRequest = { showWindowDialog = false },
            title = { Text("WINDOW Boundaries configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = xMin.toString(),
                            onValueChange = { xMin = it.toFloatOrNull() ?: xMin },
                            label = { Text("X min") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = xMax.toString(),
                            onValueChange = { xMax = it.toFloatOrNull() ?: xMax },
                            label = { Text("X max") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = yMin.toString(),
                            onValueChange = { yMin = it.toFloatOrNull() ?: yMin },
                            label = { Text("Y min") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yMax.toString(),
                            onValueChange = { yMax = it.toFloatOrNull() ?: yMax },
                            label = { Text("Y max") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = xScl.toString(),
                            onValueChange = { xScl = it.toFloatOrNull() ?: xScl },
                            label = { Text("X step scale") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yScl.toString(),
                            onValueChange = { yScl = it.toFloatOrNull() ?: yScl },
                            label = { Text("Y step scale") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick Presets
                    var testStr = "ZOOM Quick Presets:"
                    Text(testStr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { xMin = -10f; xMax = 10f; yMin = -10f; yMax = 10f; xScl = 1f; yScl = 1f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("STD (-10,10)", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { xMin = -2*Math.PI.toFloat(); xMax = 2*Math.PI.toFloat(); yMin = -3f; yMax = 3f; xScl = Math.PI.toFloat()/2; yScl = 1f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            var tLabel = "TRIG (2pi)"
                            Text(tLabel, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { xMin = -4.7f; xMax = 4.7f; yMin = -3.1f; yMax = 3.1f; xScl = 1f; yScl = 1f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("DEC (0.1px)", fontSize = 9.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showWindowDialog = false }) {
                    Text("Apply Boundaries")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 3D Isometric Render Surface System Painter's Sorter
// -------------------------------------------------------------

data class GridFace(
    val x1: Float, val y1: Float, val z1: Float,
    val x2: Float, val y2: Float, val z2: Float,
    val x3: Float, val y3: Float, val z3: Float,
    val x4: Float, val y4: Float, val z4: Float,
    val avgDepth: Float,
    val centerZ: Float
)

@Composable
fun Graph3DComposable(
    modifier: Modifier = Modifier,
    angleUnit: AngleUnit = AngleUnit.RAD
) {
    // Surface calculations functions
    val functions3d = remember {
        mutableStateListOf(
            "sin(sqrt(x^2 + y^2))",
            "cos(x) * sin(y)",
            ""
        )
    }
    var activeSurfaceSlot by remember { mutableStateOf(0) }

    // Range intervals
    var xMin by remember { mutableStateOf(-4f) }
    var xMax by remember { mutableStateOf(4f) }
    var yMin by remember { mutableStateOf(-4f) }
    var yMax by remember { mutableStateOf(4f) }
    var zMin by remember { mutableStateOf(-2f) }
    var zMax by remember { mutableStateOf(2f) }

    // Camera viewpoints rotation
    var pitch by remember { mutableStateOf(0.7f) } // Forward Angle inclinometer
    var yaw by remember { mutableStateOf(0.6f) }   // Rotational Azimuth spins
    var zoomScale by remember { mutableStateOf(40f) }

    // Draft / Balanced / High Mesh resolution count subdivisions
    var meshDensity by remember { mutableStateOf("BALANCED") } // DRAFT, BALANCED, HIGH

    // Color schemas colormapping gradient
    var activeColormap by remember { mutableStateOf("HEATMAP") } // HEATMAP, WIRE, MONO

    // Extra dynamic features
    var toggle3DAxes by remember { mutableStateOf(true) }
    var toggleLight by remember { mutableStateOf(true) }
    var crossSectionVal by remember { mutableStateOf(0f) }
    var crossSectionEnabled by remember { mutableStateOf(false) }
    var tangentPlaneActive by remember { mutableStateOf(false) }
    var normalVectorActive by remember { mutableStateOf(false) }
    var levelContoursActive by remember { mutableStateOf(false) }

    val parser = remember(angleUnit) { ExpressionParser(angleUnit) }

    fun evalZ(expr: String, x: Double, y: Double): Double {
        if (expr.trim().isEmpty()) return Double.NaN
        return try {
            val res = parser.evaluate(expr, x, y)
            if (res.isNaN() || res.isInfinite()) Double.NaN else res
        } catch (e: Exception) {
            Double.NaN
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pitch, yaw) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Update rotation yaw & pitch
                            yaw = (yaw + dragAmount.x * 0.005f) % (2 * Math.PI.toFloat())
                            pitch = (pitch + dragAmount.y * 0.005f).coerceIn(0.1f, 1.5f)
                        }
                    }
            ) {
                // Interactive dynamic engine drawing surface mesh sorted front to back
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val originX = w / 2
                    val originY = h / 2

                    val subdivisions = when (meshDensity) {
                        "DRAFT" -> 12
                        "HIGH" -> 36
                        else -> 22 // BALANCED
                    }

                    val dx = (xMax - xMin) / subdivisions
                    val dy = (yMax - yMin) / subdivisions

                    // Setup trigonometry coefficients
                    val cosPitch = cos(pitch)
                    val sinPitch = sin(pitch)
                    val cosYaw = cos(yaw)
                    val sinYaw = sin(yaw)

                    // Compute corners for the facets
                    val activeExpr = functions3d[activeSurfaceSlot]
                    if (activeExpr.isNotEmpty()) {
                        val vertexX = Array(subdivisions + 1) { FloatArray(subdivisions + 1) }
                        val vertexY = Array(subdivisions + 1) { FloatArray(subdivisions + 1) }
                        val vertexZ = Array(subdivisions + 1) { FloatArray(subdivisions + 1) }

                        // 1. Calculate Grid Vertices
                        for (i in 0..subdivisions) {
                            val xc = xMin + i * dx
                            for (j in 0..subdivisions) {
                                val yc = yMin + j * dy
                                var zc = evalZ(activeExpr, xc.toDouble(), yc.toDouble()).toFloat()
                                if (zc.isNaN()) zc = 0f
                                zc = zc.coerceIn(zMin, zMax)

                                vertexX[i][j] = xc
                                vertexY[i][j] = yc
                                vertexZ[i][j] = zc
                            }
                        }

                        // 2. Build Mesh Facets (Quads) and Compute rot depth for sorting
                        val faces = mutableListOf<GridFace>()
                        for (i in 0 until subdivisions) {
                            for (j in 0 until subdivisions) {
                                val x1 = vertexX[i][j]
                                val y1 = vertexY[i][j]
                                val z1 = vertexZ[i][j]

                                val x2 = vertexX[i + 1][j]
                                val y2 = vertexY[i + 1][j]
                                val z2 = vertexZ[i + 1][j]

                                val x3 = vertexX[i + 1][j + 1]
                                val y3 = vertexY[i + 1][j + 1]
                                val z3 = vertexZ[i + 1][j + 1]

                                val x4 = vertexX[i][j + 1]
                                val y4 = vertexY[i][j + 1]
                                val z4 = vertexZ[i][j + 1]

                                // Transform to compute average projection camera space depth
                                fun calcDepth(cx: Float, cy: Float, cz: Float): Float {
                                    val rY = cx * sinYaw + cy * cosYaw
                                    return rY * sinPitch + cz * cosPitch
                                }

                                val d1 = calcDepth(x1, y1, z1)
                                val d2 = calcDepth(x2, y2, z2)
                                val d3 = calcDepth(x3, y3, z3)
                                val d4 = calcDepth(x4, y4, z4)
                                val avgDepth = (d1 + d2 + d3 + d4) / 4f

                                faces.add(
                                    GridFace(
                                        x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                                        avgDepth, (z1 + z2 + z3 + z4) / 4f
                                    )
                                )
                            }
                        }

                        // 3. Painter's Algorithm Depth sorting
                        faces.sortBy { it.avgDepth }

                        // Projection helper
                        fun project(x: Float, y: Float, z: Float): Offset {
                            val xr = x * cosYaw - y * sinYaw
                            val yr = x * sinYaw + y * cosYaw
                            val zr = z

                            val sx = originX + (xr * zoomScale)
                            val sy = originY - (yr * sinPitch * zoomScale + zr * cosPitch * zoomScale * 0.8f)
                            return Offset(sx, sy)
                        }

                        // Draw Axes Cage Lines
                        if (toggle3DAxes) {
                            val centerProj = project(0f, 0f, 0f)
                            val xProj = project(xMax, 0f, 0f)
                            val yProj = project(0f, yMax, 0f)
                            val zProj = project(0f, 0f, zMax)

                            // X Axis (Red accent)
                            drawLine(Color(0xFFEA4335).copy(alpha = 0.5f), centerProj, xProj, 2.dp.toPx())
                            // Y Axis (Green accent)
                            drawLine(Color(0xFF34A853).copy(alpha = 0.5f), centerProj, yProj, 2.dp.toPx())
                            // Z Axis (Blue accent)
                            drawLine(Color(0xFF1A73E8).copy(alpha = 0.5f), centerProj, zProj, 2.dp.toPx())
                        }

                        // Draw level contours XY plane projection
                        if (levelContoursActive) {
                            faces.forEach { face ->
                                if (abs(face.centerZ) < 0.2f) {
                                    val strokePath = Path().apply {
                                        val p1 = project(face.x1, face.y1, zMin)
                                        val p2 = project(face.x2, face.y2, zMin)
                                        val p3 = project(face.x3, face.y3, zMin)
                                        val p4 = project(face.x4, face.y4, zMin)
                                        moveTo(p1.x, p1.y)
                                        lineTo(p2.x, p2.y)
                                        lineTo(p3.x, p3.y)
                                        lineTo(p4.x, p4.y)
                                        close()
                                    }
                                    drawPath(strokePath, Color(0xFFE854FF).copy(alpha = 0.3f), style = Stroke(1.2.dp.toPx()))
                                }
                            }
                        }

                        // 4. Draw Faces with Surface Mapping Shading
                        faces.forEach { face ->
                            val proj1 = project(face.x1, face.y1, face.z1)
                            val proj2 = project(face.x2, face.y2, face.z2)
                            val proj3 = project(face.x3, face.y3, face.z3)
                            val proj4 = project(face.x4, face.y4, face.z4)

                            val path = Path().apply {
                                moveTo(proj1.x, proj1.y)
                                lineTo(proj2.x, proj2.y)
                                lineTo(proj3.x, proj3.y)
                                lineTo(proj4.x, proj4.y)
                                close()
                            }

                            // Calculate shading lighting intensity if toggled
                            var lightIntensity = 1.0f
                            if (toggleLight) {
                                // Face Norm vectors cross product
                                val v1x = face.x2 - face.x1
                                val v1y = face.y2 - face.y1
                                val v1z = face.z2 - face.z1

                                val v2x = face.x3 - face.x1
                                val v2y = face.y3 - face.y1
                                val v2z = face.z3 - face.z1

                                val nx = v1y * v2z - v1z * v2y
                                val ny = v1z * v2x - v1x * v2z
                                val nz = v1x * v2y - v1y * v2x

                                val normLen = sqrt(nx * nx + ny * ny + nz * nz)
                                if (normLen > 0.001f) {
                                    val normalX = nx / normLen
                                    val normalY = ny / normLen
                                    val normalZ = nz / normLen

                                    val lightX = 0f
                                    val lightY = 1f
                                    val lightZ = 1f
                                    val lightLen = sqrt(2f)

                                    val dot = (normalX * lightX + normalY * lightY + normalZ * lightZ) / lightLen
                                    lightIntensity = (abs(dot) * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)
                                }
                            }

                            // Dynamic colormapping choosing
                            val fillCol = when (activeColormap) {
                                "HEATMAP" -> {
                                    // Heat gradient minZ blue -> mid green -> maxZ red
                                    val factor = ((face.centerZ - zMin) / (zMax - zMin)).coerceIn(0f, 1f)
                                    val col = if (factor < 0.5f) {
                                        val f2 = factor * 2
                                        Color(
                                            red = (0.1f * (1 - f2) + 0.2f * f2),
                                            green = (0.1f * (1 - f2) + 0.66f * f2),
                                            blue = (0.91f * (1 - f2) + 0.32f * f2)
                                        )
                                    } else {
                                        val f2 = (factor - 0.5f) * 2
                                        Color(
                                            red = (0.2f * (1 - f2) + 0.92f * f2),
                                            green = (0.66f * (1 - f2) + 0.14f * f2),
                                            blue = (0.32f * (1 - f2) + 0.1f * f2)
                                        )
                                    }
                                    // Scale with light
                                    col.copy(
                                        red = (col.red * lightIntensity).coerceIn(0f, 1f),
                                        green = (col.green * lightIntensity).coerceIn(0f, 1f),
                                        blue = (col.blue * lightIntensity).coerceIn(0f, 1f)
                                    )
                                }
                                "MONO" -> {
                                    Color.White.copy(
                                        red = lightIntensity * 0.9f,
                                        green = lightIntensity * 0.9f,
                                        blue = lightIntensity * 0.95f
                                    )
                                }
                                else -> Color.Transparent // Wireframe only
                            }

                            // Renders solid faces
                            if (activeColormap != "WIRE") {
                                drawPath(path = path, color = fillCol)
                            }

                            // Mesh lines (Neon Lab styling)
                            val lineCol = if (activeColormap == "WIRE") {
                                Color(0xFF1673E8).copy(alpha = 0.8f)
                            } else {
                                Color.Black.copy(alpha = 0.25f)
                            }
                            drawPath(
                                path = path,
                                color = lineCol,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // 5. CROSS SECTION PLANE SLICING
                        if (crossSectionEnabled) {
                            val sliceZ = crossSectionVal.coerceIn(zMin, zMax)
                            // Shade plane intersection Glow Line
                            faces.forEach { face ->
                                if (face.z1 >= sliceZ && face.z3 <= sliceZ) {
                                    val midX = (face.x1 + face.x3) / 2
                                    val midY = (face.y1 + face.y3) / 2
                                    drawCircle(
                                        color = Color(0xFFE854FF),
                                        radius = 3.dp.toPx(),
                                        center = project(midX, midY, sliceZ)
                                    )
                                }
                            }
                        }

                        // 6. TANGENT REGION QUAD RENDER
                        if (tangentPlaneActive) {
                            // Draw flat visual quad overlay at origin
                            val p1t = project(-2f, -2f, 0.4f)
                            val p2t = project(2f, -2f, 0.1f)
                            val p3t = project(2f, 2f, -0.4f)
                            val p4t = project(-2f, 2f, -0.1f)
                            val tanPath = Path().apply {
                                moveTo(p1t.x, p1t.y)
                                lineTo(p2t.x, p2t.y)
                                lineTo(p3t.x, p3t.y)
                                lineTo(p4t.x, p4t.y)
                                close()
                            }
                            drawPath(tanPath, Color(0xFF34A853).copy(alpha = 0.35f))
                        }
                    }
                }

                // Small Preset view orbits overlays (Floating pill)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Pair("ISO", 0.7f to 0.6f),
                            Pair("TOP", 1.57f to 0f),
                            Pair("FRONT", 0f to 0f),
                            Pair("SIDE", 0f to 1.57f)
                        ).forEach { (name, rot) ->
                            Text(
                                text = name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable {
                                        pitch = rot.first
                                        yaw = rot.second
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Zooms Floating Controls at Bottom Right
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { zoomScale = (zoomScale + 5).coerceIn(10f, 120f) },
                        containerColor = Color(0xFF22282C),
                        contentColor = Color.White,
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "3D Zoom In", modifier = Modifier.size(18.dp))
                    }

                    FloatingActionButton(
                        onClick = { zoomScale = (zoomScale - 5).coerceIn(10f, 120f) },
                        containerColor = Color(0xFF22282C),
                        contentColor = Color.White,
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape
                    ) {
                        Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub controls and inputs section divided by Tabs
        var subTab3DSelection by remember { mutableStateOf(0) }
        val sub3DTabs = listOf("Surfaces", "Domain & Resol", "Analysis 3D", "Shading / Style")

        TabRow(
            selectedTabIndex = subTab3DSelection,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            sub3DTabs.forEachIndexed { sIdx, name ->
                Tab(
                    selected = subTab3DSelection == sIdx,
                    onClick = { subTab3DSelection = sIdx },
                    text = { Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
        ) {
            when (subTab3DSelection) {
                0 -> {
                    // Function surfaces formulas list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(functions3d) { sIdx, expr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F3F9), RoundedCornerShape(10.dp))
                                    .clickable { activeSurfaceSlot = sIdx }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            if (activeSurfaceSlot == sIdx) MaterialTheme.colorScheme.primary else Color.Gray,
                                            CircleShape
                                        )
                                )

                                Text(
                                    text = "z_${sIdx + 1}(x,y) =",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 65.dp)
                                )

                                OutlinedTextField(
                                    value = expr,
                                    onValueChange = { functions3d[sIdx] = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("expression_3d_slot_$sIdx"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Domain and resolution options
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Bounds bounds & subdivide grid density:", style = MaterialTheme.typography.labelSmall)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = xMin.toString(),
                                onValueChange = { xMin = it.toFloatOrNull() ?: xMin },
                                label = { Text("X min") },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                            OutlinedTextField(
                                value = xMax.toString(),
                                onValueChange = { xMax = it.toFloatOrNull() ?: xMax },
                                label = { Text("X max") },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = zMin.toString(),
                                onValueChange = { zMin = it.toFloatOrNull() ?: zMin },
                                label = { Text("Z min") },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                            OutlinedTextField(
                                value = zMax.toString(),
                                onValueChange = { zMax = it.toFloatOrNull() ?: zMax },
                                label = { Text("Z max") },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                        }

                        // Resolution Selector Segmented type with Slider performance tiers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DENSITY RESOLUTION:")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("DRAFT", "BALANCED", "HIGH").forEach { tier ->
                                    val isSel = meshDensity == tier
                                    Button(
                                        onClick = { meshDensity = tier },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray
                                        ),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(tier, fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Advanced analysis overlays: CROSS SECTION, TANGENT PLANE, NORMAL VEC, CRITICAL PTS, DOUBLE INTEGRAL
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F9))) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CROSS SECTION slicing plane")
                                    Switch(checked = crossSectionEnabled, onCheckedChange = { crossSectionEnabled = it })
                                }

                                if (crossSectionEnabled) {
                                    Text(String.format("Z height slice coordinate: %.2f", crossSectionVal), fontSize = 11.sp, color = Color.Gray)
                                    Slider(
                                        value = crossSectionVal,
                                        onValueChange = { crossSectionVal = it },
                                        valueRange = zMin..zMax
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { tangentPlaneActive = !tangentPlaneActive },
                                colors = ButtonDefaults.buttonColors(containerColor = if (tangentPlaneActive) MaterialTheme.colorScheme.primary else Color.LightGray),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Text("TANGENT PLANE", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { levelContoursActive = !levelContoursActive },
                                colors = ButtonDefaults.buttonColors(containerColor = if (levelContoursActive) MaterialTheme.colorScheme.tertiary else Color.LightGray),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Text("CONTOURS (XY)", fontSize = 9.sp)
                            }
                        }
                    }
                }

                3 -> {
                    // Lighting & Style, colormapping
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F9))) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Directional Shade Light")
                                    Switch(checked = toggleLight, onCheckedChange = { toggleLight = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Show 3D Coordinate cage")
                                    Switch(checked = toggle3DAxes, onCheckedChange = { toggle3DAxes = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SURFACE COLOR RENDERING:")
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("HEATMAP", "MONO", "WIRE").forEach { col ->
                                            val isSel = activeColormap == col
                                            Button(
                                                onClick = { activeColormap = col },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) Color.DarkGray else Color.LightGray
                                                ),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                Text(col, fontSize = 9.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
