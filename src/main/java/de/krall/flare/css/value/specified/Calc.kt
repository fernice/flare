package de.krall.flare.css.value.specified

import de.krall.flare.Experimental
import de.krall.flare.css.ParserContext
import de.krall.flare.css.value.ClampingMode
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.FontBaseSize
import de.krall.flare.css.value.SpecifiedValue
import de.krall.flare.css.value.computed.PixelLength
import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import de.krall.flare.css.value.computed.CalcLengthOrPercentage as ComputedCalcLengthOrPercentage
import de.krall.flare.css.value.computed.Percentage as ComputedPercentage

enum class CalcUnit {

    NUMBER,

    INTEGER,

    LENGTH,

    PERCENTAGE,

    LENGTH_OR_PERCENTAGE,

    @Experimental
    ANGLE,

    @Experimental
    TIME;
}

class CalcLengthOrPercentage(private val clampingMode: ClampingMode) : SpecifiedValue<ComputedCalcLengthOrPercentage> {

    internal var absolute: Option<AbsoluteLength> = None()

    internal var em: Option<Float> = None()
    internal var ex: Option<Float> = None()
    internal var ch: Option<Float> = None()
    internal var rem: Option<Float> = None()

    internal var vw: Option<Float> = None()
    internal var vh: Option<Float> = None()
    internal var vmin: Option<Float> = None()
    internal var vmax: Option<Float> = None()

    internal var percentage: Option<ComputedPercentage> = None()

    fun toComputedValue(context: Context, baseSize: FontBaseSize): ComputedCalcLengthOrPercentage {
        var length = 0f

        absolute.let { value ->
            length += value.toComputedValue(context).px()
        }

        for (fontRelativeLength in listOf(
                em.map(FontRelativeLength::Em),
                ex.map(FontRelativeLength::Ex),
                ch.map(FontRelativeLength::Ch),
                rem.map(FontRelativeLength::Rem)
        )) {
            fontRelativeLength.let { value ->
                length += value.toComputedValue(context, baseSize).px()
            }
        }

        val viewportSize = context.viewportSizeForViewportUnitResolution()

        for (viewportPercentageLength in listOf(
                vw.map(ViewportPercentageLength::Vw),
                vh.map(ViewportPercentageLength::Vh),
                vmin.map(ViewportPercentageLength::Vmin),
                vmax.map(ViewportPercentageLength::Vmax)
        )) {
            viewportPercentageLength.let { value ->
                length += value.toComputedValue(context, viewportSize).px()
            }
        }

        return ComputedCalcLengthOrPercentage(clampingMode, PixelLength(length), percentage)
    }

    override fun toComputedValue(context: Context): ComputedCalcLengthOrPercentage {
        return toComputedValue(context, FontBaseSize.CurrentStyle())
    }
}

sealed class CalcNode {

    companion object {

        fun parseNumber(context: ParserContext, input: Parser): Result<Float, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.NUMBER)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toNumber()
                    .mapErr { input.newError(ParseErrorKind.Unkown()) }
        }

        fun parseInteger(context: ParserContext, input: Parser): Result<Int, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.INTEGER)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toNumber()
                    .map { number -> number.toInt() }
                    .mapErr { input.newError(ParseErrorKind.Unkown()) }
        }

        fun parseLenth(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.LENGTH)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                    .mapErr { input.newError(ParseErrorKind.Unkown()) }
        }

        fun parsePercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<Float, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.PERCENTAGE)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toPercentage()
                    .mapErr { input.newError(ParseErrorKind.Unkown()) }
        }

        fun parseNumberOrPercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<NumberOrPercentage, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.PERCENTAGE)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            val number = calcNode.toNumber()

            if (number is Ok) {
                return Ok(NumberOrPercentage.Number(number.value))
            }

            val percentage = calcNode.toPercentage()

            if (percentage is Ok) {
                return Ok(NumberOrPercentage.Percentage(percentage.value))
            }

            return Err(input.newError(ParseErrorKind.Unkown()))
        }

        fun parseLenthOrPercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.LENGTH_OR_PERCENTAGE)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                    .mapErr { input.newError(ParseErrorKind.Unkown()) }
        }

        fun parse(context: ParserContext,
                  input: Parser,
                  expectedUnit: CalcUnit): Result<CalcNode, ParseError> {

            val rootResult = parseProduct(context, input, expectedUnit)

            var root = when (rootResult) {
                is Ok -> rootResult.value
                is Err -> return rootResult
            }

            loop@
            while (true) {
                val state = input.state()

                var tokenResult = input.nextIncludingWhitespace()

                var token = when (tokenResult) {
                    is Ok -> tokenResult.value
                    is Err -> {
                        input.reset(state)
                        break@loop
                    }
                }

                when (token) {
                    is Token.Whitespace -> {
                        if (input.isExhausted()) {
                            break@loop
                        }

                        val location = input.sourceLocation()
                        tokenResult = input.nextIncludingWhitespace()

                        token = when (tokenResult) {
                            is Ok -> tokenResult.value
                            is Err -> return tokenResult
                        }

                        when (token) {
                            is Token.Plus -> {
                                val rightResult = parseProduct(context, input, expectedUnit)
                                val right = when (rightResult) {
                                    is Ok -> rightResult.value
                                    is Err -> return rightResult
                                }

                                root = CalcNode.Sum(root, right)
                            }
                            is Token.Minus -> {
                                val rightResult = parseProduct(context, input, expectedUnit)
                                val right = when (rightResult) {
                                    is Ok -> rightResult.value
                                    is Err -> return rightResult
                                }

                                root = CalcNode.Sub(root, right)
                            }
                            else -> Err(location.newUnexpectedTokenError(token))
                        }
                    }
                    else -> {
                        input.reset(state)
                        break@loop
                    }
                }
            }

            return Ok(root)
        }

        private fun parseProduct(context: ParserContext,
                                 input: Parser,
                                 expectedUnit: CalcUnit): Result<CalcNode, ParseError> {
            val rootResult = parseOne(context, input, expectedUnit)

            var root = when (rootResult) {
                is Ok -> rootResult.value
                is Err -> return rootResult
            }

            loop@
            while (true) {
                val state = input.state()

                var tokenResult = input.nextIncludingWhitespace()

                var token = when (tokenResult) {
                    is Ok -> tokenResult.value
                    is Err -> {
                        input.reset(state)
                        break@loop
                    }
                }

                when (token) {
                    is Token.Whitespace -> {
                        if (input.isExhausted()) {
                            break@loop
                        }

                        val location = input.sourceLocation()
                        tokenResult = input.nextIncludingWhitespace()

                        token = when (tokenResult) {
                            is Ok -> tokenResult.value
                            is Err -> return tokenResult
                        }

                        when (token) {
                            is Token.Asterisk -> {
                                val rightResult = parseOne(context, input, expectedUnit)
                                val right = when (rightResult) {
                                    is Ok -> rightResult.value
                                    is Err -> return rightResult
                                }

                                root = CalcNode.Mul(root, right)
                            }
                            is Token.Solidus -> {
                                val rightResult = parseOne(context, input, expectedUnit)
                                val right = when (rightResult) {
                                    is Ok -> rightResult.value
                                    is Err -> return rightResult
                                }

                                root = CalcNode.Div(root, right)
                            }
                            else -> Err(location.newUnexpectedTokenError(token))
                        }
                    }
                    else -> {
                        input.reset(state)
                        break@loop
                    }
                }
            }

            return Ok(root)
        }

        private fun parseOne(context: ParserContext,
                             input: Parser,
                             expectedUnit: CalcUnit): Result<CalcNode, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            when (token) {
                is Token.Number -> {
                    return Ok(CalcNode.Number(token.number.float()))
                }
                is Token.Dimension -> {
                    if (expectedUnit == CalcUnit.LENGTH || expectedUnit == CalcUnit.LENGTH_OR_PERCENTAGE) {
                        return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                                .map { length -> CalcNode.Length(length) }
                                .mapErr { location.newUnexpectedTokenError(token) }
                    }

                    return Err(location.newUnexpectedTokenError(token))
                }
                is Token.Percentage -> {
                    if (expectedUnit == CalcUnit.PERCENTAGE || expectedUnit == CalcUnit.LENGTH_OR_PERCENTAGE) {
                        return Ok(CalcNode.Percentage(token.number.float()))
                    }

                    return Err(location.newUnexpectedTokenError(token))
                }
                is Token.Function -> {
                    return if (!token.name.equals("calc", true)) {
                        Err(location.newUnexpectedTokenError(token))
                    } else {
                        input.parseNestedBlock { input -> parse(context, input, expectedUnit) }
                    }
                }
                is Token.LParen -> {
                    return input.parseNestedBlock { input -> parse(context, input, expectedUnit) }
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }

    fun toLengthOrPercentage(clampingMode: ClampingMode): Result<CalcLengthOrPercentage, Empty> {
        val ret = CalcLengthOrPercentage(clampingMode)

        val result = reduceCalc(ret, 1f)

        return when (result) {
            is Ok -> Ok(ret)
            is Err -> result
        }
    }

    internal abstract fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty>

    abstract fun toNumber(): Result<Float, Empty>

    abstract fun toPercentage(): Result<Float, Empty>

    class Length(val length: NoCalcLength) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            when (length) {
                is NoCalcLength.Absolute -> {
                    ret.absolute = when (ret.absolute) {
                        is Some -> Some(ret.absolute.unwrap() + length.length * factor)
                        is None -> Some(length.length * factor)
                    }
                }
                is NoCalcLength.FontRelative -> {
                    val rel = length.length
                    when (rel) {
                        is FontRelativeLength.Em -> {
                            ret.em = Some(ret.em.unwrapOr(0f) + rel.value * factor)
                        }
                        is FontRelativeLength.Ex -> {
                            ret.ex = Some(ret.ex.unwrapOr(0f) + rel.value * factor)
                        }
                        is FontRelativeLength.Ch -> {
                            ret.ch = Some(ret.ch.unwrapOr(0f) + rel.value * factor)
                        }
                        is FontRelativeLength.Rem -> {
                            ret.rem = Some(ret.rem.unwrapOr(0f) + rel.value * factor)
                        }
                    }
                }
                is NoCalcLength.ViewportPercentage -> {
                    val rel = length.length
                    when (rel) {
                        is ViewportPercentageLength.Vw -> {
                            ret.vw = Some(ret.vw.unwrapOr(0f) + rel.value * factor)
                        }
                        is ViewportPercentageLength.Vh -> {
                            ret.vh = Some(ret.vh.unwrapOr(0f) + rel.value * factor)
                        }
                        is ViewportPercentageLength.Vmin -> {
                            ret.vmin = Some(ret.vmin.unwrapOr(0f) + rel.value * factor)
                        }
                        is ViewportPercentageLength.Vmax -> {
                            ret.vmax = Some(ret.vmax.unwrapOr(0f) + rel.value * factor)
                        }
                    }
                }
            }

            return Ok()
        }

        override fun toNumber(): Result<Float, Empty> {
            return Err()
        }

        override fun toPercentage(): Result<Float, Empty> {
            return Err()
        }
    }

    class Percentage(val value: Float) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            ret.percentage = Some(ComputedPercentage(
                    ret.percentage.mapOr({ p -> p.value }, 0f) + value * factor
            ))

            return Ok()
        }

        override fun toNumber(): Result<Float, Empty> {
            return Err()
        }

        override fun toPercentage(): Result<Float, Empty> {
            return Ok(value)
        }
    }

    class Number(val value: Float) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            return Err()
        }

        override fun toNumber(): Result<Float, Empty> {
            return Ok(value)
        }

        override fun toPercentage(): Result<Float, Empty> {
            return Err()
        }
    }

    class Sum(val left: CalcNode, val right: CalcNode) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            val leftResult = left.reduceCalc(ret, factor)

            if (leftResult is Err) {
                return leftResult
            }

            return right.reduceCalc(ret, factor)
        }

        override fun toNumber(): Result<Float, Empty> {
            val leftNumber = left.toNumber()

            val left = when (leftNumber) {
                is Ok -> leftNumber.value
                is Err -> return leftNumber
            }

            val rightNumber = right.toNumber()

            val right = when (rightNumber) {
                is Ok -> rightNumber.value
                is Err -> return rightNumber
            }

            return Ok(left + right)
        }

        override fun toPercentage(): Result<Float, Empty> {
            val leftPercentage = left.toPercentage()

            val left = when (leftPercentage) {
                is Ok -> leftPercentage.value
                is Err -> return leftPercentage
            }

            val rightPercentage = right.toPercentage()

            val right = when (rightPercentage) {
                is Ok -> rightPercentage.value
                is Err -> return rightPercentage
            }

            return Ok(left + right)
        }
    }

    class Sub(val left: CalcNode, val right: CalcNode) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            val leftResult = left.reduceCalc(ret, factor)

            if (leftResult is Err) {
                return leftResult
            }

            return right.reduceCalc(ret, factor * -1)
        }

        override fun toNumber(): Result<Float, Empty> {
            val leftNumber = left.toNumber()

            val left = when (leftNumber) {
                is Ok -> leftNumber.value
                is Err -> return leftNumber
            }

            val rightNumber = right.toNumber()

            val right = when (rightNumber) {
                is Ok -> rightNumber.value
                is Err -> return rightNumber
            }

            return Ok(left - right)
        }

        override fun toPercentage(): Result<Float, Empty> {
            val leftPercentage = left.toPercentage()

            val left = when (leftPercentage) {
                is Ok -> leftPercentage.value
                is Err -> return leftPercentage
            }

            val rightPercentage = right.toPercentage()

            val right = when (rightPercentage) {
                is Ok -> rightPercentage.value
                is Err -> return rightPercentage
            }

            return Ok(left - right)
        }
    }

    class Mul(val left: CalcNode, val right: CalcNode) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            var operand = left.toNumber()

            return when (operand) {
                is Ok -> {
                    right.reduceCalc(ret, factor * operand.value)
                }
                is Err -> {
                    operand = right.toNumber()

                    when (operand) {
                        is Ok -> left.reduceCalc(ret, factor * operand.value)
                        is Err -> operand
                    }
                }
            }
        }

        override fun toNumber(): Result<Float, Empty> {
            var operand = left.toPercentage()

            return when (operand) {
                is Ok -> {
                    val value = right.toNumber()

                    when (value) {
                        is Ok -> Ok(operand.value * value.value)
                        is Err -> value
                    }
                }
                is Err -> {
                    operand = right.toPercentage()

                    val leftOperand = when (operand) {
                        is Ok -> operand.value
                        is Err -> return operand
                    }

                    val value = left.toNumber()

                    val rightOperand = when (value) {
                        is Ok -> value.value
                        is Err -> return value
                    }

                    Ok(leftOperand * rightOperand)
                }
            }
        }

        override fun toPercentage(): Result<Float, Empty> {
            var operand = left.toNumber()

            return when (operand) {
                is Ok -> {
                    val value = right.toNumber()

                    when (value) {
                        is Ok -> Ok(operand.value * value.value)
                        is Err -> value
                    }
                }
                is Err -> {
                    operand = right.toNumber()

                    val leftOperand = when (operand) {
                        is Ok -> operand.value
                        is Err -> return operand
                    }

                    val value = left.toNumber()

                    val rightOperand = when (value) {
                        is Ok -> value.value
                        is Err -> return value
                    }

                    Ok(leftOperand * rightOperand)
                }
            }
        }
    }

    class Div(val left: CalcNode, val right: CalcNode) : CalcNode() {
        override fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
            val operandResult = right.toNumber()

            val operand = when (operandResult) {
                is Ok -> operandResult.value
                is Err -> return operandResult
            }

            if (operand == 0f) {
                return Err()
            }

            return left.reduceCalc(ret, factor / operand)
        }

        override fun toNumber(): Result<Float, Empty> {
            val operandResult = right.toNumber()

            val operand = when (operandResult) {
                is Ok -> operandResult.value
                is Err -> return operandResult
            }

            if (operand == 0f) {
                return Err()
            }

            val number = left.toNumber()

            return when (number) {
                is Ok -> Ok(number.value / operand)
                is Err -> number
            }
        }

        override fun toPercentage(): Result<Float, Empty> {
            val operandResult = right.toNumber()

            val operand = when (operandResult) {
                is Ok -> operandResult.value
                is Err -> return operandResult
            }

            if (operand == 0f) {
                return Err()
            }

            val percentage = left.toPercentage()

            return when (percentage) {
                is Ok -> Ok(percentage.value / operand)
                is Err -> percentage
            }
        }
    }
}