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
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result

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

enum class Style {

    NONE, HIDDEN, DOTTED, DASHED, SOLID, DOUBLE, GROOVE, RIDGE, INSET, OUTSET;

    companion object {

        fun parse(input: Parser): Result<Style, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "none" -> Ok(NONE)
                "hidden" -> Ok(HIDDEN)
                "dotted" -> Ok(DOTTED)
                "dashed" -> Ok(DASHED)
                "solid" -> Ok(SOLID)
                "double" -> Ok(DOUBLE)
                "groove" -> Ok(GROOVE)
                "ridge" -> Ok(RIDGE)
                "inset" -> Ok(INSET)
                "outset" -> Ok(OUTSET)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
