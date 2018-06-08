package de.krall.flare.style.value.computed

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result
import de.krall.flare.style.value.ComputedValue

class BorderCornerRadius(val width: LengthOrPercentage,
                         val height: LengthOrPercentage) : ComputedValue {

    companion object {

        private val zero: BorderCornerRadius by lazy {
            BorderCornerRadius(
                    LengthOrPercentage.zero(),
                    LengthOrPercentage.zero()
            )
        }

        fun zero(): BorderCornerRadius {
            return zero
        }
    }
}

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
                "none" -> Ok(NONE)
                "hidden" -> Ok(HIDDEN)
                "dotted" -> Ok(DOTTED)
                "dashed" -> Ok(DASHED)
                "solid" -> Ok(SOLID)
                "double" -> Ok(DOUBLE)
                "groove" -> Ok(GROOVE)
                "ridge" -> Ok(RIDGE)
                "inset" -> Ok(INSET)
                "outset" -> Ok(OUTSET)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}