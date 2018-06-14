package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext

class Position {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<Position, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }

        fun center():Position {
            return Position()
        }
    }
}

enum class X {

    Left,

    Right;

    companion object {
        fun parse(input: Parser): Result<X, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "left" -> Ok(X.Left)
                "right" -> Ok(X.Right)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}

enum class Y {

    Top,

    Bottom;

    companion object {
        fun parse(input: Parser): Result<Y, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "top" -> Ok(Y.Top)
                "bottom" -> Ok(Y.Bottom)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}