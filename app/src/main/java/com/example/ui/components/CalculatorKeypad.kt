package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.ProfitGreen
import java.util.Locale
import kotlin.math.abs

/**
 * Evaluates live arithmetic expressions such as "350+8600+300+700" or "1200-150*2"
 * with operator precedence (*, /, % before +, -) and ignores trailing operators safely.
 */
object LiveCalculatorEngine {

    private val operators = setOf('+', '-', '×', '÷', '*', '/', '%')

    fun appendToken(currentExpr: String, token: String): String {
        if (token == "C") return ""
        if (token == "BACK") {
            return if (currentExpr.isNotEmpty()) currentExpr.dropLast(1) else ""
        }

        val lastChar = currentExpr.lastOrNull()

        // Handle operator input
        if (token.length == 1 && token[0] in operators) {
            if (currentExpr.isEmpty()) {
                return if (token == "-") "-" else ""
            }
            if (lastChar != null && lastChar in operators) {
                return currentExpr.dropLast(1) + token
            }
            if (lastChar == '.') {
                return currentExpr.dropLast(1) + token
            }
            return currentExpr + token
        }

        // Handle decimal point: prevent two decimals in the same number segment
        if (token == ".") {
            val currentSegment = currentExpr.takeLastWhile { it !in operators }
            if (currentSegment.contains('.')) return currentExpr
            return if (currentSegment.isEmpty()) "${currentExpr}0." else "$currentExpr."
        }

        // Handle "00" or "0"
        if (token == "00" || token == "0") {
            val currentSegment = currentExpr.takeLastWhile { it !in operators }
            if (currentSegment == "0") return currentExpr
            if (currentSegment.isEmpty() && token == "00") return "${currentExpr}0"
        }

        return currentExpr + token
    }

    fun evaluate(expression: String): Double {
        var cleaned = expression
            .replace('×', '*')
            .replace('÷', '/')
            .replace(",", "")
            .trim()

        while (cleaned.isNotEmpty() && cleaned.last() in setOf('+', '-', '*', '/', '%', '.')) {
            cleaned = cleaned.dropLast(1)
        }
        if (cleaned.isBlank() || cleaned == "-") return 0.0

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) return 0.0

            // Pass 1: *, /, %
            val pass1 = mutableListOf<String>()
            var i = 0
            while (i < tokens.size) {
                val t = tokens[i]
                if ((t == "*" || t == "/" || t == "%") && pass1.isNotEmpty() && i + 1 < tokens.size) {
                    val left = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: 0.0
                    val right = tokens[i + 1].toDoubleOrNull() ?: 0.0
                    val res = when (t) {
                        "*" -> left * right
                        "/" -> if (abs(right) > 1e-9) left / right else 0.0
                        "%" -> (left * right) / 100.0
                        else -> left
                    }
                    pass1.add(res.toString())
                    i += 2
                } else {
                    pass1.add(t)
                    i += 1
                }
            }

            // Pass 2: +, -
            var result = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
            var j = 1
            while (j + 1 < pass1.size) {
                val op = pass1[j]
                val nextVal = pass1[j + 1].toDoubleOrNull() ?: 0.0
                result = when (op) {
                    "+" -> result + nextVal
                    "-" -> result - nextVal
                    else -> result
                }
                j += 2
            }
            if (result.isNaN() || result.isInfinite()) 0.0 else result
        } catch (_: Throwable) {
            cleaned.toDoubleOrNull() ?: 0.0
        }
    }

    fun formatAmount(value: Double): String {
        if (abs(value) < 0.005) return "0"
        return if (abs(value - value.toLong()) < 0.005) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun hasOperator(expression: String): Boolean {
        val body = if (expression.startsWith("-")) expression.drop(1) else expression
        return body.any { it in operators }
    }

    private fun tokenize(expr: String): List<String> {
        val result = mutableListOf<String>()
        val numBuf = StringBuilder()
        expr.forEachIndexed { idx, ch ->
            val isUnaryMinus = (ch == '-' && (idx == 0 || expr[idx - 1] in setOf('+', '-', '*', '/', '%')))
            if (ch.isDigit() || ch == '.' || isUnaryMinus) {
                numBuf.append(ch)
            } else if (ch in setOf('+', '-', '*', '/', '%')) {
                if (numBuf.isNotEmpty()) {
                    result.add(numBuf.toString())
                    numBuf.clear()
                }
                result.add(ch.toString())
            }
        }
        if (numBuf.isNotEmpty()) {
            result.add(numBuf.toString())
        }
        return result
    }
}

/**
 * DigiKhata-style Live Amount Display Box + Expression Strip + Full Calculator Keypad.
 */
@Composable
fun LiveCalculatorAmountField(
    expression: String,
    onExpressionChange: (String) -> Unit,
    language: AppLanguage,
    label: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val computedValue = LiveCalculatorEngine.evaluate(expression)
    val formattedTotal = LiveCalculatorEngine.formatAmount(computedValue)
    val showExpressionStrip = LiveCalculatorEngine.hasOperator(expression)

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Live Total Box (like DigiKhata "Amount: Rs. 9950")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.8.dp, accentColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label ?: if (language == AppLanguage.URDU) "رقم (لائیو ٹوٹل)" else "Amount (Live Total)",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rs. $formattedTotal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expression.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onExpressionChange("") },
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (language == AppLanguage.URDU) "صاف کریں" else "Clear",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Live Expression Strip below box (e.g. "350+8600+300+700")
        if (showExpressionStrip) {
            Text(
                text = expression,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Force LTR layout for standard numeric calculator keypad regardless of Urdu/English RTL
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            CalculatorKeypadGrid(
                onKey = { token ->
                    onExpressionChange(LiveCalculatorEngine.appendToken(expression, token))
                },
                onDone = onDone
            )
        }
    }
}

@Composable
fun CalculatorKeypadGrid(
    onKey: (String) -> Unit,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val opBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    val opText = MaterialTheme.colorScheme.onSecondaryContainer
    val numText = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Row 1: %  ÷  ×  ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CalcKeyButton("%", opBg, opText, Modifier.weight(1f)) { onKey("%") }
            CalcKeyButton("÷", opBg, opText, Modifier.weight(1f)) { onKey("÷") }
            CalcKeyButton("×", opBg, opText, Modifier.weight(1f)) { onKey("×") }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(opBg)
                    .clickable { onKey("BACK") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = opText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Row 2: 7  8  9  -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CalcKeyButton("7", numBg, numText, Modifier.weight(1f)) { onKey("7") }
            CalcKeyButton("8", numBg, numText, Modifier.weight(1f)) { onKey("8") }
            CalcKeyButton("9", numBg, numText, Modifier.weight(1f)) { onKey("9") }
            CalcKeyButton("−", opBg, opText, Modifier.weight(1f), fontSize = 22) { onKey("-") }
        }

        // Rows 3, 4, 5: Left 3 columns (4,5,6 / 1,2,3 / 00,0,.) + Right column (Tall + and Green →)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CalcKeyButton("4", numBg, numText, Modifier.weight(1f)) { onKey("4") }
                    CalcKeyButton("5", numBg, numText, Modifier.weight(1f)) { onKey("5") }
                    CalcKeyButton("6", numBg, numText, Modifier.weight(1f)) { onKey("6") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CalcKeyButton("1", numBg, numText, Modifier.weight(1f)) { onKey("1") }
                    CalcKeyButton("2", numBg, numText, Modifier.weight(1f)) { onKey("2") }
                    CalcKeyButton("3", numBg, numText, Modifier.weight(1f)) { onKey("3") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CalcKeyButton("00", numBg, numText, Modifier.weight(1f)) { onKey("00") }
                    CalcKeyButton("0", numBg, numText, Modifier.weight(1f)) { onKey("0") }
                    CalcKeyButton(".", numBg, numText, Modifier.weight(1f)) { onKey(".") }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Tall '+' button (spans 2 rows = 46 + 7 + 46 = 99.dp)
                CalcKeyButton(
                    label = "+",
                    containerColor = opBg,
                    contentColor = opText,
                    modifier = Modifier.fillMaxWidth(),
                    heightDp = 99,
                    fontSize = 24
                ) { onKey("+") }

                // Green Done / → button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProfitGreen)
                        .clickable { onDone?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcKeyButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    heightDp: Int = 46,
    fontSize: Int = 18,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
