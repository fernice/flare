package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.style.parser.AllowQuirks
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.PixelLength
import de.krall.flare.style.value.computed.intoNonNegative
import modern.std.Err
import modern.std.Ok
import modern.std.Result
import modern.std.unwrapOrElse
import de.krall.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

sealed class BorderSideWidth : SpecifiedValue<ComputedNonNegativeLength> {

    object Thin : BorderSideWidth()

    object Medium : BorderSideWidth()

    object Thick : BorderSideWidth()

    data class Length(val length: de.krall.flare.style.value.specified.NonNegativeLength) : BorderSideWidth()

    final override fun toComputedValue(context: Context): de.krall.flare.style.value.computed.NonNegativeLength {
        return when (this) {
            is BorderSideWidth.Thin -> {
                PixelLength(1f).intoNonNegative()
            }
            is BorderSideWidth.Medium -> {
                PixelLength(3f).intoNonNegative()
            }
            is BorderSideWidth.Thick -> {
                PixelLength(5f).intoNonNegative()
            }
            is BorderSideWidth.Length -> {
                length.toComputedValue(context)
            }
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BorderSideWidth, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<BorderSideWidth, ParseError> {
            val length = input.tryParse { NonNegativeLength.parseQuirky(context, input, allowQuirks) }

            if (length is Ok) {
                return Ok(BorderSideWidth.Length(length.value))
            }

            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "thin" -> Ok(BorderSideWidth.Thin)
                "medium" -> Ok(BorderSideWidth.Medium)
                "thick" -> Ok(BorderSideWidth.Thick)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}

data class BorderCornerRadius(
        val width: LengthOrPercentage,
        val height: LengthOrPercentage
) : SpecifiedValue<ComputedBorderCornerRadius> {

    override fun toComputedValue(context: Context): ComputedBorderCornerRadius {
        return ComputedBorderCornerRadius(
                width.toComputedValue(context),
                height.toComputedValue(context)
        )
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BorderCornerRadius, ParseError> {
            val widthResult = LengthOrPercentage.parse(context, input)

            val width = when (widthResult) {
                is Ok -> widthResult.value
                is Err -> return widthResult
            }

            val height = input.tryParse { input -> LengthOrPercentage.parse(context, input) }
                    .unwrapOrElse { _ -> width }

            return Ok(BorderCornerRadius(width, height))
        }
    }
}
