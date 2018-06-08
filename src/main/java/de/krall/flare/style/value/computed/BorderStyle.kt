package de.krall.flare.style.value.computed

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result


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
                "none" -> Ok(Style.NONE)
                "hidden" -> Ok(Style.HIDDEN)
                "dotted" -> Ok(Style.DOTTED)
                "dashed" -> Ok(Style.DASHED)
                "solid" -> Ok(Style.SOLID)
                "double" -> Ok(Style.DOUBLE)
                "groove" -> Ok(Style.GROOVE)
                "ridge" -> Ok(Style.RIDGE)
                "inset" -> Ok(Style.INSET)
                "outset" -> Ok(Style.OUTSET)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}