package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.*
import de.krall.flare.style.parser.AllowQuirks
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength
import de.krall.flare.style.value.computed.PixelLength
import de.krall.flare.style.value.computed.intoNonNegative
import de.krall.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius


sealed class BorderSideWidth : SpecifiedValue<ComputedNonNegativeLength> {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BorderSideWidth, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
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
                "thin" -> Ok(BorderSideWidth.Thin())
                "medium" -> Ok(BorderSideWidth.Medium())
                "thick" -> Ok(BorderSideWidth.Thick())
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }

    class Thin : BorderSideWidth() {
        override fun toComputedValue(context: Context): ComputedNonNegativeLength {
            return PixelLength(1f).intoNonNegative()
        }
    }

    class Medium : BorderSideWidth() {
        override fun toComputedValue(context: Context): ComputedNonNegativeLength {
            return PixelLength(1f).intoNonNegative()
        }
    }

    class Thick : BorderSideWidth() {
        override fun toComputedValue(context: Context): ComputedNonNegativeLength {
            return PixelLength(1f).intoNonNegative()
        }
    }

    class Length(val length: de.krall.flare.style.value.specified.NonNegativeLength) : BorderSideWidth() {
        override fun toComputedValue(context: Context): ComputedNonNegativeLength {
            return length.toComputedValue(context)
        }
    }
}

class BorderCornerRadius(val width: LengthOrPercentage,
                         val height: LengthOrPercentage) : SpecifiedValue<ComputedBorderCornerRadius> {

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

    override fun toComputedValue(context: Context): ComputedBorderCornerRadius {
        return ComputedBorderCornerRadius(
                width.toComputedValue(context),
                height.toComputedValue(context)
        )
    }
}
