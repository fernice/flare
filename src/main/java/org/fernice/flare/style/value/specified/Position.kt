/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.Parse
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.CalcLengthOrPercentage
import org.fernice.flare.style.value.computed.Percentage
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.mapOr
import fernice.std.unwrap
import fernice.std.unwrapOr
import org.fernice.flare.cssparser.ToCss
import java.io.Writer
import org.fernice.flare.style.value.computed.LengthOrPercentage as ComputedLengthOrPercentage
import org.fernice.flare.style.value.computed.Position as ComputedPosition

class Position(
    val horizontal: HorizontalPosition,
    val vertical: VerticalPosition
) : SpecifiedValue<ComputedPosition>, ToCss {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Position, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<Position, ParseError> {
            val xPosResult = input.tryParse { parser -> PositionComponent.parseQuirky(context, parser, allowQuirks, X.Companion) }

            when (xPosResult) {
                is Ok -> {
                    val xPos = xPosResult.value

                    when (xPos) {
                        is PositionComponent.Center -> {
                            val yPosResult = input.tryParse { parser -> PositionComponent.parseQuirky(context, parser, allowQuirks, Y.Companion) }

                            if (yPosResult is Ok) {
                                return Ok(Position(xPos, yPosResult.value))
                            }

                            val xPos = input.tryParse { parser -> PositionComponent.parseQuirky(context, parser, allowQuirks, X.Companion) }
                                .unwrapOr(xPos)
                            val yPos = PositionComponent.Center<Y>()

                            return Ok(Position(xPos, yPos))
                        }
                        is PositionComponent.Side<X> -> {
                            if (input.tryParse { parser -> parser.expectIdentifierMatching("center") }.isOk()) {
                                val yPos = PositionComponent.Center<Y>()

                                return Ok(Position(xPos, yPos))
                            }

                            val ySide = input.tryParse(Y.Companion::parse)

                            if (ySide is Ok) {
                                val yLop = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }
                                    .ok()

                                val yPos = PositionComponent.Side(ySide.value, yLop)

                                return Ok(Position(xPos, yPos))
                            }


                            val (side, lop) = xPos

                            val xPosSplit = PositionComponent.Side(side, None)
                            val yPos = lop.mapOr({ lop -> PositionComponent.Length<Y>(lop) }, PositionComponent.Center<Y>())

                            return Ok(Position(xPosSplit, yPos))
                        }
                        is PositionComponent.Length -> {
                            val ySide = input.tryParse(Y.Companion::parse)

                            if (ySide is Ok) {
                                val yPos = PositionComponent.Side(ySide.value, None)

                                return Ok(Position(xPos, yPos))
                            }

                            val yLop = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }
                            if (yLop is Ok) {
                                val yPos = PositionComponent.Length<Y>(yLop.value)

                                return Ok(Position(xPos, yPos))
                            }

                            val yPos = PositionComponent.Center<Y>()
                            input.tryParse { parser -> parser.expectIdentifierMatching("center") }

                            return Ok(Position(xPos, yPos))
                        }
                    }
                }
                is Err -> {
                    val yKeywordResult = Y.parse(input)

                    val yKeyword = when (yKeywordResult) {
                        is Ok -> yKeywordResult.value
                        is Err -> return yKeywordResult
                    }

                    val sideResult: Result<Pair<Option<LengthOrPercentage>, PositionComponent<X>>, ParseError> = input.tryParse { parser ->
                        val yLop = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }
                            .ok()

                        val xKeyword = parser.tryParse(X.Companion::parse)
                        if (xKeyword is Ok) {
                            val xLop = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }
                                .ok()
                            val xPos = PositionComponent.Side(xKeyword.value, xLop)

                            return@tryParse Ok(yLop to xPos)
                        }

                        val centerKeyword = parser.expectIdentifierMatching("center")
                        if (centerKeyword is Err) {
                            return@tryParse Err(centerKeyword.value)
                        }

                        val xPos = PositionComponent.Center<X>()
                        return@tryParse Ok(yLop to xPos)
                    }

                    if (sideResult is Ok) {
                        val (yLop, xPos) = sideResult.value

                        val yPos = PositionComponent.Side(yKeyword, yLop)

                        return Ok(Position(xPos, yPos))
                    }

                    val xPos = PositionComponent.Center<X>()
                    val yPos = PositionComponent.Side(yKeyword, None)

                    return Ok(Position(xPos, yPos))
                }
            }
        }

        fun center(): Position {
            return Position(
                PositionComponent.Center(),
                PositionComponent.Center()
            )
        }
    }

    override fun toComputedValue(context: Context): ComputedPosition {
        return ComputedPosition(
            horizontal.toComputedValue(context),
            vertical.toComputedValue(context)
        )
    }

    override fun toCss(writer: Writer) {
        TODO("implement toCss(write) for position")
    }
}

typealias HorizontalPosition = PositionComponent<X>

typealias VerticalPosition = PositionComponent<Y>

sealed class PositionComponent<S : Side> : SpecifiedValue<ComputedLengthOrPercentage>, ToCss {

    class Center<S : org.fernice.flare.style.value.specified.Side> : PositionComponent<S>()

    class Length<S : org.fernice.flare.style.value.specified.Side>(val length: LengthOrPercentage) : PositionComponent<S>()

    data class Side<S : org.fernice.flare.style.value.specified.Side>(val side: S, val length: Option<LengthOrPercentage>) : PositionComponent<S>()

    override fun toCss(writer: Writer) {
        TODO("implement toCss(write) for position-position")
    }

    companion object {

        fun <S : org.fernice.flare.style.value.specified.Side> parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks,
            parse: Parse<S>
        ): Result<PositionComponent<S>, ParseError> {
            if (input.tryParse { parser -> parser.expectIdentifierMatching("center") }.isOk()) {
                return Ok(Center())
            }

            val lopResult = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }

            if (lopResult is Ok) {
                return Ok(Length(lopResult.value))
            }

            val sideResult = parse.parse(context, input)

            val side = when (sideResult) {
                is Ok -> sideResult.value
                is Err -> return sideResult
            }

            val lop = input.tryParse { parser -> LengthOrPercentage.parseQuirky(context, parser, allowQuirks) }.ok()

            return Ok(Side(side, lop))
        }
    }

    override fun toComputedValue(context: Context): ComputedLengthOrPercentage {
        return when (this) {
            is Center -> ComputedLengthOrPercentage.fifty()
            is Side -> {
                if (this.length is None) {
                    val percentage = Percentage(
                        if (this.side.isStart()) {
                            0.0f
                        } else {
                            1.0f
                        }
                    )
                    return ComputedLengthOrPercentage.Percentage(percentage)
                }

                val length = this.length.unwrap()

                if (this.side.isStart()) {
                    length.toComputedValue(context)
                } else {
                    val computed = length.toComputedValue(context)

                    when (computed) {
                        is ComputedLengthOrPercentage.Length -> {
                            ComputedLengthOrPercentage.Calc(
                                CalcLengthOrPercentage.new(
                                    -computed.length, Some(Percentage.hundred())
                                )
                            )
                        }
                        is ComputedLengthOrPercentage.Percentage -> {
                            ComputedLengthOrPercentage.Percentage(
                                Percentage(
                                    1.0f - computed.percentage.value
                                )
                            )
                        }
                        is ComputedLengthOrPercentage.Calc -> {
                            val p = Percentage(1.0f - computed.calc.percentage.mapOr({ p -> p.value }, 0.0f))
                            val l = -computed.calc.unclampedLength()
                            ComputedLengthOrPercentage.Calc(CalcLengthOrPercentage.new(l, Some(p)))
                        }
                    }
                }
            }
            is Length -> this.length.toComputedValue(context)
        }
    }
}

interface Side {

    fun isStart(): Boolean
}

enum class X : Side {

    Left,

    Right;

    override fun isStart(): Boolean {
        return when (this) {
            Left -> true
            else -> false
        }
    }

    companion object : Parse<X> {

        override fun parse(context: ParserContext, input: Parser): Result<X, ParseError> {
            return parse(input)
        }

        fun parse(input: Parser): Result<X, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "left" -> Ok(Left)
                "right" -> Ok(Right)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}

enum class Y : Side {

    Top,

    Bottom;

    override fun isStart(): Boolean {
        return when (this) {
            Top -> true
            else -> false
        }
    }

    companion object : Parse<Y> {

        override fun parse(context: ParserContext, input: Parser): Result<Y, ParseError> {
            return parse(input)
        }

        fun parse(input: Parser): Result<Y, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "top" -> Ok(Top)
                "bottom" -> Ok(Bottom)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}
