package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Err
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext

class CssUrl {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<CssUrl, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }
    }
}

typealias ImageUrl = CssUrl
