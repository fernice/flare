/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.value.ComputedValue
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ToCss
import java.io.Writer

data class BorderCornerRadius(
    val width: LengthOrPercentage,
    val height: LengthOrPercentage
) : ComputedValue {

    companion object {

        private val zero: BorderCornerRadius by lazy {
            BorderCornerRadius(
                LengthOrPercentage.zero(),
                LengthOrPercentage.zero()
            )
        }

        fun zero(): BorderCornerRadius {
            return zero
        }
    }
}

sealed class BorderStyle : ToCss {

    object None : BorderStyle()
    object Hidden : BorderStyle()
    object Dotted : BorderStyle()
    object Dashed : BorderStyle()
    object Solid : BorderStyle()
    object Double : BorderStyle()
    object Groove : BorderStyle()
    object Ride : BorderStyle()
    object Inset : BorderStyle()
    object Outset : BorderStyle()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is BorderStyle.None -> "none"
                is BorderStyle.Hidden -> "hidden"
                is BorderStyle.Dotted -> "dotted"
                is BorderStyle.Dashed -> "dashed"
                is BorderStyle.Solid -> "solid"
                is BorderStyle.Double -> "double"
                is BorderStyle.Groove -> "groove"
                is BorderStyle.Ride -> "ride"
                is BorderStyle.Inset -> "inset"
                is BorderStyle.Outset -> "outset"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<BorderStyle, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.lowercase()) {
                "none" -> Ok(None)
                "hidden" -> Ok(Hidden)
                "dotted" -> Ok(Dotted)
                "dashed" -> Ok(Dashed)
                "solid" -> Ok(Solid)
                "double" -> Ok(Double)
                "groove" -> Ok(Groove)
                "ridge" -> Ok(Ride)
                "inset" -> Ok(Inset)
                "outset" -> Ok(Outset)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
