/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.ClampingMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue

data class Number(
    val value: Float,
    val clampingMode: ClampingMode?
) : SpecifiedValue<Float> {

    fun wasCalc(): Boolean = clampingMode != null
    override fun toComputedValue(context: Context): Float = value

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Number, ParseError> {
            return parseWithClampingMode(context, input, ClampingMode.All)
        }

        fun parseWithClampingMode(context: ParserContext, input: Parser, clampingMode: ClampingMode): Result<Number, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Number -> {
                    if (clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        Ok(Number(token.number.float(), null))
                    } else {
                        Err(location.newUnexpectedTokenError(token))
                    }
                }
                is Token.Function -> {
                    if (token.name.equals("calc", ignoreCase = true)) {
                        val number = when (val result = CalcNode.parseNumber(context, input)) {
                            is Ok -> result.value
                            is Err -> return result
                        }

                        Ok(Number(number, clampingMode))
                    } else {
                        Err(location.newUnexpectedTokenError(token))
                    }
                }
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }
    }
}