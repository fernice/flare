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
import org.fernice.flare.style.parser.Parse
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import fernice.std.Empty
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.unwrapOr
import org.fernice.flare.cssparser.ToCss
import java.io.Writer
import org.fernice.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat
import org.fernice.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize

sealed class BackgroundSize : SpecifiedValue<ComputedBackgroundSize>, ToCss {

    class Explicit(val width: NonNegativeLengthOrPercentageOrAuto, val height: NonNegativeLengthOrPercentageOrAuto) : BackgroundSize()

    object Cover : BackgroundSize()

    object Contain : BackgroundSize()

    override fun toComputedValue(context: Context): ComputedBackgroundSize {
        return when (this) {
            is BackgroundSize.Explicit -> ComputedBackgroundSize.Explicit(
                width.toComputedValue(context),
                height.toComputedValue(context)
            )
            is BackgroundSize.Cover -> ComputedBackgroundSize.Cover
            is BackgroundSize.Contain -> ComputedBackgroundSize.Contain
        }
    }

    override fun toCss(writer: Writer) {
        TODO()
    }

    companion object : Parse<BackgroundSize> {
        override fun parse(context: ParserContext, input: Parser): Result<BackgroundSize, ParseError> {
            val widthResult = input.tryParse { parser -> NonNegativeLengthOrPercentageOrAuto.parse(context, parser) }

            if (widthResult is Ok) {
                val width = widthResult.value
                val height = input.tryParse { parser -> NonNegativeLengthOrPercentageOrAuto.parse(context, parser) }
                    .unwrapOr(width)

                return Ok(Explicit(width, height))
            }

            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return Ok(
                when (ident.toLowerCase()) {
                    "cover" -> Cover
                    "contain" -> Contain
                    else -> return Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
                }
            )
        }

        fun auto(): BackgroundSize {
            return BackgroundSize.Explicit(
                NonNegativeLengthOrPercentageOrAuto.auto(),
                NonNegativeLengthOrPercentageOrAuto.auto()
            )
        }
    }
}

sealed class BackgroundRepeat : SpecifiedValue<ComputedBackgroundRepeat>, ToCss {

    object RepeatX : BackgroundRepeat()

    object RepeatY : BackgroundRepeat()

    class Keywords(val horizontal: BackgroundRepeatKeyword, val vertical: Option<BackgroundRepeatKeyword>) : BackgroundRepeat()

    override fun toComputedValue(context: Context): ComputedBackgroundRepeat {
        return when (this) {
            is BackgroundRepeat.RepeatX -> {
                ComputedBackgroundRepeat(
                    BackgroundRepeatKeyword.Repeat,
                    BackgroundRepeatKeyword.NoRepeat
                )
            }
            is BackgroundRepeat.RepeatY -> {
                ComputedBackgroundRepeat(
                    BackgroundRepeatKeyword.NoRepeat,
                    BackgroundRepeatKeyword.Repeat
                )
            }
            is BackgroundRepeat.Keywords -> {
                ComputedBackgroundRepeat(
                    horizontal,
                    vertical.unwrapOr(horizontal)
                )
            }
        }
    }

    override fun toCss(writer: Writer) {
        TODO()
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BackgroundRepeat, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            when (identifier.toLowerCase()) {
                "repeat-x" -> return Ok(RepeatX)
                "repeat-y" -> return Ok(RepeatY)
                else -> {
                }
            }

            val horizontalResult = BackgroundRepeatKeyword.fromIdent(identifier)
            val horizontal = when (horizontalResult) {
                is Ok -> horizontalResult.value
                is Err -> return Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }

            val vertical = input.tryParse(BackgroundRepeatKeyword.Companion::parse).ok()

            return Ok(Keywords(horizontal, vertical))
        }
    }
}

enum class BackgroundRepeatKeyword {

    Repeat,

    Space,

    Round,

    NoRepeat;

    companion object {
        fun fromIdent(keyword: String): Result<BackgroundRepeatKeyword, Empty> {
            return when (keyword.toLowerCase()) {
                "repeat" -> Ok(Repeat)
                "space" -> Ok(Space)
                "round" -> Ok(Round)
                "no-repeat" -> Ok(NoRepeat)
                else -> Err()
            }
        }

        fun parse(input: Parser): Result<BackgroundRepeatKeyword, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "repeat" -> Ok(Repeat)
                "space" -> Ok(Space)
                "round" -> Ok(Round)
                "no-repeat" -> Ok(NoRepeat)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
