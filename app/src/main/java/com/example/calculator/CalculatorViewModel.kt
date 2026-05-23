package com.example.calculator

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CalculatorMode {
    COMP, MATRIX, GRAPH_2D, GRAPH_3D, TUTORIAL
}

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(database.historyDao())

    val historyState: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentMode = MutableStateFlow(CalculatorMode.COMP)
    val currentMode = _currentMode.asStateFlow()

    private val _isShiftActive = MutableStateFlow(false)
    val isShiftActive = _isShiftActive.asStateFlow()

    private val _isAlphaActive = MutableStateFlow(false)
    val isAlphaActive = _isAlphaActive.asStateFlow()

    private val _angleUnit = MutableStateFlow(AngleUnit.RAD)
    val angleUnit = _angleUnit.asStateFlow()

    private val _memoryValue = MutableStateFlow(0.0)
    val memoryValue = _memoryValue.asStateFlow()

    private val _ansValue = MutableStateFlow(0.0)
    val ansValue = _ansValue.asStateFlow()

    private val _expressionInput = MutableStateFlow("")
    val expressionInput = _expressionInput.asStateFlow()

    private val _evaluationResult = MutableStateFlow("")
    val evaluationResult = _evaluationResult.asStateFlow()

    private val _matrixRowsA = MutableStateFlow(3)
    val matrixRowsA = _matrixRowsA.asStateFlow()
    private val _matrixColsA = MutableStateFlow(3)
    val matrixColsA = _matrixColsA.asStateFlow()

    private val _matrixRowsB = MutableStateFlow(3)
    val matrixRowsB = _matrixRowsB.asStateFlow()
    private val _matrixColsB = MutableStateFlow(3)
    val matrixColsB = _matrixColsB.asStateFlow()

    private val _matrixRowsC = MutableStateFlow(3)
    val matrixRowsC = _matrixRowsC.asStateFlow()
    private val _matrixColsC = MutableStateFlow(3)
    val matrixColsC = _matrixColsC.asStateFlow()

    val matrixValuesA = Array(5) { Array(5) { MutableStateFlow("0") } }
    val matrixValuesB = Array(5) { Array(5) { MutableStateFlow("0") } }
    val matrixValuesC = Array(5) { Array(5) { MutableStateFlow("0") } }

    private val _activeMatrixSlot = MutableStateFlow("A")
    val activeMatrixSlot = _activeMatrixSlot.asStateFlow()

    private val _displayMode = MutableStateFlow("DEC") // "DEC", "FRAC", "SCI"
    val displayMode = _displayMode.asStateFlow()

    private val _scalarFactor = MutableStateFlow("2.0")
    val scalarFactor = _scalarFactor.asStateFlow()

    private val _manualRowI = MutableStateFlow("1")
    val manualRowI = _manualRowI.asStateFlow()

    private val _manualRowJ = MutableStateFlow("2")
    val manualRowJ = _manualRowJ.asStateFlow()

    private val _manualScalarK = MutableStateFlow("2.0")
    val manualScalarK = _manualScalarK.asStateFlow()

    private val _matrixErrorMsg = MutableStateFlow<String?>(null)
    val matrixErrorMsg = _matrixErrorMsg.asStateFlow()

    private var lastMatrixAns: Matrix? = null
    private var lastMatrixComputed: Matrix? = null
    private var lastMatrixOpLabel: String = ""

    private val _matrixResultText = MutableStateFlow("")
    val matrixResultText = _matrixResultText.asStateFlow()

    private val _expression2D = MutableStateFlow("sin(x)")
    val expression2D = _expression2D.asStateFlow()

    private val _expression3D = MutableStateFlow("sin(sqrt(x^2 + y^2))")
    val expression3D = _expression3D.asStateFlow()

    private val _cursorValue2D = MutableStateFlow<Pair<Double, Double>?>(null)
    val cursorValue2D = _cursorValue2D.asStateFlow()

    init {
        resetMatrixA()
        resetMatrixB()
        resetMatrixC()
    }

    fun setMode(mode: CalculatorMode) {
        _currentMode.value = mode
    }

    fun toggleShift() {
        _isShiftActive.value = !_isShiftActive.value
        if (_isShiftActive.value) _isAlphaActive.value = false
    }

    fun toggleAlpha() {
        _isAlphaActive.value = !_isAlphaActive.value
        if (_isAlphaActive.value) _isShiftActive.value = false
    }

    fun toggleAngleUnit() {
        _angleUnit.value = when (_angleUnit.value) {
            AngleUnit.RAD -> AngleUnit.DEG
            AngleUnit.DEG -> AngleUnit.GRAD
            AngleUnit.GRAD -> AngleUnit.RAD
        }
    }

    fun setAngleUnit(unit: AngleUnit) {
        _angleUnit.value = unit
    }

    fun onKeyPressed(key: String) {
        var consumedModifiers = false
        val isShift = _isShiftActive.value
        val isAlpha = _isAlphaActive.value

        when (key) {
            "AC" -> {
                _expressionInput.value = ""
                _evaluationResult.value = ""
            }
            "DEL" -> {
                val current = _expressionInput.value
                if (current.isNotEmpty()) {
                    _expressionInput.value = current.substring(0, current.length - 1)
                }
            }
            "SHIFT" -> {
                toggleShift()
                consumedModifiers = true
            }
            "ALPHA" -> {
                toggleAlpha()
                consumedModifiers = true
            }
            "MODE" -> {
                // Done on click listeners in toolbar menu
            }
            "=" -> {
                evaluateExpression()
            }
            // Memory operations
            "MC" -> {
                _memoryValue.value = 0.0
                _evaluationResult.value = "Memory Cleared"
            }
            "MR" -> {
                _expressionInput.value += "m"
            }
            "MS" -> {
                val d = evaluateSilent(_expressionInput.value)
                _memoryValue.value = d
                _evaluationResult.value = "MS = " + formatVal(d)
                _expressionInput.value = ""
            }
            "M+" -> {
                val d = evaluateSilent(_expressionInput.value)
                _memoryValue.value += d
                _evaluationResult.value = "M+ = " + formatVal(_memoryValue.value)
                _expressionInput.value = ""
            }
            "M-" -> {
                val d = evaluateSilent(_expressionInput.value)
                _memoryValue.value -= d
                _evaluationResult.value = "M- = " + formatVal(_memoryValue.value)
                _expressionInput.value = ""
            }
            else -> {
                var insertStr = when (key) {
                    "sin" -> {
                        if (isShift && isAlpha) "asinh("
                        else if (isShift) "asin("
                        else if (isAlpha) "sinh("
                        else "sin("
                    }
                    "cos" -> {
                        if (isShift && isAlpha) "acosh("
                        else if (isShift) "acos("
                        else if (isAlpha) "cosh("
                        else "cos("
                    }
                    "tan" -> {
                        if (isShift && isAlpha) "atanh("
                        else if (isShift) "atan("
                        else if (isAlpha) "tanh("
                        else "tan("
                    }
                    "ln" -> {
                        if (isShift) "e^("
                        else if (isAlpha) "e"
                        else "ln("
                    }
                    "log" -> {
                        if (isShift) "10^("
                        else "log("
                    }
                    "log2" -> {
                        if (isShift) "logn("
                        else "log2("
                    }
                    "^" -> {
                        if (isShift) "nrt("
                        else "^"
                    }
                    "sqrt" -> {
                        if (isShift) "cbrt("
                        else "sqrt("
                    }
                    "x²" -> {
                        if (isShift) "^3"
                        else "^2"
                    }
                    "1/x" -> {
                        if (isShift) "fact("
                        else "^-1"
                    }
                    "(" -> {
                        if (isShift) "npr("
                        else "("
                    }
                    ")" -> {
                        if (isShift) "ncr("
                        else ")"
                    }
                    "pi" -> {
                        if (isShift) "phi"
                        else "pi"
                    }
                    "e" -> {
                        if (isShift) "c"
                        else "e"
                    }
                    "Ans" -> {
                        if (isShift) "round("
                        else "ans"
                    }
                    "EXP" -> {
                        if (isShift) "mod("
                        else "*10^"
                    }
                    "%" -> {
                        if (isShift) "" // handled below
                        else "mod("
                    }
                    "±" -> {
                        "" // handled below
                    }
                    "floor" -> {
                        if (isShift) "ceil("
                        else "floor("
                    }
                    "round" -> {
                        if (isShift) {
                            String.format(Locale.US, "%.5f", Math.random())
                        } else {
                            "round("
                        }
                    }
                    "mod" -> "mod("
                    "abs" -> "abs("
                    "nPr" -> "npr("
                    "nCr" -> "ncr("
                    "CE", "C" -> {
                        _expressionInput.value = ""
                        ""
                    }
                    else -> key
                }

                if ((key == "%" && isShift) || key == "±") {
                    val curr = _expressionInput.value
                    if (curr.startsWith("-(")) {
                        _expressionInput.value = curr.removePrefix("-(").removeSuffix(")")
                    } else if (curr.isNotEmpty()) {
                        _expressionInput.value = "-($curr)"
                    } else {
                        _expressionInput.value = "-"
                    }
                } else if (insertStr.isNotEmpty()) {
                    _expressionInput.value += insertStr
                }
                consumedModifiers = true
            }
        }

        if (!consumedModifiers) {
            _isShiftActive.value = false
            _isAlphaActive.value = false
        }
    }

    private fun evaluateSilent(expr: String): Double {
        if (expr.isBlank()) return 0.0
        return try {
            val parser = ExpressionParser(_angleUnit.value, ansVal = _ansValue.value, memVal = _memoryValue.value)
            parser.evaluate(expr)
        } catch (e: Exception) {
            0.0
        }
    }

    fun formatVal(resultVal: Double): String {
        return if (resultVal.isInfinite() || resultVal.isNaN()) {
            "Math ERROR"
        } else if (resultVal % 1.0 == 0.0) {
            resultVal.toLong().toString()
        } else {
            String.format(Locale.US, "%.10g", resultVal)
        }
    }

    fun evaluateExpression() {
        val expr = _expressionInput.value
        if (expr.isBlank()) return
        try {
            val parser = ExpressionParser(_angleUnit.value, ansVal = _ansValue.value, memVal = _memoryValue.value)
            val resultVal = parser.evaluate(expr)
            val formattedResult = formatVal(resultVal)
            _evaluationResult.value = formattedResult

            if (!resultVal.isNaN() && !resultVal.isInfinite()) {
                _ansValue.value = resultVal
            }

            viewModelScope.launch {
                repository.insert(HistoryItem(expression = expr, result = formattedResult, isFavorite = false))
            }
        } catch (e: Exception) {
            _evaluationResult.value = e.message ?: "Syntax ERROR"
        }
    }

    fun toggleFavorite(item: HistoryItem) {
        viewModelScope.launch {
            repository.update(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun selectHistoryExpression(item: HistoryItem) {
        _expressionInput.value = item.expression
        _evaluationResult.value = item.result
        _currentMode.value = CalculatorMode.COMP
    }

    fun setMatrixDimensionA(rows: Int, cols: Int) {
        _matrixRowsA.value = rows
        _matrixColsA.value = cols
    }

    fun setMatrixDimensionB(rows: Int, cols: Int) {
        _matrixRowsB.value = rows
        _matrixColsB.value = cols
    }

    fun setMatrixDimensionC(rows: Int, cols: Int) {
        _matrixRowsC.value = rows
        _matrixColsC.value = cols
    }

    fun resetMatrixA() {
        for (i in 0..4) {
            for (j in 0..4) {
                matrixValuesA[i][j].value = "0"
            }
        }
    }

    fun resetMatrixB() {
        for (i in 0..4) {
            for (j in 0..4) {
                matrixValuesB[i][j].value = "0"
            }
        }
    }

    fun resetMatrixC() {
        for (i in 0..4) {
            for (j in 0..4) {
                matrixValuesC[i][j].value = "0"
            }
        }
    }

    fun setActiveMatrixSlot(slot: String) {
        _activeMatrixSlot.value = slot
    }

    fun setDisplayMode(mode: String) {
        _displayMode.value = mode
        refreshMatrixResult()
    }

    fun setScalarFactor(value: String) {
        _scalarFactor.value = value
    }

    fun setManualRowI(value: String) {
        _manualRowI.value = value
    }

    fun setManualRowJ(value: String) {
        _manualRowJ.value = value
    }

    fun setManualScalarK(value: String) {
        _manualScalarK.value = value
    }

    fun clearMatrixError() {
        _matrixErrorMsg.value = null
    }

    private fun refreshMatrixResult() {
        val m = lastMatrixComputed
        if (m != null) {
            _matrixResultText.value = lastMatrixOpLabel + "\n" + m.format(_displayMode.value)
        }
    }

    fun getMatrixFromState(slot: String): Matrix {
        val rows = when (slot) {
            "A" -> _matrixRowsA.value
            "B" -> _matrixRowsB.value
            else -> _matrixRowsC.value
        }
        val cols = when (slot) {
            "A" -> _matrixColsA.value
            "B" -> _matrixColsB.value
            else -> _matrixColsC.value
        }
        val stateArray = when (slot) {
            "A" -> matrixValuesA
            "B" -> matrixValuesB
            else -> matrixValuesC
        }

        val doubleData = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val cellStr = stateArray[i][j].value.trim()
                doubleData[i][j] = cellStr.toDoubleOrNull() ?: 0.0
            }
        }
        return Matrix(rows, cols, doubleData)
    }

    fun computeMatrixOperation(op: String) {
        _matrixErrorMsg.value = null
        try {
            val a = getMatrixFromState("A")
            val b = getMatrixFromState("B")
            val c = getMatrixFromState("C")
            
            val active = getMatrixFromState(_activeMatrixSlot.value)

            val scalar = _scalarFactor.value.toDoubleOrNull() ?: 2.0
            val rowI = (_manualRowI.value.toIntOrNull() ?: 1) - 1
            val rowJ = (_manualRowJ.value.toIntOrNull() ?: 2) - 1
            val rScalar = _manualScalarK.value.toDoubleOrNull() ?: 2.0

            when (op) {
                // Basic Arithmetic
                "A+B" -> {
                    val res = a.plus(b)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix A+B:"
                    refreshMatrixResult()
                }
                "A-B" -> {
                    val res = a.minus(b)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix A-B:"
                    refreshMatrixResult()
                }
                "A*B" -> {
                    val res = a.times(b)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix A×B:"
                    refreshMatrixResult()
                }
                "scalar*A" -> {
                    val res = active.times(scalar)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Scalar ($scalar) × Matrix [${_activeMatrixSlot.value}]:"
                    refreshMatrixResult()
                }
                "A/scalar" -> {
                    val res = active.div(scalar)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix [${_activeMatrixSlot.value}] ÷ Scalar ($scalar):"
                    refreshMatrixResult()
                }

                // Matrix-Specific Operations (Double/Scalar results - go to main LCD display)
                "det" -> {
                    val detVal = active.determinant()
                    val formatted = formatVal(detVal)
                    _evaluationResult.value = "det([${_activeMatrixSlot.value}]) = $formatted"
                    _matrixResultText.value = "det([${_activeMatrixSlot.value}]) = $formatted"
                }
                "tr" -> {
                    val trVal = active.trace()
                    val formatted = formatVal(trVal)
                    _evaluationResult.value = "tr([${_activeMatrixSlot.value}]) = $formatted"
                    _matrixResultText.value = "tr([${_activeMatrixSlot.value}]) = $formatted"
                }
                "rank" -> {
                    val rankVal = active.rank()
                    _evaluationResult.value = "rank([${_activeMatrixSlot.value}]) = $rankVal"
                    _matrixResultText.value = "rank([${_activeMatrixSlot.value}]) = $rankVal"
                }

                // Matrix outputs
                "inv" -> {
                    val res = active.inverse()
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix [${_activeMatrixSlot.value}]⁻¹:"
                    refreshMatrixResult()
                }
                "trans" -> {
                    val res = active.transpose()
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Matrix [${_activeMatrixSlot.value}]ᵀ:"
                    refreshMatrixResult()
                }
                "ref" -> {
                    val res = active.ref()
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "REF([${_activeMatrixSlot.value}]):"
                    refreshMatrixResult()
                }
                "rref" -> {
                    val res = active.rref()
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "RREF([${_activeMatrixSlot.value}]):"
                    refreshMatrixResult()
                }

                // Power & Exponents
                "A2" -> {
                    val res = active.power(2)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "[${_activeMatrixSlot.value}]²:"
                    refreshMatrixResult()
                }
                "An" -> {
                    val p = scalar.toInt()
                    val res = active.power(p)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "[${_activeMatrixSlot.value}]^$p:"
                    refreshMatrixResult()
                }
                "A_neg_1" -> {
                    val res = active.inverse()
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "[${_activeMatrixSlot.value}]⁻¹:"
                    refreshMatrixResult()
                }
                "A_neg_n" -> {
                    val p = scalar.toInt()
                    val res = active.power(-p)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "[${_activeMatrixSlot.value}]^-$p:"
                    refreshMatrixResult()
                }

                // Decomposition
                "LU" -> {
                    val (l, u) = active.luDecomposition()
                    _matrixResultText.value = "LU Decomposition:\nMatrix L:\n${l.format(_displayMode.value)}\n\nMatrix U:\n${u.format(_displayMode.value)}"
                }
                "QR" -> {
                    val (q, r) = active.qrDecomposition()
                    _matrixResultText.value = "QR Decomposition:\nMatrix Q:\n${q.format(_displayMode.value)}\n\nMatrix R:\n${r.format(_displayMode.value)}"
                }
                "SVD" -> {
                    val (u, sig, v) = active.svdDecomposition()
                    _matrixResultText.value = "SVD Decomposition:\nMatrix U:\n${u.format(_displayMode.value)}\n\nSigma:\n${sig.format(_displayMode.value)}\n\nMatrix Vᵀ:\n${v.format(_displayMode.value)}"
                }
                "eigen" -> {
                    val (evs, evecs) = active.eigenDecomposition()
                    val sb = java.lang.StringBuilder()
                    sb.append("Eigenvalues & Eigenvectors:\n")
                    for (i in evs.indices) {
                        val evVal = formatVal(evs[i])
                        sb.append("λ_${i+1} = $evVal\n")
                        sb.append("v_${i+1} = [${evecs[i].joinToString(", ") { String.format(Locale.US, "%.4f", it) }}]\n\n")
                    }
                    _matrixResultText.value = sb.toString().trim()
                }

                // Solving Systems
                "solve" -> {
                    val res = a.solve(b)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Solution Vector x (Ax=B):"
                    refreshMatrixResult()
                }
                "cramer" -> {
                    val res = a.cramer(b)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Cramer's Rule Vector x (Ax=B):"
                    refreshMatrixResult()
                }

                // Norms & Properties
                "norm" -> {
                    val nVal = active.normFrobenius()
                    val formatted = formatVal(nVal)
                    _evaluationResult.value = "norm([${_activeMatrixSlot.value}]) = $formatted"
                    _matrixResultText.value = "norm([${_activeMatrixSlot.value}]) = $formatted"
                }
                "cond" -> {
                    val cVal = active.cond()
                    val formatted = formatVal(cVal)
                    _evaluationResult.value = "cond([${_activeMatrixSlot.value}]) = $formatted"
                    _matrixResultText.value = "cond([${_activeMatrixSlot.value}]) = $formatted"
                }
                "span" -> {
                    val r = active.rank()
                    val msg = "Span Dim = $r / ${active.cols}"
                    _evaluationResult.value = msg
                    _matrixResultText.value = "Span Properties:\nRank is $r.\nColumn space spans a subspace of dimension $r."
                }
                "sym" -> {
                    val isSym = active.isSymmetric()
                    val resStr = if (isSym) "Symmetric: Yes" else "Symmetric: No"
                    _evaluationResult.value = resStr
                    _matrixResultText.value = "Symmetry check:\n[${_activeMatrixSlot.value}] is ${if (isSym) "" else "NOT "}symmetric."
                }
                "orth" -> {
                    val isOrth = active.isOrthogonal()
                    val resStr = if (isOrth) "Orthogonal: Yes" else "Orthogonal: No"
                    _evaluationResult.value = resStr
                    _matrixResultText.value = "Orthogonality check:\n[${_activeMatrixSlot.value}] is ${if (isOrth) "" else "NOT "}orthogonal."
                }

                // Row operations
                "row_swap" -> {
                    val res = active.swapRows(rowI, rowJ)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Row Swap R${rowI+1} ↔ R${rowJ+1}:"
                    refreshMatrixResult()
                }
                "row_scale" -> {
                    val res = active.scaleRow(rowI, rScalar)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Row Scale ${rScalar} × R${rowI+1}:"
                    refreshMatrixResult()
                }
                "row_add_scaled" -> {
                    val res = active.addScaledRow(rowI, rowJ, rScalar)
                    lastMatrixComputed = res
                    lastMatrixAns = res
                    lastMatrixOpLabel = "Row Add R${rowJ+1} + (${rScalar}) × R${rowI+1}:"
                    refreshMatrixResult()
                }

                // Utilities
                "clear" -> {
                    when (_activeMatrixSlot.value) {
                        "A" -> resetMatrixA()
                        "B" -> resetMatrixB()
                        "C" -> resetMatrixC()
                    }
                    _matrixResultText.value = "Matrix [${_activeMatrixSlot.value}] Cleared to 0"
                }
                "clear_all" -> {
                    resetMatrixA()
                    resetMatrixB()
                    resetMatrixC()
                    _matrixResultText.value = "All Matrices Cleared to 0"
                }
                "ans" -> {
                    val ansM = lastMatrixAns
                    if (ansM != null) {
                        val rows = when (_activeMatrixSlot.value) {
                            "A" -> _matrixRowsA.value
                            "B" -> _matrixRowsB.value
                            else -> _matrixRowsC.value
                        }
                        val cols = when (_activeMatrixSlot.value) {
                            "A" -> _matrixColsA.value
                            "B" -> _matrixColsB.value
                            else -> _matrixColsC.value
                        }
                        val destVals = when (_activeMatrixSlot.value) {
                            "A" -> matrixValuesA
                            "B" -> matrixValuesB
                            else -> matrixValuesC
                        }

                        for (i in 0 until rows) {
                            for (j in 0 until cols) {
                                if (i < ansM.rows && j < ansM.cols) {
                                    destVals[i][j].value = String.format(Locale.US, "%.5f", ansM.data[i][j]).trimEnd('0').trimEnd('.')
                                }
                            }
                        }
                        _matrixResultText.value = "Loaded Last Matrix Result into Slot [${_activeMatrixSlot.value}]"
                    } else {
                        _matrixResultText.value = "No Last Matrix Result (ANS) available"
                    }
                }
            }
        } catch (e: ArithmeticException) {
            _matrixErrorMsg.value = e.message ?: "Matrix is singular — operation undefined."
            _matrixResultText.value = "Arithmetic ERROR:\n${e.message}"
        } catch (e: IllegalArgumentException) {
            _matrixErrorMsg.value = e.message ?: "Incompatible dimensions."
            _matrixResultText.value = "Dimension ERROR:\n${e.message}"
        } catch (e: Exception) {
            _matrixResultText.value = "Execution ERROR:\n${e.message}"
        }
    }

    fun setExpression2D(expr: String) {
        _expression2D.value = expr
    }

    fun setExpression3D(expr: String) {
        _expression3D.value = expr
    }

    fun setCursor2D(pt: Pair<Double, Double>?) {
        _cursorValue2D.value = pt
    }

    fun exportHistoryToText(context: Context) {
        val historyList = historyState.value
        if (historyList.isEmpty()) return
        val sb = StringBuilder()
        sb.append("--- CASIO SCI-CALC HISTORY BACKUP ---\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for (item in historyList) {
            val dateStr = sdf.format(Date(item.timestamp))
            val favSymbol = if (item.isFavorite) "★ " else "  "
            sb.append("$favSymbol[$dateStr] ${item.expression} = ${item.result}\n")
        }

        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Calculator History")
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }
}
