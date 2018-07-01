package de.krall.flare.cssparser

import de.krall.flare.std.*

interface AtRuleParser<P, R> {

    fun parseAtRulePrelude(input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
    }

    fun parseAtRule(input: Parser, prelude: P): Result<R, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
    }
}

interface QualifiedRuleParser<P, R> {

    fun parseQualifiedRulePrelude(input: Parser): Result<P, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
    }

    fun parseQualifiedRule(input: Parser, prelude: P): Result<R, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature()))
    }
}

interface DeclarationParser<D> {

    fun parseValue(input: Parser, name: String): Result<D, ParseError>
}

class ParseErrorSlice(val error: ParseError, val slice: String)

class DeclarationListParser<A, D, P>(private val input: Parser, private val parser: P) where P : AtRuleParser<A, D>, P : DeclarationParser<D> {

    fun next(): Option<Result<D, ParseErrorSlice>> {
        loop@
        while (true) {
            val state = input.state();
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return None()
            }

            when (token) {
                is Token.Whitespace,
                is Token.SemiColon,
                is Token.Comment -> continue@loop
                is Token.Identifier -> {
                    val result = input.parseUntilBefore(Delimiters.SemiColon) { input ->
                        val colon = input.expectColon()

                        colon as? Err ?: parser.parseValue(input, token.name)
                    }

                    return Some(result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) })
                }
                is Token.AtKeyword -> {
                    return Some(parseAtRule(input, parser, state, token.name))
                }
                else -> {
                    val result = input.parseUntilAfter(Delimiters.SemiColon, { Err(state.location().newUnexpectedTokenError(token)) })

                    return Some(result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) })
                }
            }
        }
    }
}

class RuleListParser<A, Q, R, P>(private val input: Parser,
                                 private val parser: P,
                                 private val stylesheet: Boolean)
        where P : AtRuleParser<A, R>, P : QualifiedRuleParser<Q, R> {

    private var firstRule = true

    fun next(): Option<Result<R, ParseErrorSlice>> {
        while (true) {
            val state = input.state()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return None()
            }

            val atKeyword: Option<String> = when (token) {
                is Token.AtKeyword -> Some(token.name)
                else -> {
                    input.reset(state)
                    None()
                }
            }

            if (atKeyword is Some) {
                val firstStylesheetRule = stylesheet && firstRule
                firstRule = false

                if (firstStylesheetRule && atKeyword.value.equals("charset", true)) {
                    input.parseUntilAfter(Delimiters.SemiColon, { Ok() })
                } else {
                    return Some(parseAtRule(input, parser, state, atKeyword.value))
                }
            } else {
                firstRule = false

                val result = parseQualifiedRule(input, parser)

                return Some(result.mapErr { e -> ParseErrorSlice(e, input.sliceFrom(state.position())) })
            }
        }
    }
}

private fun <P, R> parseAtRule(input: Parser,
                               parser: AtRuleParser<P, R>,
                               state: ParserState,
                               name: String): Result<R, ParseErrorSlice> {
    return Err(ParseErrorSlice(
            state.location().newUnexpectedTokenError(Token.AtKeyword(name)),
            input.sliceFrom(state.position())
    ))
}

private fun <P, R> parseQualifiedRule(input: Parser,
                                      parser: QualifiedRuleParser<P, R>): Result<R, ParseError> {
    val preludeResult = input.parseUntilBefore(Delimiters.LeftBrace, parser::parseQualifiedRulePrelude)

    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> return tokenResult
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

fun parseImportant(input: Parser): Result<Empty, ParseError> {
    val bangResult = input.expectBang()

    if (bangResult is Err) {
        return bangResult
    }

    return input.expectIdentifierMatching("important")
}