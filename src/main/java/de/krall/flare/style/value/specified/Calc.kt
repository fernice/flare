package de.krall.flare.style.value.specified

import de.krall.flare.Experimental
import de.krall.flare.cssparser.NumberOrPercentage
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.std.Some
import de.krall.flare.std.let
import de.krall.flare.std.mapOr
import de.krall.flare.std.unwrap
import de.krall.flare.std.unwrapOr
import de.krall.flare.style.parser.ClampingMode
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.FontBaseSize
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.PixelLength
import de.krall.flare.style.value.computed.CalcLengthOrPercentage as ComputedCalcLengthOrPercentage
import de.krall.flare.style.value.computed.Percentage as ComputedPercentage

/**
 * A complex length consisting out of several lengths with different units as well as a percentage. Different
 * to [LengthOrPercentage] this is not a real monad as length and percentage may be both be present at the
 * same time. Other than that [CalcLengthOrPercentage] is the calculable equivalent to LengthOrPercentage.
 */
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

/**
 * The expected unit of a calc expression.
 */
enum class CalcUnit {

    NUMBER,

    INTEGER,

    LENGTH,

    PERCENTAGE,

    LENGTH_OR_PERCENTAGE,

    ANGLE,

    @Experimental
    TIME;
}

/**
 * Internal type for readability purposes only.
 */
private typealias SpecifiedAngle = Angle

/**
 * An abstract representation of a calc() expression in CSS. A single calc node may be an operation like add, subtract,
 * multiply or divide or a scalar of a type like a length, percentage, number, angle or time. As its abstract it cannot
 * fully restore the original input that had been used during parsing as for example '()' and 'calc()' are being
 * coalesced into the same expression.
 */
sealed class CalcNode {

    class Length(val length: NoCalcLength) : CalcNode()

    class Percentage(val value: Float) : CalcNode()

    class Number(val value: Float) : CalcNode()

    class Angle(val angle: SpecifiedAngle) : CalcNode()

    class Time(val value: Float) : CalcNode()

    class Sum(val left: CalcNode, val right: CalcNode) : CalcNode()

    class Sub(val left: CalcNode, val right: CalcNode) : CalcNode()

    class Mul(val left: CalcNode, val right: CalcNode) : CalcNode()

    class Div(val left: CalcNode, val right: CalcNode) : CalcNode()

    /**
     * Tires to convert the calc expression into a [CalcLengthOrPercentage]. The expression may not contain any of
     * [CalcNode.Angle] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both sides and in
     * [CalcNode.Div] only on the right hand side.
     */
    fun toLengthOrPercentage(clampingMode: ClampingMode): Result<CalcLengthOrPercentage, Empty> {
        val ret = CalcLengthOrPercentage(clampingMode)

        val result = reduceCalc(ret, 1f)

        return when (result) {
            is Ok -> Ok(ret)
            is Err -> result
        }
    }

    /**
     * Tries to reduce the calc expression into a [CalcLengthOrPercentage]. The expression may not contain any of
     * [CalcNode.Angle] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both sides and in
     * [CalcNode.Div] only on the right hand side.
     */
    internal open fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Empty, Empty> {
        when (this) {
            is CalcNode.Percentage -> {
                ret.percentage = Some(ComputedPercentage(
                        ret.percentage.mapOr({ p -> p.value }, 0f) + value * factor
                ))
            }
            is CalcNode.Length -> {
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
            }
            is CalcNode.Sum -> {
                val leftResult = left.reduceCalc(ret, factor)
                if (leftResult is Err) {
                    return leftResult
                }

                val rightResult = right.reduceCalc(ret, factor)
                if (rightResult is Err) {
                    return rightResult
                }
            }
            is CalcNode.Sub -> {
                val leftResult = left.reduceCalc(ret, factor)
                if (leftResult is Err) {
                    return leftResult
                }

                val rightResult = right.reduceCalc(ret, factor * -1)
                if (rightResult is Err) {
                    return rightResult
                }
            }
            is CalcNode.Mul -> {
                var operand = left.toNumber()
                if (operand is Err) {
                    return operand
                }

                when (operand) {
                    is Ok -> {
                        val result = right.reduceCalc(ret, factor * operand.value)
                        if (result is Err) {
                            return result
                        }
                    }
                    is Err -> {
                        operand = right.toNumber()
                        if (operand is Err) {
                            return operand
                        }

                        val result = left.reduceCalc(ret, factor * operand.unwrap())
                        if (result is Err) {
                            return result
                        }
                    }
                }
            }
            is CalcNode.Div -> {
                val operandResult = right.toNumber()
                val operand = when (operandResult) {
                    is Ok -> operandResult.value
                    is Err -> return operandResult
                }

                if (operand == 0f) {
                    return Err()
                }

                val result = left.reduceCalc(ret, factor / operand)
                if (result is Err) {
                    return result
                }
            }
            is CalcNode.Angle,
            is CalcNode.Time,
            is CalcNode.Number -> return Err()
        }

        return Ok()
    }

    /**
     * Tries to simplify the calc expression to an [Float] representing a percentage. The expression may not contain any of
     * [CalcNode.Length], [CalcNode.Angle] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both
     * sides and in [CalcNode.Div] only on the right hand side.
     */
    open fun toPercentage(): Result<Float, Empty> {
        return Ok(when (this) {
            is CalcNode.Percentage -> this.value
            is CalcNode.Sum -> {
                val leftResult = this.left.toPercentage()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toPercentage()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                left + right
            }
            is CalcNode.Sub -> {
                val leftResult = this.left.toPercentage()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toPercentage()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                left - right
            }
            is CalcNode.Mul -> {
                val leftResult = this.left.toPercentage()
                when (leftResult) {
                    is Ok -> {
                        val left = leftResult.value

                        val rightResult = this.right.toNumber()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        left * right
                    }
                    is Err -> {
                        val leftResult = this.left.toNumber()
                        val left = when (leftResult) {
                            is Ok -> leftResult.value
                            is Err -> return leftResult
                        }

                        val rightResult = this.right.toPercentage()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        left * right
                    }
                }
            }
            is CalcNode.Div -> {
                val leftResult = this.left.toPercentage()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toNumber()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                if (right == 0.0f) {
                    return Err()
                }

                left / right
            }
            is CalcNode.Length,
            is CalcNode.Number,
            is CalcNode.Time,
            is CalcNode.Angle -> return Err()
        })
    }

    /**
     * Tries to simplify the calc expression to a [Float]. The expression may not contain any of [CalcNode.Length],
     * [CalcNode.Percentage], [CalcNode.Time] and [CalcNode.Angle].
     */
    open fun toNumber(): Result<Float, Empty> {
        return Ok(when (this) {
            is CalcNode.Number -> this.value
            is CalcNode.Sum -> {
                val leftResult = this.left.toNumber()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toNumber()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                left + right
            }
            is CalcNode.Sub -> {
                val leftResult = this.left.toNumber()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toNumber()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                left - right
            }
            is CalcNode.Mul -> {
                val leftResult = this.left.toNumber()
                when (leftResult) {
                    is Ok -> {
                        val left = leftResult.value

                        val rightResult = this.right.toNumber()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        left * right
                    }
                    is Err -> {
                        val leftResult = this.left.toNumber()
                        val left = when (leftResult) {
                            is Ok -> leftResult.value
                            is Err -> return leftResult
                        }

                        val rightResult = this.right.toNumber()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        left * right
                    }
                }
            }
            is CalcNode.Div -> {
                val leftResult = this.left.toNumber()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toNumber()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                if (right == 0.0f) {
                    return Err()
                }

                left / right
            }
            is CalcNode.Length,
            is CalcNode.Percentage,
            is CalcNode.Time,
            is CalcNode.Angle -> return Err()
        })
    }

    /**
     * Tries to simplify the calc expression to an [SpecifiedAngle]. The expression may not contain any of [CalcNode.Length],
     * [CalcNode.Percentage] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both sides and in
     * [CalcNode.Div] only on the right hand side.
     */
    fun toAngle(): Result<SpecifiedAngle, Empty> {
        return Ok(when (this) {
            is CalcNode.Angle -> this.angle
            is CalcNode.Sum -> {
                val leftResult = this.left.toAngle()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toAngle()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                SpecifiedAngle.fromCalc(left.radians() + right.radians())
            }
            is CalcNode.Sub -> {
                val leftResult = this.left.toAngle()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toAngle()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                SpecifiedAngle.fromCalc(left.radians() - right.radians())
            }
            is CalcNode.Mul -> {
                val leftResult = this.left.toAngle()
                when (leftResult) {
                    is Ok -> {
                        val left = leftResult.value

                        val rightResult = this.right.toNumber()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        SpecifiedAngle.fromCalc(left.radians() * right)
                    }
                    is Err -> {
                        val leftResult = this.left.toNumber()
                        val left = when (leftResult) {
                            is Ok -> leftResult.value
                            is Err -> return leftResult
                        }

                        val rightResult = this.right.toAngle()
                        val right = when (rightResult) {
                            is Ok -> rightResult.value
                            is Err -> return rightResult
                        }

                        SpecifiedAngle.fromCalc(left * right.radians())
                    }
                }
            }
            is CalcNode.Div -> {
                val leftResult = this.left.toAngle()
                val left = when (leftResult) {
                    is Ok -> leftResult.value
                    is Err -> return leftResult
                }

                val rightResult = this.right.toNumber()
                val right = when (rightResult) {
                    is Ok -> rightResult.value
                    is Err -> return rightResult
                }

                if (right == 0.0f) {
                    return Err()
                }

                SpecifiedAngle.fromCalc(left.radians() / right)
            }
            is CalcNode.Number,
            is CalcNode.Length,
            is CalcNode.Percentage,
            is CalcNode.Time -> return Err()
        })
    }

    companion object {

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a number.
         * A calc() can only be parsed into a number if the expression does not contain any scalars other than numbers.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseNumber(context: ParserContext, input: Parser): Result<Float, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.NUMBER)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toNumber()
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into an integer.
         * A calc() can only be parsed into an integer if the expression does not contain any scalars other than numbers.
         * Truncates the fraction part of the number.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseInteger(context: ParserContext, input: Parser): Result<Int, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.INTEGER)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toNumber()
                    .map { number -> number.toInt() }
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a length.
         * A calc() can only be parsed into a length if the expression does not contain any scalars other than lengths
         * as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseLength(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.LENGTH)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a percentage.
         * A calc() can only be parsed into a percentage if the expression does not contain any scalars other than percentages
         * as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parsePercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<Float, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.PERCENTAGE)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toPercentage()
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a [NumberOrPercentage].
         * A calc() can only be parsed into a NumberOrPercentage if the expression does not contain any scalars other than either
         * only percentages as well as numbers in multiplications on both sides and in division on the right hand side or numbers.
         * Expects the 'calc(' to have already been parsed.
         */
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

            return Err(input.newError(ParseErrorKind.Unkown))
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a [CalcLengthOrPercentage].
         * A calc() can only be parsed into a CalcLengthOrPercentage if the expression does not contain any scalars other than either
         * only lengths or percentages as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseLengthOrPercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.LENGTH_OR_PERCENTAGE)

            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into an [SpecifiedAngle].
         * A calc() can only be parsed into an Angle if the expression does not contain any scalars other than angles as well as
         * numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseAngle(context: ParserContext, input: Parser): Result<SpecifiedAngle, ParseError> {
            val calcNodeResult = parse(context, input, CalcUnit.ANGLE)
            val calcNode = when (calcNodeResult) {
                is Ok -> calcNodeResult.value
                is Err -> return calcNodeResult
            }

            return calcNode
                    .toAngle()
                    .mapErr { input.newError(ParseErrorKind.Unkown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression. Expects the 'calc(' to have already been parsed.
         *
         * Internally parses only the sum part of the expression, see [parseProduct] for the product part and [parseOne] for the
         * scalar part as well as the part in parentheses.
         */
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

        /**
         * Tries to parse a product.
         */
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

        /**
         * Tries to parse a scalar or nested (parenthesized) expression.
         */
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
                    when (expectedUnit) {
                        CalcUnit.LENGTH,
                        CalcUnit.LENGTH_OR_PERCENTAGE -> {
                            return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                                    .map(CalcNode::Length)
                                    .mapErr { location.newUnexpectedTokenError(token) }
                        }
                        CalcUnit.ANGLE -> {
                            return SpecifiedAngle.parseDimension(token.number.float(), token.unit, true)
                                    .map(CalcNode::Angle)
                                    .mapErr { location.newUnexpectedTokenError(token) }
                        }
                        else -> {
                        }
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
}