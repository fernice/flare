/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import org.fernice.std.*

/**
 * Marks a parser that is capable of parsing any kind of at-rule.
 */
interface AtRuleParser<P, R> {

    fun parseAtRulePrelude(name: String, input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    fun parseAtRuleBlock(start: ParserState, prelude: P, input: Parser): Result<R, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    fun parseAtRuleWithoutBlock(start: ParserState, prelude: P): Result<R, Unit> {
        return Err()
    }
}

/**
 * Marks a parser that is capable of parsing a qualified rule. A qualified may always consists out of a block
 * containing the property declarations and prelude that comes before the block naming the selectors that the
 * qualified rule should match to.
 */
interface QualifiedRuleParser<P, R> {

    /**
     * Parses the prelude in front of the opening braces of a qualified. Typically, this should be a list of selectors.
     * The input is limited to the scope of the prelude preceding the block.
     */
    fun parseQualifiedRulePrelude(input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    /**
     * Parses the content of the qualified rule block. Typically, this should be the property declarations or nested
     * at-rules. The input is limited to the scope of the block.
     */
    fun parseQualifiedRuleBlock(start: ParserState, prelude: P, input: Parser): Result<R, ParseError> {
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
    fun parseValue(name: String, input: Parser): Result<D, ParseError>
}

/**
 * Wraps a [ParseError] additionally providing the original input that raised the parse error. This should be used for
 * error reporting as it provides human readable information about the error.
 */
data class ParseErrorSlice(val error: ParseError, val slice: String)

interface RuleBodyItemParser<AtRulePrelude, QualifiedRulePrelude, R> :
    AtRuleParser<AtRulePrelude, R>,
    QualifiedRuleParser<QualifiedRulePrelude, R>,
    DeclarationParser<R> {

    fun shouldParseDeclarations(): Boolean

    fun shouldParseQualifiedRule(): Boolean
}

/**
 * Parser wrapper for parsing a list of declaration inside a declaration block. Takes of care of recognizing declarations
 * and at-rules as well as a limiting the parsable scope passed on to the utilized [parser].
 */
class RuleBodyParser<P, AtRulePrelude, QualifiedRulePrelude, R>(
    private val input: Parser,
    private val parser: P,
) where P : RuleBodyItemParser<AtRulePrelude, QualifiedRulePrelude, R> {

    /**
     * Tries to parse the next property declaration in the block. If an error occurs the parser tries recover by skipping the
     * declaration. Returns [None] if no more declarations are left to parse in the block.
     *
     * # Implementation Detail
     * Skips all preceding [Token.Whitespace], [Token.SemiColon] and [Token.Comment] prior to the declaration. Tries to parse
     * a property declaration if the next token is an [Token.Identifier], otherwise if the next token is an [Token.AtKeyword]
     * tries to parse an at-rule. If the parsing fails returns a [ParseErrorSlice] containing the [ParseError] that occurred
     * as well as a slice of input in which it occurred.
     */
    fun next(): Result<R, ParseErrorSlice>? {
        while (true) {
            val start = input.state()

            when (val token = input.nextIncludingWhitespaceAndComment().ok() ?: return null) {
                is Token.RBrace,
                is Token.Whitespace,
                is Token.SemiColon,
                is Token.Comment,
                -> continue

                is Token.AtKeyword -> {
                    return parseAtRuleBlock(start, token.name, input, parser)
                }

                // Kotlin's lack of guards makes this a clusterfuck
                else -> {
                    val shouldParseDeclarations = parser.shouldParseDeclarations()
                    val shouldParseQualified = parser.shouldParseQualifiedRule()

                    var result = if (token is Token.Identifier && shouldParseDeclarations) {
                        val errorBehavior = when {
                            shouldParseQualified -> ParseUntilErrorBehavior.Stop
                            else -> ParseUntilErrorBehavior.Consume
                        }
                        input.parseUntilAfter(Delimiters.SemiColon, errorBehavior) { nestedInput ->
                            nestedInput.expectColon().unwrap { return@parseUntilAfter it }
                            parser.parseValue(token.name, nestedInput)
                        }
                    } else {
                        null
                    }

                    if (result is Ok) return result

                    if (shouldParseQualified) {
                        input.reset(start)

                        val delimiters = when {
                            shouldParseDeclarations -> Delimiters.SemiColon or Delimiters.LeftBrace
                            else -> Delimiters.LeftBrace
                        }
                        parseQualifiedRule(start, input, parser, delimiters).ifOk { qualifiedRule ->
                            return Ok(qualifiedRule)
                        }
                    }

                    if (result == null) {
                        result = input.parseUntilAfter(Delimiters.SemiColon) { _ ->
                            Err(start.location().newUnexpectedTokenError(token))
                        }
                    }

                    return result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(start.position())) }
                }
            }
        }
    }
}

/**
 * Parser wrapper for parsing a list of rules inside a stylesheet. Takes of care of recognizing qualified-rules
 * and at-rules as well as a limiting the parsable scope passed on to the utilized [parser].
 */
class StylesheetParser<P, AtRulePrelude, QualifiedRulePrelude, R>(
    private val input: Parser,
    private val parser: P,
) where P : AtRuleParser<AtRulePrelude, R>,
        P : QualifiedRuleParser<QualifiedRulePrelude, R> {

    private var seenRule = false

    /**
     * Tries to parse the next rule in the stylesheet. If an error occurs the parser tries recover by skipping the
     * rule. Returns `null` if no more declarations are left to parse in the block.
     */
    fun next(): Result<R, ParseErrorSlice>? {
        while (true) {
            input.skipWhitespace()

            val start = input.state()

            val atKeyword = when (val token = input.next().unwrap { return null }) {
                is Token.AtKeyword -> token.name
                else -> {
                    input.reset(start)
                    null
                }
            }

            if (atKeyword != null) {
                val firstRule = !seenRule
                seenRule = true

                if (firstRule && atKeyword.equals("charset", true)) {
                    input.parseUntilAfter(Delimiters.SemiColon or Delimiters.LeftBrace) { Ok() }
                } else {
                    return parseAtRuleBlock(start, atKeyword, input, parser)
                }
            } else {
                seenRule = true

                val result = parseQualifiedRule(start, input, parser, Delimiters.LeftBrace)

                return result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(start.position())) }
            }
        }
    }
}

/**
 * Tries to parse an at-rule with the specified [name] using the [parser]. The [Token.AtKeyword] is expected to have
 * already been parsed.
 */
private fun <P, R> parseAtRuleBlock(
    start: ParserState,
    name: String,
    input: Parser,
    parser: AtRuleParser<P, R>,
): Result<R, ParseErrorSlice> {
    val delimiters = Delimiters.SemiColon or Delimiters.LeftBrace
    val prelude = input.parseUntilBefore(delimiters) { nestedInput -> parser.parseAtRulePrelude(name, nestedInput) }
    when (prelude) {
        is Ok -> {
            val result = when (val token = input.next()) {
                is Ok -> when (token.value) {
                    is Token.SemiColon -> parser.parseAtRuleWithoutBlock(start, prelude.value)
                        .mapErr { input.newUnexpectedTokenError(Token.SemiColon) }

                    is Token.LBrace -> input.parseNestedBlock { nestedInput -> parser.parseAtRuleBlock(start, prelude.value, nestedInput) }
                    else -> error("unreachable")
                }

                is Err -> parser.parseAtRuleWithoutBlock(start, prelude.value)
                    .mapErr { input.newError(ParseErrorKind.EndOfFile) }
            }
            return result.mapErr { ParseErrorSlice(it, input.sliceFrom(start.position())) }
        }

        is Err -> {
            val endPosition = input.sourcePosition()
            when (val token = input.next()) {
                is Ok -> when (token.value) {
                    is Token.SemiColon, is Token.LBrace -> {}
                    else -> error("unreachable")
                }

                is Err -> {}
            }

            return Err(ParseErrorSlice(prelude.value, input.slice(start.position(), endPosition)))
        }
    }
}

/**
 * Tries to parse a qualified using the [parser].
 */
private fun <P, R> parseQualifiedRule(
    start: ParserState,
    input: Parser,
    parser: QualifiedRuleParser<P, R>,
    delimiters: Delimiters,
): Result<R, ParseError> {
    val preludeResult = input.parseUntilBefore(delimiters) { nestedInput -> parser.parseQualifiedRulePrelude(nestedInput) }
    // consume the { } block
    input.expectBraceBlock().unwrap { return it }
    // before checking the result of the prelude
    val prelude = preludeResult.unwrap { return it }

    return input.parseNestedBlock { nestedInput -> parser.parseQualifiedRuleBlock(start, prelude, nestedInput) }
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


