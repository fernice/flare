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

sealed class Style : ToCss {

    object None : Style()
    object Hidden : Style()
    object Dotted : Style()
    object Dashed : Style()
    object Solid : Style()
    object Double : Style()
    object Groove : Style()
    object Ride : Style()
    object Inset : Style()
    object Outset : Style()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Style.None -> "none"
                is Style.Hidden -> "hidden"
                is Style.Dotted -> "dotted"
                is Style.Dashed -> "dashed"
                is Style.Solid -> "solid"
                is Style.Double -> "double"
                is Style.Groove -> "groove"
                is Style.Ride -> "ride"
                is Style.Inset -> "inset"
                is Style.Outset -> "outset"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<Style, ParseError> {
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
