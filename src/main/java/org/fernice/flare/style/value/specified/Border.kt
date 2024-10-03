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
import org.fernice.flare.style.AllowQuirks
import org.fernice.flare.style.Parse
import org.fernice.flare.style.ParseQuirky
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.flare.style.value.computed.toNonNegative
import java.io.Writer
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

sealed class BorderSideWidth : SpecifiedValue<ComputedNonNegativeLength>, ToCss {

    object Thin : BorderSideWidth()
    object Medium : BorderSideWidth()
    object Thick : BorderSideWidth()
    data class Length(val length: org.fernice.flare.style.value.specified.NonNegativeLength) : BorderSideWidth()

    final override fun toComputedValue(context: Context): org.fernice.flare.style.value.computed.NonNegativeLength {
        return when (this) {
            is BorderSideWidth.Thin -> PixelLength(1f).toNonNegative()
            is BorderSideWidth.Medium -> PixelLength(3f).toNonNegative()
            is BorderSideWidth.Thick -> PixelLength(5f).toNonNegative()

            is BorderSideWidth.Length -> length.toComputedValue(context)
        }
    }

    override fun toCss(writer: Writer) {
        when (this) {
            is BorderSideWidth.Thin -> writer.append("thin")
            is BorderSideWidth.Medium -> writer.append("medium")
            is BorderSideWidth.Thick -> writer.append("thick")

            is BorderSideWidth.Length -> length.toCss(writer)
        }
    }

    companion object : Parse<BorderSideWidth>, ParseQuirky<BorderSideWidth> {
        override fun parse(context: ParserContext, input: Parser): Result<BorderSideWidth, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        override fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<BorderSideWidth, ParseError> {
            val length = input.tryParse { NonNegativeLength.parseQuirky(context, input, allowQuirks) }

            if (length is Ok) {
                return Ok(Length(length.value))
            }

            val location = input.sourceLocation()

            val ident = when (val ident = input.expectIdentifier()) {
                is Ok -> ident.value
                is Err -> return ident
            }

            return when (ident.lowercase()) {
                "thin" -> Ok(Thin)
                "medium" -> Ok(Medium)
                "thick" -> Ok(Thick)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}

data class BorderCornerRadius(
    val width: LengthOrPercentage,
    val height: LengthOrPercentage
) : SpecifiedValue<ComputedBorderCornerRadius>, ToCss {

    override fun toComputedValue(context: Context): ComputedBorderCornerRadius {
        return ComputedBorderCornerRadius(
            width.toComputedValue(context),
            height.toComputedValue(context)
        )
    }

    override fun toCss(writer: Writer) {
        width.toCss(writer)

        if (width != height) {
            writer.append(' ')
            height.toCss(writer)
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BorderCornerRadius, ParseError> {
            val width = when (val width = LengthOrPercentage.parse(context, input)) {
                is Ok -> width.value
                is Err -> return width
            }

            val height = input.tryParse { i -> LengthOrPercentage.parse(context, i) }
                .unwrapOr(width)

            return Ok(BorderCornerRadius(width, height))
        }
    }
}
