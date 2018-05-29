package de.krall.flare.cssparser

import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

fun parseNth(input: Parser): Result<Nth, ParseError> {
    val location = input.sourceLocation()
    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> return tokenResult
    }

    return when (token) {
        is Token.Number -> {
            Ok(Nth(0, token.number.int()))
        }
        is Token.Dimension -> {
            val unit = token.unit.toLowerCase()

            when (unit) {
                "n" -> parseB(input, token.number.int())
                "n-" -> parseSignlessB(input, token.number.int(), -1)
                else -> {
                    val digitsResult = parseDashDigits(unit)

                    when (digitsResult) {
                        is Ok -> Ok(Nth(token.number.int(), digitsResult.value))
                        is Err -> Err(location.newUnexpectedTokenError(token))
                    }
                }
            }
        }
        is Token.Identifier -> {
            val text = token.name.toLowerCase()

            when (text) {
                "even" -> Ok(Nth(2, 0))
                "odd" -> Ok(Nth(2, 1))
                "n" -> parseB(input, 1)
                "-n" -> parseB(input, -1)
                "n-" -> parseSignlessB(input, 1, -1)
                "-n-" -> parseSignlessB(input, -1, -1)
                else -> {
                    val (digitsResult, a) = if (text.startsWith("-")) {
                        Pair(parseDashDigits(text.substring(1)), -1)
                    } else {
                        Pair(parseDashDigits(text), 1)
                    }

                    when (digitsResult) {
                        is Ok -> Ok(Nth(a, digitsResult.value))
                        is Err -> Err(location.newUnexpectedTokenError(token))
                    }
                }
            }
        }
        is Token.Plus -> {
            val afterPlusLocation = input.sourceLocation()
            val innerTokenResult = input.next()

            val innerToken = when (innerTokenResult) {
                is Ok -> innerTokenResult.value
                is Err -> return innerTokenResult
            }

            when (innerToken) {
                is Token.Identifier -> {
                    val text = innerToken.name.toLowerCase()

                    when (text) {
                        "n" -> parseB(input, 1)
                        "n-" -> parseSignlessB(input, 1, -1)
                        else -> {
                            val digitsResult = parseDashDigits(text)

                            when (digitsResult) {
                                is Ok -> Ok(Nth(1, digitsResult.value))
                                is Err -> Err(afterPlusLocation.newUnexpectedTokenError(innerToken))
                            }
                        }
                    }
                }
                else -> {
                    Err(afterPlusLocation.newUnexpectedTokenError(innerToken))
                }
            }
        }
        else -> {
            Err(location.newUnexpectedTokenError(token))
        }
    }
}

private fun parseB(input: Parser, a: Int): Result<Nth, ParseError> {
    val state = input.state()
    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> return tokenResult
    }

    return when (token) {
        is Token.Plus -> parseSignlessB(input, a, 1)
        is Token.Minus -> parseSignlessB(input, a, -1)
        is Token.Number -> {
            if (token.number.negative) {
                Ok(Nth(a, token.number.int()))
            } else {
                input.reset(state)
                Ok(Nth(a, 0))
            }
        }
        else -> {
            input.reset(state)
            Ok(Nth(a, 0))
        }
    }
}

private fun parseSignlessB(input: Parser, a: Int, sign: Int): Result<Nth, ParseError> {
    val location = input.sourceLocation()
    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> return tokenResult
    }

    return when (token) {
        is Token.Number -> {
            if (token.number.negative) {
                Ok(Nth(a, sign * token.number.int()))
            } else {
                Err(location.newUnexpectedTokenError(token))
            }
        }
        else -> {
            Err(location.newUnexpectedTokenError(token))
        }
    }
}

private fun parseDashDigits(text: String): Result<Int, Empty> {
    return if (text.length >= 3 && text.startsWith("n-") && isNumeric(text.substring(2))) {
        parseNumberSaturate(text.substring(1))
    } else {
        Err()
    }
}

private fun parseNumberSaturate(number: String): Result<Int, Empty> {
    return Ok(number.toInt())
}

private fun isNumeric(text: String): Boolean {
    for (c in text.toCharArray()) {
        if (c < '0' || c > '9') {
            return false
        }
    }
    return true
}

data class Nth(val a: Int, val b: Int)