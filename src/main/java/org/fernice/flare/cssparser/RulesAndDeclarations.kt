/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result

/**
 * Marks a parser that is capable of parsing any kind of at-rule.
 */
interface AtRuleParser<P, R> {

    fun parseAtRulePrelude(input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    fun parseAtRule(input: Parser, prelude: P): Result<R, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }
}

/**
 * Marks a parser that is capable of parsing a qualified rule. A qualified may always consists out of a block
 * containing the property declarations and prelude that comes before the block naming the selectors that the
 * qualified rule should match to.
 */
interface QualifiedRuleParser<P, R> {

    /**
     * Parses the prelude in front of the opening braces of a qualified. Typically this should be a list of selectors.
     * The input is limited to the scope of the prelude preceding the block.
     */
    fun parseQualifiedRulePrelude(input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    /**
     * Parses the content of the qualified rule block. Typically this should be the property declarations or nested
     * at-rules. The input is limited to the scope of the block.
     */
    fun parseQualifiedRule(input: Parser, prelude: P): Result<R, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }
}

/**
 * Marks a parser that is capable of parsing a declaration.
 */
interface DeclarationParser<D> {

    /**
     * Parses a single declaration with the specified [name] from the [input]. The leading [Token.Identifier] of the
     * declaration has already been parsed.
     */
    fun parseValue(input: Parser, name: String): Result<D, ParseError>
}

/**
 * Wraps a [ParseError] additionally providing the original input that raised the parse error. This should be used for
 * error reporting as it provides human readable information about the error.
 */
data class ParseErrorSlice(val error: ParseError, val slice: String)

/**
 * Parser wrapper for parsing a list of declaration inside a declaration block. Takes of care of recognizing declarations
 * and at-rules as well as a limiting the parsable scope passed on to the utilized [parser].
 */
class DeclarationListParser<A, D, P>(
    private val input: Parser,
    private val parser: P,
) where P : AtRuleParser<A, D>, P : DeclarationParser<D> {

    /**
     * Tries to parse the next property declaration in the block. If an error occurs the parser tries recover by skipping the
     * declaration. Returns [None] if no more declarations are left to parse in the block.
     *
     * # Implementation Detail
     * Skips all preceding [Token.Whitespace], [Token.SemiColon] and [Token.Comment] prior to the declaration. Tries to parse
     * an property declaration if the next token is an [Token.Identifier], otherwise if the next token is an [Token.AtKeyword]
     * tries to parse an at-rule. If the parsing fails returns a [ParseErrorSlice] containing the [ParseError] that occurred
     * as well as a slice of input in which it occurred.
     */
    fun next(): Result<D, ParseErrorSlice>? {
        loop@
        while (true) {
            val state = input.state()

            val token = when (val token = input.nextIncludingWhitespaceAndComment()) {
                is Ok -> token.value
                is Err -> return null
            }

            when (token) {
                is Token.Whitespace,
                is Token.SemiColon,
                is Token.Comment,
                -> continue@loop
                is Token.Identifier -> {
                    val result = input.parseUntilBefore(Delimiters.SemiColon) { input ->
                        val colon = input.expectColon()

                        colon as? Err ?: parser.parseValue(input, token.name)
                    }

                    return result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) }
                }
                is Token.AtKeyword -> {
                    return parseAtRule(input, parser, state, token.name)
                }
                else -> {
                    val result = input.parseUntilAfter(Delimiters.SemiColon) { Err(state.location().newUnexpectedTokenError(token)) }

                    return result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) }
                }
            }
        }
    }
}

/**
 * Parser wrapper for parsing a list of rules inside a stylesheet. Takes of care of recognizing qualified-rules
 * and at-rules as well as a limiting the parsable scope passed on to the utilized [parser].
 */
class RuleListParser<A, Q, R, P>(
    private val input: Parser,
    private val parser: P,
    private val stylesheet: Boolean,
) where P : AtRuleParser<A, R>, P : QualifiedRuleParser<Q, R> {

    private var firstRule = true

    /**
     * Tries to parse the next rule in the stylesheet. If an error occurs the parser tries recover by skipping the
     * rule. Returns [None] if no more declarations are left to parse in the block.
     */
    fun next(): Result<R, ParseErrorSlice>? {
        while (true) {
            input.skipWhitespace()

            val state = input.state()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return null
            }

            val atKeyword = when (token) {
                is Token.AtKeyword -> token.name
                else -> {
                    input.reset(state)
                    null
                }
            }

            if (atKeyword != null) {
                val firstStylesheetRule = stylesheet && firstRule
                firstRule = false

                if (firstStylesheetRule && atKeyword.equals("charset", true)) {
                    input.parseUntilAfter(Delimiters.SemiColon) { Ok() }
                } else {
                    return parseAtRule(input, parser, state, atKeyword)
                }
            } else {
                firstRule = false

                val result = parseQualifiedRule(input, parser)

                return result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) }
            }
        }
    }
}

/**
 * Tries to parse an at-rule with the specified [name] using the [parser]. The [Token.AtKeyword] is expected to have
 * already been parsed.
 */
private fun <P, R> parseAtRule(
    input: Parser,
    @Suppress("UNUSED_PARAMETER") parser: AtRuleParser<P, R>,
    state: ParserState,
    name: String,
): Result<R, ParseErrorSlice> {
    return Err(
        ParseErrorSlice(
            state.location().newUnexpectedTokenError(Token.AtKeyword(name)),
            input.sliceFrom(state.position())
        )
    )
}

/**
 * Tries to parse an qualified using the [parser].
 */
private fun <P, R> parseQualifiedRule(
    input: Parser,
    parser: QualifiedRuleParser<P, R>,
): Result<R, ParseError> {
    val preludeResult = input.parseUntilBefore(Delimiters.LeftBrace, parser::parseQualifiedRulePrelude)

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> return token
    }

    when (token) {
        is Token.LBrace -> {
            val prelude = when (preludeResult) {
                is Ok -> preludeResult.value
                is Err -> return preludeResult
            }

            return input.parseNestedBlock { parser.parseQualifiedRule(it, prelude) }
        }
        else -> throw IllegalStateException("unreachable")
    }
}

/**
 * Tries to parse an '!important' from the [input].
 */
fun parseImportant(input: Parser): Result<Unit, ParseError> {
    val bangResult = input.expectBang()

    if (bangResult is Err) {
        return bangResult
    }

    return input.expectIdentifierMatching("important")
}
