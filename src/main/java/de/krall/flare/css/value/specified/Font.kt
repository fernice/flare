package de.krall.flare.css.value.specified

import de.krall.flare.css.parser.ParserContext
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Err
import de.krall.flare.std.Result

sealed class FontFamily {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontFamily, ParseError> {
            return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
        }
    }
}

sealed class FontSize {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontSize, ParseError> {
            return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
        }
    }

    class Length(val length: LengthOrPercentage) : FontSize()

    class Smaller : FontSize()

    class Larger : FontSize()
}