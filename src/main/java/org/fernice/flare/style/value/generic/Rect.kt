/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.generic

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result

class Rect<T>(val top: T,
              val right: T,
              val bottom: T,
              val left: T) {

    companion object {

        fun <T> parseWith(context: ParserContext,
                          input: Parser,
                          parser: (ParserContext, Parser) -> Result<T, ParseError>): Result<Rect<T>, ParseError> {
            val firstResult = parser(context, input)

            val first = when (firstResult) {
                is Ok -> firstResult.value
                is Err -> return firstResult
            }

            val secondResult = input.tryParse { parser(context, input) }

            val second = when (secondResult) {
                is Ok -> secondResult.value
                is Err -> return Ok(Rect(
                        first,
                        first,
                        first,
                        first
                ))
            }

            val thirdResult = input.tryParse { parser(context, input) }

            val third = when (thirdResult) {
                is Ok -> thirdResult.value
                is Err -> return Ok(Rect(
                        first,
                        second,
                        first,
                        second
                ))
            }

            val fourthResult = input.tryParse { parser(context, input) }

            val fourth = when (fourthResult) {
                is Ok -> fourthResult.value
                is Err -> return Ok(Rect(
                        first,
                        second,
                        third,
                        second
                ))
            }

            return Ok(Rect(
                    first,
                    second,
                    third,
                    fourth
            ))
        }
    }
}
