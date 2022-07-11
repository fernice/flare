/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.value.specified

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.cssparser.Color as ParserColor
import org.fernice.flare.style.value.computed.Fill as ComputedFill


sealed class Fill : SpecifiedValue<ComputedFill> {

    object None : Fill()
    data class Color(val value: RGBA) : Fill()

    override fun toComputedValue(context: Context): ComputedFill {
        return when (this) {
            is Fill.None -> ComputedFill.None
            is Fill.Color -> ComputedFill.Color(value)
        }
    }

    companion object {

        fun parse(input: Parser): Result<Fill, ParseError> {
            when (val result = input.tryParse { parser -> ParserColor.parse(parser) }) {
                is Ok -> {
                    return when (val color = result.value) {
                        is ParserColor.RGBA -> Ok(Fill.Color(color.rgba))
                        is ParserColor.CurrentColor -> Err(input.newUnexpectedTokenError(Token.Identifier("currentcolor")))
                    }
                }
                else -> {}
            }

            val location = input.sourceLocation()
            val ident = when (val result = input.expectIdentifier()) {
                is Ok -> result.value
                is Err -> return result
            }

            return Ok(
                when (ident.lowercase()) {
                    "none" -> Fill.None
                    else -> return Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
                }
            )
        }
    }
}