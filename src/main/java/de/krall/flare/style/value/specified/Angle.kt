package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Err
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext

class Angle {

    companion object {

        fun parseAllowingUnitless(context: ParserContext, input: Parser): Result<Angle, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }
    }
}