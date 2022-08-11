/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.std.unwrapOr
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.Parse
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import java.io.Writer
import org.fernice.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat
import org.fernice.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize

sealed class BackgroundSize : SpecifiedValue<ComputedBackgroundSize>, ToCss {

    class Explicit(val width: NonNegativeLengthOrPercentageOrAuto, val height: NonNegativeLengthOrPercentageOrAuto) : BackgroundSize()
    object Cover : BackgroundSize()
    object Contain : BackgroundSize()

    final override fun toComputedValue(context: Context): ComputedBackgroundSize {
        return when (this) {
            is BackgroundSize.Explicit -> ComputedBackgroundSize.Explicit(
                width.toComputedValue(context),
                height.toComputedValue(context)
            )
            is BackgroundSize.Cover -> ComputedBackgroundSize.Cover
            is BackgroundSize.Contain -> ComputedBackgroundSize.Contain
        }
    }

    final override fun toCss(writer: Writer) {
        when (this) {
            is BackgroundSize.Explicit -> {
                width.toCss(writer)
                writer.append(' ')
                height.toCss(writer)
            }
            is BackgroundSize.Cover -> writer.append("cover")
            is BackgroundSize.Contain -> writer.append("contain")
        }
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

            val ident = when (val ident = input.expectIdentifier()) {
                is Ok -> ident.value
                is Err -> return ident
            }

            return Ok(
                when (ident.lowercase()) {
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
    data class Keywords(val horizontal: BackgroundRepeatKeyword, val vertical: BackgroundRepeatKeyword) : BackgroundRepeat()

    final override fun toComputedValue(context: Context): ComputedBackgroundRepeat {
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
                    vertical
                )
            }
        }
    }

    final override fun toCss(writer: Writer) {
        when (this) {
            is BackgroundRepeat.RepeatX -> writer.append("repeat-x")
            is BackgroundRepeat.RepeatY -> writer.append("repeat-y")
            is BackgroundRepeat.Keywords -> {
                horizontal.toCss(writer)

                writer.append(' ')
                vertical.toCss(writer)
            }
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BackgroundRepeat, ParseError> {
            val location = input.sourceLocation()

            val identifier = when (val identifier = input.expectIdentifier()) {
                is Ok -> identifier.value
                is Err -> return identifier
            }

            when (identifier.lowercase()) {
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

            return Ok(Keywords(horizontal, vertical ?: horizontal))
        }

        val Repeat by lazy { BackgroundRepeat.Keywords(BackgroundRepeatKeyword.Repeat, BackgroundRepeatKeyword.Repeat) }
    }
}

sealed class BackgroundRepeatKeyword : ToCss {

    object Repeat : BackgroundRepeatKeyword()
    object Space : BackgroundRepeatKeyword()
    object Round : BackgroundRepeatKeyword()
    object NoRepeat : BackgroundRepeatKeyword()

    final override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is BackgroundRepeatKeyword.Repeat -> "repeat"
                is BackgroundRepeatKeyword.Space -> "space"
                is BackgroundRepeatKeyword.Round -> "round"
                is BackgroundRepeatKeyword.NoRepeat -> "no-repeat"
            }
        )
    }

    companion object {
        fun fromIdent(keyword: String): Result<BackgroundRepeatKeyword, Unit> {
            return when (keyword.lowercase()) {
                "repeat" -> Ok(Repeat)
                "space" -> Ok(Space)
                "round" -> Ok(Round)
                "no-repeat" -> Ok(NoRepeat)
                else -> Err()
            }
        }

        fun parse(input: Parser): Result<BackgroundRepeatKeyword, ParseError> {
            val location = input.sourceLocation()

            val identifier = when (val identifier = input.expectIdentifier()) {
                is Ok -> identifier.value
                is Err -> return identifier
            }

            return when (identifier.lowercase()) {
                "repeat" -> Ok(Repeat)
                "space" -> Ok(Space)
                "round" -> Ok(Round)
                "no-repeat" -> Ok(NoRepeat)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
