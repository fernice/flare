/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result

/**
 * Parsed representation of the An+B notation.
 */
data class Nth(val a: Int, val b: Int)

/**
 * Parses the An+B notation used for nth child expressions. The notation was originally designed for a different css parser
 * than the one currently defined by the specification, making it slightly more complicated to parse.
 *
 * More information can be found [here](https://www.w3.org/TR/css-syntax-3/#the-anb-type).
 */
internal fun parseNth(input: Parser): Result<Nth, ParseError> {
    val location = input.sourceLocation()

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> return token
    }

    return when (token) {
        is Token.Number -> {
            Ok(Nth(0, token.number.int()))
        }
        is Token.Dimension -> {
            when (val unit = token.unit.lowercase()) {
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
            when (val text = token.name.toLowerCase()) {
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

            val innerToken = when (val innerTokenResult = input.next()) {
                is Ok -> innerTokenResult.value
                is Err -> return innerTokenResult
            }

            when (innerToken) {
                is Token.Identifier -> {
                    when (val text = innerToken.name.toLowerCase()) {
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

/**
 * Parses the B part of the the An+B notation optionally leading signs.
 */
private fun parseB(input: Parser, a: Int): Result<Nth, ParseError> {
    val state = input.state()

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> return token
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

/**
 * Parses the B part of the An+B notation without the leading sign.
 */
private fun parseSignlessB(input: Parser, a: Int, sign: Int): Result<Nth, ParseError> {
    val location = input.sourceLocation()

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> return token
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

/**
 * Tries to parse the B part of the An+B notation from a text. The text is expected to be at least three characters long
 * and start with 'n-' followed by a number. This case exists only because the parser will interpret this part as a text,
 * as it starts with two characters that start a string.
 */
private fun parseDashDigits(text: String): Result<Int, Unit> {
    return if (text.length >= 3 && text.startsWith("n-") && isNumeric(text.substring(2))) {
        parseNumberSaturate(text.substring(1))
    } else {
        Err()
    }
}

/**
 * Parses a text describing a number in to a integer.
 */
private fun parseNumberSaturate(number: String): Result<Int, Unit> {
    return Ok(number.toInt())
}

/**
 * Returns if the [text] only contains numeric characters.
 */
private fun isNumeric(text: String): Boolean {
    for (c in text.toCharArray()) {
        if (c < '0' || c > '9') {
            return false
        }
    }
    return true
}
