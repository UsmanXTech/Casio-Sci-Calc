package com.example.calculator

import kotlin.math.abs
import kotlin.math.roundToLong

class Matrix(val rows: Int, val cols: Int, val data: Array<DoubleArray>) {
    constructor(rows: Int, cols: Int) : this(rows, cols, Array(rows) { DoubleArray(cols) })

    fun plus(other: Matrix): Matrix {
        if (rows != other.rows || cols != other.cols) {
            throw IllegalArgumentException("Incompatible dimensions for addition")
        }
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] + other.data[i][j]
            }
        }
        return result
    }

    fun minus(other: Matrix): Matrix {
        if (rows != other.rows || cols != other.cols) {
            throw IllegalArgumentException("Incompatible dimensions for subtraction")
        }
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] - other.data[i][j]
            }
        }
        return result
    }

    fun times(other: Matrix): Matrix {
        if (cols != other.rows) {
            throw IllegalArgumentException("Incompatible dimensions for multiplication ($cols cols vs ${other.rows} rows)")
        }
        val result = Matrix(rows, other.cols)
        for (i in 0 until rows) {
            for (j in 0 until other.cols) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += data[i][k] * other.data[k][j]
                }
                result.data[i][j] = sum
            }
        }
        return result
    }

    fun times(scalar: Double): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] * scalar
            }
        }
        return result
    }

    fun div(scalar: Double): Matrix {
        if (abs(scalar) < 1e-15) {
            throw ArithmeticException("Division by zero scalar")
        }
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] / scalar
            }
        }
        return result
    }

    fun transpose(): Matrix {
        val result = Matrix(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[j][i] = data[i][j]
            }
        }
        return result
    }

    fun trace(): Double {
        if (rows != cols) {
            throw IllegalArgumentException("Trace is only defined for square matrices")
        }
        var sum = 0.0
        for (i in 0 until rows) {
            sum += data[i][i]
        }
        return sum
    }

    fun determinant(): Double {
        if (rows != cols) {
            throw IllegalArgumentException("Determinant is only defined for square matrices")
        }
        return getDeterminantRec(data)
    }

    private fun getDeterminantRec(matrix: Array<DoubleArray>): Double {
        val n = matrix.size
        if (n == 1) return matrix[0][0]
        if (n == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
        }
        var det = 0.0
        for (j in 0 until n) {
            val subMatrix = Array(n - 1) { DoubleArray(n - 1) }
            for (i in 1 until n) {
                var colCount = 0
                for (k in 0 until n) {
                    if (k == j) continue
                    subMatrix[i - 1][colCount++] = matrix[i][k]
                }
            }
            val sign = if (j % 2 == 0) 1.0 else -1.0
            det += sign * matrix[0][j] * getDeterminantRec(subMatrix)
        }
        return det
    }

    fun inverse(): Matrix {
        if (rows != cols) {
            throw IllegalArgumentException("Inverse is only defined for square matrices")
        }
        val det = determinant()
        if (abs(det) < 1e-12) {
            throw ArithmeticException("Matrix is singular (det ≈ 0) — inverse undefined")
        }
        val n = rows
        val result = Matrix(n, n)
        if (n == 1) {
            result.data[0][0] = 1.0 / data[0][0]
            return result
        }

        val adj = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                val subMatrix = Array(n - 1) { DoubleArray(n - 1) }
                var rowCount = 0
                for (r in 0 until n) {
                    if (r == i) continue
                    var colCount = 0
                    for (c in 0 until n) {
                        if (c == j) continue
                        subMatrix[rowCount][colCount++] = data[r][c]
                    }
                    rowCount++
                }
                val sign = if ((i + j) % 2 == 0) 1.0 else -1.0
                adj[j][i] = sign * getDeterminantRec(subMatrix)
            }
        }

        for (i in 0 until n) {
            for (j in 0 until n) {
                result.data[i][j] = adj[i][j] / det
            }
        }
        return result
    }

    fun ref(): Matrix {
        val result = Matrix(rows, cols, Array(rows) { i -> data[i].copyOf() })
        var lead = 0
        for (r in 0 until rows) {
            if (cols <= lead) break
            var i = r
            while (abs(result.data[i][lead]) < 1e-9) {
                i++
                if (rows == i) {
                    i = r
                    lead++
                    if (cols == lead) return result
                }
            }
            val temp = result.data[i]
            result.data[i] = result.data[r]
            result.data[r] = temp

            val lv = result.data[r][lead]
            if (abs(lv) > 1e-9) {
                for (c in 0 until cols) {
                    result.data[r][c] /= lv
                }
            }
            for (i2 in r + 1 until rows) {
                val lv2 = result.data[i2][lead]
                for (c in 0 until cols) {
                    result.data[i2][c] -= lv2 * result.data[r][c]
                }
            }
            lead++
        }
        return result
    }

    fun rref(): Matrix {
        val result = Matrix(rows, cols, Array(rows) { i -> data[i].copyOf() })
        var lead = 0
        for (r in 0 until rows) {
            if (cols <= lead) break
            var i = r
            while (abs(result.data[i][lead]) < 1e-9) {
                i++
                if (rows == i) {
                    i = r
                    lead++
                    if (cols == lead) return result
                }
            }
            val temp = result.data[i]
            result.data[i] = result.data[r]
            result.data[r] = temp

            val lv = result.data[r][lead]
            if (abs(lv) > 1e-9) {
                for (c in 0 until cols) {
                    result.data[r][c] /= lv
                }
            }
            for (i2 in 0 until rows) {
                if (i2 != r) {
                    val lv2 = result.data[i2][lead]
                    for (c in 0 until cols) {
                        result.data[i2][c] -= lv2 * result.data[r][c]
                    }
                }
            }
            lead++
        }
        return result
    }

    fun rank(): Int {
        val rrefM = rref()
        var nonZeroRows = 0
        for (i in 0 until rows) {
            var allZeros = true
            for (j in 0 until cols) {
                if (abs(rrefM.data[i][j]) > 1e-9) {
                    allZeros = false
                    break
                }
            }
            if (!allZeros) nonZeroRows++
        }
        return nonZeroRows
    }

    fun power(n: Int): Matrix {
        if (rows != cols) {
            throw IllegalArgumentException("Powers are only defined for square matrices")
        }
        if (n == 0) {
            val identity = Matrix(rows, cols)
            for (i in 0 until rows) identity.data[i][i] = 1.0
            return identity
        }
        if (n < 0) {
            return inverse().power(-n)
        }
        var prod = Matrix(rows, cols, Array(rows) { i -> data[i].copyOf() })
        for (iter in 1 until n) {
            prod = prod.times(this)
        }
        return prod
    }

    fun normFrobenius(): Double {
        var sum = 0.0
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                sum += data[i][j] * data[i][j]
            }
        }
        return kotlin.math.sqrt(sum)
    }

    fun cond(): Double {
        val normA = normFrobenius()
        return try {
            val invA = inverse()
            val normInvA = invA.normFrobenius()
            normA * normInvA
        } catch (e: Exception) {
            Double.POSITIVE_INFINITY
        }
    }

    fun isSymmetric(): Boolean {
        if (rows != cols) return false
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                if (abs(data[i][j] - data[j][i]) > 1e-9) return false
            }
        }
        return true
    }

    fun isOrthogonal(): Boolean {
        if (rows != cols) return false
        val identity = Matrix(rows, cols)
        for (i in 0 until rows) identity.data[i][i] = 1.0
        return try {
            val prod = times(transpose())
            var diff = 0.0
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    diff += abs(prod.data[i][j] - identity.data[i][j])
                }
            }
            diff < 1e-9
        } catch (e: Exception) {
            false
        }
    }

    fun luDecomposition(): Pair<Matrix, Matrix> {
        if (rows != cols) {
            throw IllegalArgumentException("LU decomposition is only defined for square matrices")
        }
        val n = rows
        val l = Array(n) { DoubleArray(n) }
        val u = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (k in i until n) {
                var sum = 0.0
                for (j in 0 until i) {
                    sum += l[i][j] * u[j][k]
                }
                u[i][k] = data[i][k] - sum
            }

            for (k in i until n) {
                if (i == k) {
                    l[i][i] = 1.0
                } else {
                    var sum = 0.0
                    for (j in 0 until i) {
                        sum += l[k][j] * u[j][i]
                    }
                    if (abs(u[i][i]) < 1e-12) {
                        l[k][i] = (data[k][i] - sum) / 1e-12
                    } else {
                        l[k][i] = (data[k][i] - sum) / u[i][i]
                    }
                }
            }
        }
        return Pair(Matrix(n, n, l), Matrix(n, n, u))
    }

    fun qrDecomposition(): Pair<Matrix, Matrix> {
        val qData = Array(rows) { DoubleArray(cols) }
        val rData = Array(cols) { DoubleArray(cols) }

        val aVectors = Array(cols) { DoubleArray(rows) }
        for (j in 0 until cols) {
            for (i in 0 until rows) {
                aVectors[j][i] = data[i][j]
            }
        }

        val qVectors = Array(cols) { DoubleArray(rows) }

        for (j in 0 until cols) {
            val v = aVectors[j].copyOf()
            for (i in 0 until j) {
                val dot = dotProduct(aVectors[j], qVectors[i])
                rData[i][j] = dot
                for (k in 0 until rows) {
                    v[k] -= dot * qVectors[i][k]
                }
            }
            val norm = norm(v)
            rData[j][j] = norm
            if (norm > 1e-12) {
                for (k in 0 until rows) {
                    qVectors[j][k] = v[k] / norm
                }
            } else {
                for (k in 0 until rows) {
                    qVectors[j][k] = 0.0
                }
            }
        }

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                if (j < qVectors.size) {
                    qData[i][j] = qVectors[j][i]
                }
            }
        }

        return Pair(Matrix(rows, cols, qData), Matrix(cols, cols, rData))
    }

    private fun dotProduct(u: DoubleArray, v: DoubleArray): Double {
        var sum = 0.0
        for (i in u.indices) {
            sum += u[i] * v[i]
        }
        return sum
    }

    private fun norm(u: DoubleArray): Double {
        var sum = 0.0
        for (x in u) {
            sum += x * x
        }
        return kotlin.math.sqrt(sum)
    }

    fun svdDecomposition(): Triple<Matrix, Matrix, Matrix> {
        val m = rows
        val n = cols
        val vData = Array(n) { DoubleArray(n) }
        for (i in 0 until n) vData[i][i] = 1.0

        val uData = Array(m) { DoubleArray(n) }
        for (i in 0 until m) {
            for (j in 0 until n) {
                uData[i][j] = data[i][j]
            }
        }

        val maxSweeps = 30
        val tol = 1e-9
        for (sweep in 0 until maxSweeps) {
            var converged = true
            for (i in 0 until n - 1) {
                for (j in i + 1 until n) {
                    var a = 0.0
                    var b = 0.0
                    var c = 0.0
                    for (k in 0 until m) {
                        a += uData[k][i] * uData[k][i]
                        b += uData[k][j] * uData[k][j]
                        c += uData[k][i] * uData[k][j]
                    }
                    if (abs(c) > tol * kotlin.math.sqrt(a * b)) {
                        converged = false
                        val zeta = (b - a) / (2.0 * c)
                        val t = if (zeta >= 0) 1.0 / (zeta + kotlin.math.sqrt(1.0 + zeta * zeta))
                                else -1.0 / (-zeta + kotlin.math.sqrt(1.0 + zeta * zeta))
                        val cosTheta = 1.0 / kotlin.math.sqrt(1.0 + t * t)
                        val sinTheta = t * cosTheta

                        for (k in 0 until m) {
                            val tempUi = uData[k][i]
                            uData[k][i] = cosTheta * tempUi - sinTheta * uData[k][j]
                            uData[k][j] = sinTheta * tempUi + cosTheta * uData[k][j]
                        }
                        for (k in 0 until n) {
                            val tempVi = vData[k][i]
                            vData[k][i] = cosTheta * tempVi - sinTheta * vData[k][j]
                            vData[k][j] = sinTheta * tempVi + cosTheta * vData[k][j]
                        }
                    }
                }
            }
            if (converged) break
        }

        val s = DoubleArray(n)
        for (j in 0 until n) {
            var sum = 0.0
            for (i in 0 until m) {
                sum += uData[i][j] * uData[i][j]
            }
            s[j] = kotlin.math.sqrt(sum)
            if (s[j] > 1e-12) {
                for (i in 0 until m) {
                    uData[i][j] /= s[j]
                }
            }
        }

        val indices = s.indices.sortedWith { i1, i2 -> s[i2].compareTo(s[i1]) }
        val sortedS = DoubleArray(n)
        val sortedU = Array(m) { DoubleArray(n) }
        val sortedV = Array(n) { DoubleArray(n) }
        for (j in 0 until n) {
            val origIdx = indices[j]
            sortedS[j] = s[origIdx]
            for (i in 0 until m) {
                sortedU[i][j] = uData[i][origIdx]
            }
            for (i in 0 until n) {
                sortedV[i][j] = vData[i][origIdx]
            }
        }

        val sigma = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            sigma[i][i] = sortedS[i]
        }

        return Triple(Matrix(m, n, sortedU), Matrix(n, n, sigma), Matrix(n, n, sortedV))
    }

    fun eigenvalues(): DoubleArray {
        if (rows != cols) {
            throw IllegalArgumentException("Eigenvalues are only defined for square matrices")
        }
        val n = rows
        val temp = Matrix(n, n, Array(n) { i -> data[i].copyOf() })
        val maxIterations = 100
        for (iter in 0 until maxIterations) {
            val (q, r) = temp.qrDecomposition()
            val next = r.times(q)
            var offDiagSum = 0.0
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (i != j) {
                        offDiagSum += abs(next.data[i][j])
                    }
                }
            }
            for (i in 0 until n) {
                for (j in 0 until n) {
                    temp.data[i][j] = next.data[i][j]
                }
            }
            if (offDiagSum < 1e-9) break
        }
        val ev = DoubleArray(n)
        for (i in 0 until n) {
            ev[i] = temp.data[i][i]
        }
        return ev
    }

    fun eigenDecomposition(): Pair<DoubleArray, Array<DoubleArray>> {
        val ev = eigenvalues()
        val n = rows
        val eigenvectors = Array(n) { DoubleArray(n) }

        for (idx in 0 until n) {
            val lam = ev[idx]
            val y = DoubleArray(n) { 1.0 }
            val mShift = Matrix(n, n)
            for (i in 0 until n) {
                for (j in 0 until n) {
                    mShift.data[i][j] = data[i][j] - (if (i == j) lam else 0.0)
                }
            }

            for (iter in 0 until 5) {
                val regMatrix = Matrix(n, n)
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        regMatrix.data[i][j] = mShift.data[i][j] + (if (i == j) 1e-9 else 0.0)
                    }
                }

                try {
                    val invReg = regMatrix.inverse()
                    val rhs = Matrix(n, 1, Array(n) { i -> doubleArrayOf(y[i]) })
                    val nextY = invReg.times(rhs)
                    var norm = 0.0
                    for (i in 0 until n) {
                        y[i] = nextY.data[i][0]
                        norm += y[i] * y[i]
                    }
                    norm = kotlin.math.sqrt(norm)
                    if (norm > 1e-12) {
                        for (i in 0 until n) {
                            y[i] /= norm
                        }
                    }
                } catch (e: Exception) {
                    val rrefM = regMatrix.rref()
                    y[0] = 1.0
                    for (i in 1 until n) y[i] = 0.0
                    break
                }
            }
            for (i in 0 until n) {
                eigenvectors[idx][i] = y[i]
            }
        }
        return Pair(ev, eigenvectors)
    }

    fun solve(b: Matrix): Matrix {
        if (rows != cols) {
            throw IllegalArgumentException("Coefficient matrix must be square (Ax=B)")
        }
        if (rows != b.rows) {
            throw IllegalArgumentException("Coefficient rows ($rows) must match vector rows (${b.rows})")
        }
        val invA = this.inverse()
        return invA.times(b)
    }

    fun cramer(b: Matrix): Matrix {
        if (rows != cols) {
            throw IllegalArgumentException("Coefficient matrix must be square for Cramer's rule")
        }
        if (rows != b.rows) {
            throw IllegalArgumentException("Coefficient rows ($rows) must match vector rows (${b.rows})")
        }
        if (b.cols != 1) {
            throw IllegalArgumentException("Cramer's rule is only formulated for single column vector")
        }
        val mainDet = determinant()
        if (abs(mainDet) < 1e-12) {
            throw ArithmeticException("Determinant ≈ 0; Cramer's rule undefined")
        }
        val n = rows
        val solution = Matrix(n, 1)
        for (j in 0 until n) {
            val replaced = Matrix(n, n)
            for (r in 0 until n) {
                for (c in 0 until n) {
                    replaced.data[r][c] = if (c == j) b.data[r][0] else data[r][c]
                }
            }
            solution.data[j][0] = replaced.determinant() / mainDet
        }
        return solution
    }

    fun swapRows(i: Int, j: Int): Matrix {
        val result = Matrix(rows, cols, Array(rows) { r -> data[r].copyOf() })
        if (i < 0 || i >= rows || j < 0 || j >= rows) throw IllegalArgumentException("Row index out of bounds")
        val temp = result.data[i]
        result.data[i] = result.data[j]
        result.data[j] = temp
        return result
    }

    fun scaleRow(i: Int, k: Double): Matrix {
        val result = Matrix(rows, cols, Array(rows) { r -> data[r].copyOf() })
        if (i < 0 || i >= rows) throw IllegalArgumentException("Row index out of bounds")
        for (c in 0 until cols) {
            result.data[i][c] *= k
        }
        return result
    }

    fun addScaledRow(srcRow: Int, destRow: Int, k: Double): Matrix {
        val result = Matrix(rows, cols, Array(rows) { r -> data[r].copyOf() })
        if (srcRow < 0 || srcRow >= rows || destRow < 0 || destRow >= rows) throw IllegalArgumentException("Row index out of bounds")
        for (c in 0 until cols) {
            result.data[destRow][c] += k * result.data[srcRow][c]
        }
        return result
    }

    fun format(displayMode: String): String {
        return data.joinToString(separator = "\n") { row ->
            row.joinToString(separator = "  ") { formatNum(it, displayMode) }
        }
    }

    private fun formatNum(value: Double, displayMode: String): String {
        if (value.isInfinite() || value.isNaN()) return "NaN"
        return when (displayMode) {
            "FRAC" -> doubleToFraction(value)
            "SCI" -> String.format(java.util.Locale.US, "%.4e", value)
            else -> {
                if (abs(value - value.roundToLong()) < 1e-9) {
                    value.roundToLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.5f", value).trimEnd('0').trimEnd('.')
                }
            }
        }
    }

    private fun doubleToFraction(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "NaN"
        if (abs(value) < 1e-9) return "0"
        if (abs(value - value.roundToLong()) < 1e-9) return value.roundToLong().toString()

        val tolerance = 1.0e-6
        var h1 = 1L
        var h2 = 0L
        var k1 = 0L
        var k2 = 1L
        var b = value
        val isNegative = value < 0
        if (isNegative) b = -b

        do {
            val a = b.toLong()
            val aux = h1
            h1 = a * h1 + h2
            h2 = aux
            val aux2 = k1
            k1 = a * k1 + k2
            k2 = aux2
            b = 1.0 / (b - a)
        } while (abs(value - h1.toDouble() / k1.toDouble()) > value * tolerance && k1 < 1000000000L)

        val num = if (isNegative) -h1 else h1
        val den = k1
        return if (den == 1L) "$num" else "$num/$den"
    }

    override fun toString(): String {
        return format("DEC")
    }
}
