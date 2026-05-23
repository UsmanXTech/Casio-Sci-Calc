package com.example.calculator

import kotlin.math.*

enum class AngleUnit {
    DEG, RAD, GRAD
}

class ExpressionParser(
    private val angleUnit: AngleUnit = AngleUnit.RAD,
    private val ansVal: Double = 0.0,
    private val memVal: Double = 0.0
) {

    fun evaluate(expression: String, xVal: Double = 0.0, yVal: Double = 0.0): Double {
        val tokens = tokenize(expression)
        if (tokens.isEmpty()) return 0.0
        val context = EvaluationContext(tokens, xVal, yVal, angleUnit, ansVal, memVal)
        return context.parse()
    }

    private class EvaluationContext(
        val tokens: List<String>,
        val xVal: Double,
        val yVal: Double,
        val angleUnit: AngleUnit,
        val ansVal: Double,
        val memVal: Double
    ) {
        var pos = 0

        fun parse(): Double {
            val result = parseExpression()
            if (pos < tokens.size) {
                throw IllegalArgumentException("Unexpected token index.")
            }
            return result
        }

        private fun peek(): String? = if (pos < tokens.size) tokens[pos] else null
        
        private fun consume(expected: String): Boolean {
            if (peek() == expected) {
                pos++
                return true
            }
            return false
        }

        private fun parseFactor(): Double {
            val token = peek() ?: throw IllegalArgumentException("Unexpected end of expression")
            pos++
            return when {
                token == "-" -> -parseFactor()
                token == "+" -> parseFactor()
                token == "(" -> {
                    val result = parseExpression()
                    if (!consume(")")) throw IllegalArgumentException("Missing closed parenthesis")
                    result
                }
                token.toDoubleOrNull() != null -> token.toDouble()
                token.lowercase() == "pi" -> Math.PI
                token.lowercase() == "e" -> Math.E
                token.lowercase() == "phi" -> 1.618033988749895
                token.lowercase() == "c" -> 299792458.0
                token.lowercase() == "ans" -> ansVal
                token.lowercase() == "m" -> memVal
                token.lowercase() == "x" -> xVal
                token.lowercase() == "y" -> yVal
                isFunction(token) -> {
                    val hasParen = peek() == "("
                    val args = if (hasParen) parseArguments() else listOf(parseFactor())
                    evaluateFunction(token.lowercase(), args)
                }
                else -> throw IllegalArgumentException("Unknown symbol: $token")
            }
        }

        private fun parseArguments(): List<Double> {
            consume("(")
            val args = mutableListOf<Double>()
            if (peek() != ")") {
                args.add(parseExpression())
                while (consume(",")) {
                    args.add(parseExpression())
                }
            }
            if (!consume(")")) {
                throw IllegalArgumentException("Missing closed parenthesis")
            }
            return args
        }

        private fun parsePower(): Double {
            var result = parseFactor()
            while (consume("^")) {
                val exponent = parseFactor()
                result = result.pow(exponent)
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parsePower()
            while (true) {
                when {
                    consume("*") -> result *= parsePower()
                    consume("/") -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }
                    else -> break
                }
            }
            return result
        }

        private fun parseExpression(): Double {
            var result = parseTerm()
            while (true) {
                when {
                    consume("+") -> result += parseTerm()
                    consume("-") -> result -= parseTerm()
                    else -> break
                }
            }
            return result
        }

        private fun toRadians(arg: Double): Double {
            return when (angleUnit) {
                AngleUnit.RAD -> arg
                AngleUnit.DEG -> Math.toRadians(arg)
                AngleUnit.GRAD -> arg * (Math.PI / 200.0)
            }
        }

        private fun fromRadians(rad: Double): Double {
            return when (angleUnit) {
                AngleUnit.RAD -> rad
                AngleUnit.DEG -> Math.toDegrees(rad)
                AngleUnit.GRAD -> rad * (200.0 / Math.PI)
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0) throw IllegalArgumentException("Factorial of negative")
            val intN = n.toInt()
            if (intN.toDouble() != n) throw IllegalArgumentException("Factorial of float non-integer")
            var res = 1.0
            for (i in 2..intN) {
                res *= i
            }
            return res
        }

        private fun evaluateFunction(func: String, args: List<Double>): Double {
            if (args.isEmpty()) throw IllegalArgumentException("Function $func expects at least 1 parameter.")
            val arg1 = args[0]
            return when (func) {
                "sin" -> sin(toRadians(arg1))
                "cos" -> cos(toRadians(arg1))
                "tan" -> tan(toRadians(arg1))
                "asin" -> fromRadians(asin(arg1))
                "acos" -> fromRadians(acos(arg1))
                "atan" -> fromRadians(atan(arg1))
                
                "sinh" -> sinh(arg1)
                "cosh" -> cosh(arg1)
                "tanh" -> tanh(arg1)
                "asinh" -> ln(arg1 + sqrt(arg1 * arg1 + 1.0))
                "acosh" -> {
                    if (arg1 < 1.0) throw IllegalArgumentException("acosh domain error: < 1")
                    ln(arg1 + sqrt(arg1 * arg1 - 1.0))
                }
                "atanh" -> {
                    if (abs(arg1) >= 1.0) throw IllegalArgumentException("atanh domain error: >= 1")
                    0.5 * ln((1.0 + arg1) / (1.0 - arg1))
                }

                "ln" -> {
                    if (arg1 <= 0.0) throw IllegalArgumentException("Domain error: ln(x) for x <= 0")
                    ln(arg1)
                }
                "log" -> {
                    if (arg1 <= 0.0) throw IllegalArgumentException("Domain error: log(x) for x <= 0")
                    log10(arg1)
                }
                "log2" -> {
                    if (arg1 <= 0.0) throw IllegalArgumentException("Domain error: log2(x) for x <= 0")
                    log(arg1, 2.0)
                }
                "logn" -> {
                    if (args.size < 2) throw IllegalArgumentException("logn requires 2 parameters: logn(base, value)")
                    val base = args[0]
                    val x = args[1]
                    if (base <= 0.0 || base == 1.0 || x <= 0.0) throw IllegalArgumentException("logn base/value error")
                    log(x, base)
                }
                "sqrt" -> {
                    if (arg1 < 0.0) throw IllegalArgumentException("Domain error: sqrt(x) for x < 0")
                    sqrt(arg1)
                }
                "cbrt" -> Math.cbrt(arg1)
                "nrt" -> {
                    if (args.size < 2) throw IllegalArgumentException("nrt requires 2 parameters: nrt(n, value)")
                    val n = args[0]
                    val x = args[1]
                    if (x < 0.0 && n.toInt() % 2 == 0) throw IllegalArgumentException("nrt even index negative x")
                    x.pow(1.0 / n)
                }
                
                "abs" -> abs(arg1)
                "floor" -> floor(arg1)
                "ceil" -> ceil(arg1)
                "round" -> round(arg1).toDouble()
                "fact" -> factorial(arg1)
                
                "npr" -> {
                    if (args.size < 2) throw IllegalArgumentException("nPr requires 2 parameters : nPr(n, r)")
                    val n = args[0]
                    val r = args[1]
                    if (n < 0 || r < 0 || r > n) return 0.0
                    factorial(n) / factorial(n - r)
                }
                "ncr" -> {
                    if (args.size < 2) throw IllegalArgumentException("nCr requires 2 parameters: nCr(n, r)")
                    val n = args[0]
                    val r = args[1]
                    if (n < 0 || r < 0 || r > n) return 0.0
                    factorial(n) / (factorial(r) * factorial(n - r))
                }
                "mod" -> {
                    if (args.size < 2) throw IllegalArgumentException("mod requires 2 parameters")
                    arg1 % args[1]
                }
                else -> throw IllegalArgumentException("Unknown function: $func")
            }
        }
    }

    private fun tokenize(expr: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        val n = expr.length

        while (i < n) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/^()," -> {
                    list.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    list.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < n && (expr[i].isLetter() || expr[i].isDigit())) {
                        sb.append(expr[i])
                        i++
                    }
                    list.add(sb.toString())
                }
                else -> {
                    throw IllegalArgumentException("Invalid character: $c")
                }
            }
        }
        
        val polishedTokens = mutableListOf<String>()
        for (idx in 0 until list.size) {
            val current = list[idx]
            if (idx > 0) {
                val prev = polishedTokens.last()
                val isPrevOperand = prev == ")" || prev.toDoubleOrNull() != null || prev.lowercase() in setOf("pi", "e", "phi", "c", "ans", "x", "y", "m")
                val isCurrStartOperand = current == "(" || current.toDoubleOrNull() != null || isFunction(current) || current.lowercase() in setOf("pi", "e", "phi", "c", "ans", "x", "y", "m")
                
                if (isPrevOperand && isCurrStartOperand) {
                    polishedTokens.add("*")
                }
            }
            polishedTokens.add(current)
        }

        return polishedTokens
    }

    private companion object {
        fun isFunction(token: String): Boolean {
            return token.lowercase() in setOf(
                "sin", "cos", "tan", "asin", "acos", "atan",
                "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
                "ln", "log", "log2", "logn", "sqrt", "cbrt", "nrt",
                "abs", "floor", "ceil", "round", "fact", "npr", "ncr", "mod"
            )
        }
    }
}
