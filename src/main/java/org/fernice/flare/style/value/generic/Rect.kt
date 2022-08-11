/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.generic

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result

data class Rect<T>(
    val top: T,
    val right: T,
    val bottom: T,
    val left: T
) {

    companion object {

        fun <T> parseWith(
            context: ParserContext,
            input: Parser,
            parser: (ParserContext, Parser) -> Result<T, ParseError>
        ): Result<Rect<T>, ParseError> {
            val first = when (val first = parser(context, input)) {
                is Ok -> first.value
                is Err -> return first
            }

            val second = when (val second = input.tryParse { parser(context, input) }) {
                is Ok -> second.value
                is Err -> return Ok(
                    Rect(
                        first,
                        first,
                        first,
                        first
                    )
                )
            }

            val third = when (val third = input.tryParse { parser(context, input) }) {
                is Ok -> third.value
                is Err -> return Ok(
                    Rect(
                        first,
                        second,
                        first,
                        second
                    )
                )
            }

            val fourth = when (val fourth = input.tryParse { parser(context, input) }) {
                is Ok -> fourth.value
                is Err -> return Ok(
                    Rect(
                        first,
                        second,
                        third,
                        second
                    )
                )
            }

            return Ok(
                Rect(
                    first,
                    second,
                    third,
                    fourth
                )
            )
        }
    }
}
