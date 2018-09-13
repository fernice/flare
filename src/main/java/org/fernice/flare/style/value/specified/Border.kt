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
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.flare.style.value.computed.intoNonNegative
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import fernice.std.unwrapOrElse
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

sealed class BorderSideWidth : SpecifiedValue<ComputedNonNegativeLength> {

    object Thin : BorderSideWidth()

    object Medium : BorderSideWidth()

    object Thick : BorderSideWidth()

    data class Length(val length: org.fernice.flare.style.value.specified.NonNegativeLength) : BorderSideWidth()

    final override fun toComputedValue(context: Context): org.fernice.flare.style.value.computed.NonNegativeLength {
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
                return Ok(Length(length.value))
            }

            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
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
