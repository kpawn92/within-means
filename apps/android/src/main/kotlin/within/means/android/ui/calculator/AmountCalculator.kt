package within.means.android.ui.calculator

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A minimalist money calculator: builds an arithmetic expression from keypad
 * input (`+ − × ÷`, digits, decimal point) and evaluates it to cents.
 *
 * Pure logic — no Android/Compose dependencies — so the input rules and the
 * evaluator are unit-tested directly. Each operand is constrained the same way
 * as the QuickAdd keypad: ≤9 integer digits and ≤2 decimals.
 *
 * Operators are stored canonically as `+ - * /`; [displayExpression] maps them
 * to the glyphs the user sees (`+ − × ÷`).
 */
class AmountCalculator(initial: String = "") {

    /** Canonical expression, e.g. "12.50+3*2". Empty means "0". */
    var expression: String = initial
        private set

    private companion object {
        const val OPERATORS = "+-*/"
        const val MAX_INTEGER_DIGITS = 9
        const val MAX_DECIMALS = 2
        const val DIVISION_SCALE = 12
    }

    private val endsWithOperator: Boolean
        get() = expression.isNotEmpty() && expression.last() in OPERATORS

    /** The operand currently being typed (everything after the last operator). */
    private val currentOperand: String
        get() {
            val lastOp = expression.indexOfLast { it in OPERATORS }
            return if (lastOp == -1) expression else expression.substring(lastOp + 1)
        }

    fun onDigit(d: Char) {
        if (d !in '0'..'9') return
        val operand = currentOperand
        if (operand.contains('.')) {
            val decimals = operand.substringAfter('.')
            if (decimals.length >= MAX_DECIMALS) return
        } else {
            if (operand.length >= MAX_INTEGER_DIGITS) return
            // A lone leading zero is replaced rather than grown ("0" + "5" → "5").
            if (operand == "0") {
                expression = expression.dropLast(1) + d
                return
            }
        }
        expression += d
    }

    fun onDot() {
        val operand = currentOperand
        if (operand.contains('.')) return
        // Starting a decimal with no integer part yields "0.".
        expression += if (operand.isEmpty()) "0." else "."
    }

    /** Appends an operator, normalising trailing operators/dots away first. */
    fun onOperator(op: Char) {
        if (op !in OPERATORS) return
        if (expression.isEmpty()) return // no leading operator
        if (expression.last() == '.') expression = expression.dropLast(1)
        expression = if (endsWithOperator) expression.dropLast(1) + op else expression + op
    }

    fun onBackspace() {
        if (expression.isNotEmpty()) expression = expression.dropLast(1)
    }

    fun clear() {
        expression = ""
    }

    /** Replaces the expression with its evaluated result, so the user can keep operating. */
    fun evaluateInPlace() {
        val text = resultText() ?: return
        expression = text
    }

    /** Evaluated amount in cents, or null if empty/invalid. May be negative. */
    fun resultCents(): Long? {
        val value = evaluate() ?: return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Evaluated amount as a 2-decimal string for the amount field, or null. */
    fun resultText(): String? {
        val value = evaluate() ?: return null
        return value.setScale(MAX_DECIMALS, RoundingMode.HALF_UP).toPlainString()
    }

    /** User-facing expression with proper math glyphs. */
    fun displayExpression(): String =
        expression
            .replace('*', '×')
            .replace('/', '÷')
            .replace('-', '−')

    private fun evaluate(): BigDecimal? {
        // Tolerate a trailing operator/dot while the user is mid-typing.
        var expr = expression
        while (expr.isNotEmpty() && (expr.last() in OPERATORS || expr.last() == '.')) {
            expr = expr.dropLast(1)
        }
        if (expr.isEmpty()) return null
        return runCatching { evalExpression(tokenize(expr)) }.getOrNull()
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val number = StringBuilder()
        for (ch in expr) {
            if (ch in OPERATORS) {
                if (number.isEmpty()) return emptyList() // operator with no preceding operand
                tokens.add(number.toString()); number.clear()
                tokens.add(ch.toString())
            } else {
                number.append(ch)
            }
        }
        if (number.isEmpty()) return emptyList()
        tokens.add(number.toString())
        return tokens
    }

    /** Shunting-yard to RPN, then fold — left-to-right with `* /` over `+ -`. */
    private fun evalExpression(tokens: List<String>): BigDecimal? {
        if (tokens.isEmpty()) return null
        val output = ArrayDeque<BigDecimal>()
        val ops = ArrayDeque<Char>()

        fun precedence(op: Char) = if (op == '*' || op == '/') 2 else 1
        fun apply(op: Char) {
            val b = output.removeLastOrNull() ?: error("malformed")
            val a = output.removeLastOrNull() ?: error("malformed")
            output.addLast(
                when (op) {
                    '+' -> a + b
                    '-' -> a - b
                    '*' -> a * b
                    '/' -> if (b.signum() == 0) error("div by zero")
                    else a.divide(b, DIVISION_SCALE, RoundingMode.HALF_UP)
                    else -> error("unknown op")
                },
            )
        }

        for (token in tokens) {
            val op = token.singleOrNull()
            if (op != null && op in OPERATORS) {
                while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(op)) apply(ops.removeLast())
                ops.addLast(op)
            } else {
                output.addLast(BigDecimal(token))
            }
        }
        while (ops.isNotEmpty()) apply(ops.removeLast())
        return output.singleOrNull()
    }
}
