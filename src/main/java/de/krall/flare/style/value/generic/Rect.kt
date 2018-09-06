package de.krall.flare.style.value.generic

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import modern.std.Err
import modern.std.Ok
import modern.std.Result

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