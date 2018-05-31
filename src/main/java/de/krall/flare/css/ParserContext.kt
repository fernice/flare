package de.krall.flare.css

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result

class ParserContext

interface Parse<T> {

    fun parse(context: ParserContext, input: Parser): Result<T, ParseError>
}