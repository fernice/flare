/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.Experimental
import org.fernice.flare.cssparser.NumberOrPercentage
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.ClampingMode
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.FontBaseSize
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.std.map
import org.fernice.std.mapErr
import java.io.Writer
import kotlin.Number
import org.fernice.flare.style.value.computed.CalcLengthOrPercentage as ComputedCalcLengthOrPercentage
import org.fernice.flare.style.value.computed.Percentage as ComputedPercentage

/**
 * A complex length consisting out of several lengths with different units as well as a percentage. Different
 * to [LengthOrPercentage] this is not a real monad as length and percentage may be both be present at the
 * same time. Other than that [CalcLengthOrPercentage] is the calculable equivalent to LengthOrPercentage.
 */
class CalcLengthOrPercentage(private val clampingMode: ClampingMode) : SpecifiedValue<ComputedCalcLengthOrPercentage>, ToCss {

    internal var absolute: AbsoluteLength? = null

    internal var em: Float? = null
    internal var ex: Float? = null
    internal var ch: Float? = null
    internal var rem: Float? = null

    internal var vw: Float? = null
    internal var vh: Float? = null
    internal var vmin: Float? = null
    internal var vmax: Float? = null

    internal var percentage: ComputedPercentage? = null

    fun toComputedValue(context: Context, baseSize: FontBaseSize): ComputedCalcLengthOrPercentage {
        var length = 0f

        absolute?.let { value ->
            length += value.toComputedValue(context).px()
        }

        for (fontRelativeLength in listOf(
            em?.let(FontRelativeLength::Em),
            ex?.let(FontRelativeLength::Ex),
            ch?.let(FontRelativeLength::Ch),
            rem?.let(FontRelativeLength::Rem)
        )) {
            fontRelativeLength?.let { value ->
                length += value.toComputedValue(context, baseSize).px()
            }
        }

        val viewportSize = context.viewportSizeForViewportUnitResolution()

        for (viewportPercentageLength in listOf(
            vw?.let(ViewportPercentageLength::Vw),
            vh?.let(ViewportPercentageLength::Vh),
            vmin?.let(ViewportPercentageLength::Vmin),
            vmax?.let(ViewportPercentageLength::Vmax)
        )) {
            viewportPercentageLength?.let { value ->
                length += value.toComputedValue(context, viewportSize).px()
            }
        }

        return ComputedCalcLengthOrPercentage(clampingMode, PixelLength(length), percentage)
    }

    override fun toComputedValue(context: Context): ComputedCalcLengthOrPercentage {
        return toComputedValue(context, FontBaseSize.CurrentStyle)
    }

    override fun toCss(writer: Writer) {
        var firstValue = true

        fun checkFirstValue(value: Number) {
            if (!firstValue) {
                writer.append(if (value.toDouble() < 0) " - " else " + ")
            } else {
                writer.append("-")
            }
            firstValue = false
        }


        writer.append("calc(")

        percentage?.let { percentage ->
            checkFirstValue(percentage.value)
            percentage.toCss(writer)
        }

        absolute?.let { absolute ->
            checkFirstValue(absolute.toPx())
            absolute.toCss(writer)
        }

        for (fontRelativeLength in listOf(
            em?.let(FontRelativeLength::Em),
            ex?.let(FontRelativeLength::Ex),
            ch?.let(FontRelativeLength::Ch),
            rem?.let(FontRelativeLength::Rem)
        )) {
            fontRelativeLength?.let { value ->
                checkFirstValue(value.sign())
                value.toCss(writer)
            }
        }

        for (viewportPercentageLength in listOf(
            vw?.let(ViewportPercentageLength::Vw),
            vh?.let(ViewportPercentageLength::Vh),
            vmin?.let(ViewportPercentageLength::Vmin),
            vmax?.let(ViewportPercentageLength::Vmax)
        )) {
            viewportPercentageLength?.let { value ->
                checkFirstValue(value.sign())
                value.toCss(writer)
            }
        }

        writer.append(')')
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
    fun toLengthOrPercentage(clampingMode: ClampingMode): Result<CalcLengthOrPercentage, Unit> {
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
    internal open fun reduceCalc(ret: CalcLengthOrPercentage, factor: Float): Result<Unit, Unit> {
        when (this) {
            is CalcNode.Percentage -> {
                ret.percentage = ComputedPercentage(
                    (ret.percentage?.value ?: 0f) + value * factor
                )
            }
            is CalcNode.Length -> {
                when (length) {
                    is NoCalcLength.Absolute -> {
                        ret.absolute = (ret.absolute ?: AbsoluteLength.Px(0f)) + length.length * factor
                    }
                    is NoCalcLength.FontRelative -> {
                        when (val rel = length.length) {
                            is FontRelativeLength.Em -> {
                                ret.em = (ret.em ?: 0f) + rel.value * factor
                            }
                            is FontRelativeLength.Ex -> {
                                ret.ex = (ret.ex ?: 0f) + rel.value * factor
                            }
                            is FontRelativeLength.Ch -> {
                                ret.ch = (ret.ch ?: 0f) + rel.value * factor
                            }
                            is FontRelativeLength.Rem -> {
                                ret.rem = (ret.rem ?: 0f) + rel.value * factor
                            }
                        }
                    }
                    is NoCalcLength.ViewportPercentage -> {
                        when (val rel = length.length) {
                            is ViewportPercentageLength.Vw -> {
                                ret.vw = (ret.vw ?: 0f) + rel.value * factor
                            }
                            is ViewportPercentageLength.Vh -> {
                                ret.vh = (ret.vh ?: 0f) + rel.value * factor
                            }
                            is ViewportPercentageLength.Vmin -> {
                                ret.vmin = (ret.vmin ?: 0f) + rel.value * factor
                            }
                            is ViewportPercentageLength.Vmax -> {
                                ret.vmax = (ret.vmax ?: 0f) + rel.value * factor
                            }
                        }
                    }
                }
            }
            is CalcNode.Sum -> {
                when (val result = left.reduceCalc(ret, factor)) {
                    is Err -> return result
                    else -> {}
                }

                when (val result = right.reduceCalc(ret, factor)) {
                    is Err -> return result
                    else -> {}
                }
            }
            is CalcNode.Sub -> {
                when (val result = left.reduceCalc(ret, factor)) {
                    is Err -> return result
                    else -> {}
                }

                when (val result = right.reduceCalc(ret, factor * -1)) {
                    is Err -> return result
                    else -> {}
                }
            }
            is CalcNode.Mul -> {
                when (val operandLeft = left.toNumber()) {
                    is Ok -> {
                        when (val result = right.reduceCalc(ret, factor * operandLeft.value)) {
                            is Err -> return result
                            else -> {}
                        }
                    }
                    is Err -> {
                        val operand = when (val operand = right.toNumber()) {
                            is Ok -> operand.value
                            is Err -> return operand
                        }

                        when (val result = left.reduceCalc(ret, factor * operand)) {
                            is Err -> return result
                            else -> {}
                        }
                    }
                }
            }
            is CalcNode.Div -> {
                val operand = when (val operand = right.toNumber()) {
                    is Ok -> operand.value
                    is Err -> return operand
                }

                if (operand == 0f) {
                    return Err()
                }

                when (val result = left.reduceCalc(ret, factor / operand)) {
                    is Err -> return result
                    else -> {}
                }
            }
            is CalcNode.Angle,
            is CalcNode.Time,
            is CalcNode.Number,
            -> return Err()
        }

        return Ok()
    }

    /**
     * Tries to simplify the calc expression to an [Float] representing a percentage. The expression may not contain any of
     * [CalcNode.Length], [CalcNode.Angle] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both
     * sides and in [CalcNode.Div] only on the right hand side.
     */
    open fun toPercentage(): Result<Float, Unit> {
        return Ok(
            when (this) {
                is Percentage -> this.value
                is Sum -> {
                    val left = when (val left = this.left.toPercentage()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toPercentage()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    left + right
                }
                is Sub -> {
                    val left = when (val left = this.left.toPercentage()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toPercentage()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    left - right
                }
                is Mul -> {
                    when (val leftResult = this.left.toPercentage()) {
                        is Ok -> {
                            val left = leftResult.value

                            val right = when (val right = this.right.toNumber()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            left * right
                        }
                        is Err -> {
                            val left = when (val left = this.left.toNumber()) {
                                is Ok -> left.value
                                is Err -> return left
                            }

                            val right = when (val right = this.right.toPercentage()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            left * right
                        }
                    }
                }
                is Div -> {
                    val left = when (val left = this.left.toPercentage()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toNumber()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    if (right == 0.0f) {
                        return Err()
                    }

                    left / right
                }
                is Length,
                is Number,
                is Time,
                is Angle,
                -> return Err()
            }
        )
    }

    /**
     * Tries to simplify the calc expression to a [Float]. The expression may not contain any of [CalcNode.Length],
     * [CalcNode.Percentage], [CalcNode.Time] and [CalcNode.Angle].
     */
    open fun toNumber(): Result<Float, Unit> {
        return Ok(
            when (this) {
                is Number -> this.value
                is Sum -> {
                    val left = when (val left = this.left.toNumber()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toNumber()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    left + right
                }
                is Sub -> {
                    val left = when (val left = this.left.toNumber()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toNumber()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    left - right
                }
                is Mul -> {
                    when (val leftResult = this.left.toNumber()) {
                        is Ok -> {
                            val left = leftResult.value

                            val right = when (val right = this.right.toNumber()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            left * right
                        }
                        is Err -> {
                            val left = when (val left = this.left.toNumber()) {
                                is Ok -> left.value
                                is Err -> return left
                            }

                            val right = when (val right = this.right.toNumber()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            left * right
                        }
                    }
                }
                is Div -> {
                    val left = when (val left = this.left.toNumber()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toNumber()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    if (right == 0.0f) {
                        return Err()
                    }

                    left / right
                }
                is Length,
                is Percentage,
                is Time,
                is Angle,
                -> return Err()
            }
        )
    }

    /**
     * Tries to simplify the calc expression to an [SpecifiedAngle]. The expression may not contain any of [CalcNode.Length],
     * [CalcNode.Percentage] and [CalcNode.Time] as well as [CalcNode.Number] only in [CalcNode.Mul] on both sides and in
     * [CalcNode.Div] only on the right hand side.
     */
    fun toAngle(): Result<SpecifiedAngle, Unit> {
        return Ok(
            when (this) {
                is Angle -> this.angle
                is Sum -> {
                    val left = when (val left = this.left.toAngle()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toAngle()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    SpecifiedAngle.fromCalc(left.degrees() + right.degrees())
                }
                is Sub -> {
                    val left = when (val left = this.left.toAngle()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toAngle()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    SpecifiedAngle.fromCalc(left.degrees() - right.degrees())
                }
                is Mul -> {
                    when (val leftResult = this.left.toAngle()) {
                        is Ok -> {
                            val left = leftResult.value

                            val right = when (val right = this.right.toNumber()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            SpecifiedAngle.fromCalc(left.degrees() * right)
                        }
                        is Err -> {
                            val left = when (val left = this.left.toNumber()) {
                                is Ok -> left.value
                                is Err -> return left
                            }

                            val right = when (val right = this.right.toAngle()) {
                                is Ok -> right.value
                                is Err -> return right
                            }

                            SpecifiedAngle.fromCalc(left * right.degrees())
                        }
                    }
                }
                is Div -> {
                    val left = when (val left = this.left.toAngle()) {
                        is Ok -> left.value
                        is Err -> return left
                    }

                    val right = when (val right = this.right.toNumber()) {
                        is Ok -> right.value
                        is Err -> return right
                    }

                    if (right == 0.0f) {
                        return Err()
                    }

                    SpecifiedAngle.fromCalc(left.degrees() / right)
                }
                is Number,
                is Length,
                is Percentage,
                is Time,
                -> return Err()
            }
        )
    }

    companion object {

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a number.
         * A calc() can only be parsed into a number if the expression does not contain any scalars other than numbers.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseNumber(context: ParserContext, input: Parser): Result<Float, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.NUMBER)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode.toNumber()
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into an integer.
         * A calc() can only be parsed into an integer if the expression does not contain any scalars other than numbers.
         * Truncates the fraction part of the number.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseInteger(context: ParserContext, input: Parser): Result<Int, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.INTEGER)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode.toNumber()
                .map { number -> number.toInt() }
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a length.
         * A calc() can only be parsed into a length if the expression does not contain any scalars other than lengths
         * as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseLength(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.LENGTH)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a percentage.
         * A calc() can only be parsed into a percentage if the expression does not contain any scalars other than percentages
         * as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parsePercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<Float, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.PERCENTAGE)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode.toPercentage()
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a [NumberOrPercentage].
         * A calc() can only be parsed into a NumberOrPercentage if the expression does not contain any scalars other than either
         * only percentages as well as numbers in multiplications on both sides and in division on the right hand side or numbers.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseNumberOrPercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<NumberOrPercentage, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.PERCENTAGE)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            val number = calcNode.toNumber()

            if (number is Ok) {
                return Ok(NumberOrPercentage.Number(number.value))
            }

            val percentage = calcNode.toPercentage()

            if (percentage is Ok) {
                return Ok(NumberOrPercentage.Percentage(percentage.value))
            }

            return Err(input.newError(ParseErrorKind.Unknown))
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into a [CalcLengthOrPercentage].
         * A calc() can only be parsed into a CalcLengthOrPercentage if the expression does not contain any scalars other than either
         * only lengths or percentages as well as numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseLengthOrPercentage(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<CalcLengthOrPercentage, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.LENGTH_OR_PERCENTAGE)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode.toLengthOrPercentage(clampingMode)
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression and then reduces it into an [SpecifiedAngle].
         * A calc() can only be parsed into an Angle if the expression does not contain any scalars other than angles as well as
         * numbers in multiplications on both sides and in division on the right hand side.
         * Expects the 'calc(' to have already been parsed.
         */
        fun parseAngle(context: ParserContext, input: Parser): Result<SpecifiedAngle, ParseError> {
            val calcNode = when (val calcNode = parse(context, input, CalcUnit.ANGLE)) {
                is Ok -> calcNode.value
                is Err -> return calcNode
            }

            return calcNode
                .toAngle()
                .mapErr { input.newError(ParseErrorKind.Unknown) }
        }

        /**
         * Tries to parse a the calc() portion of the input into a calc expression. Expects the 'calc(' to have already been parsed.
         *
         * Internally parses only the sum part of the expression, see [parseProduct] for the product part and [parseOne] for the
         * scalar part as well as the part in parentheses.
         */
        fun parse(
            context: ParserContext,
            input: Parser,
            expectedUnit: CalcUnit,
        ): Result<CalcNode, ParseError> {

            var root = when (val root = parseProduct(context, input, expectedUnit)) {
                is Ok -> root.value
                is Err -> return root
            }

            loop@
            while (true) {
                val state = input.state()

                val token = when (val token = input.nextIncludingWhitespace()) {
                    is Ok -> token.value
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

                        val innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                            is Ok -> innerToken.value
                            is Err -> return innerToken
                        }

                        when (innerToken) {
                            is Token.Plus -> {
                                val right = when (val right = parseProduct(context, input, expectedUnit)) {
                                    is Ok -> right.value
                                    is Err -> return right
                                }

                                root = CalcNode.Sum(root, right)
                            }
                            is Token.Minus -> {
                                val right = when (val right = parseProduct(context, input, expectedUnit)) {
                                    is Ok -> right.value
                                    is Err -> return right
                                }

                                root = CalcNode.Sub(root, right)
                            }
                            else -> Err(location.newUnexpectedTokenError(innerToken))
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
        private fun parseProduct(
            context: ParserContext,
            input: Parser,
            expectedUnit: CalcUnit,
        ): Result<CalcNode, ParseError> {
            var root = when (val root = parseOne(context, input, expectedUnit)) {
                is Ok -> root.value
                is Err -> return root
            }

            loop@
            while (true) {
                val state = input.state()

                val token = when (val token = input.nextIncludingWhitespace()) {
                    is Ok -> token.value
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

                        val innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                            is Ok -> innerToken.value
                            is Err -> return innerToken
                        }

                        when (innerToken) {
                            is Token.Asterisk -> {
                                val right = when (val right = parseOne(context, input, expectedUnit)) {
                                    is Ok -> right.value
                                    is Err -> return right
                                }

                                root = CalcNode.Mul(root, right)
                            }
                            is Token.Solidus -> {
                                val right = when (val right = parseOne(context, input, expectedUnit)) {
                                    is Ok -> right.value
                                    is Err -> return right
                                }

                                root = CalcNode.Div(root, right)
                            }
                            else -> Err(location.newUnexpectedTokenError(innerToken))
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
        private fun parseOne(
            context: ParserContext,
            input: Parser,
            expectedUnit: CalcUnit,
        ): Result<CalcNode, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Number -> {
                    Ok(Number(token.number.float()))
                }
                is Token.Dimension -> {
                    when (expectedUnit) {
                        CalcUnit.LENGTH,
                        CalcUnit.LENGTH_OR_PERCENTAGE,
                        -> {
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

                    Err(location.newUnexpectedTokenError(token))
                }
                is Token.Percentage -> {
                    if (expectedUnit == CalcUnit.PERCENTAGE || expectedUnit == CalcUnit.LENGTH_OR_PERCENTAGE) {
                        return Ok(Percentage(token.number.float()))
                    }

                    Err(location.newUnexpectedTokenError(token))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        Err(location.newUnexpectedTokenError(token))
                    } else {
                        input.parseNestedBlock { nestedParser -> parse(context, nestedParser, expectedUnit) }
                    }
                }
                is Token.LParen -> {
                    input.parseNestedBlock { nestedParser -> parse(context, nestedParser, expectedUnit) }
                }
                else -> {
                    Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }
}
