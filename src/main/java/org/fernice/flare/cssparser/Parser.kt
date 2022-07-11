/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Result
import fernice.std.unwrap

/**
 * A CSS assistive, partial [Parser], that provides common parse functions and keeps track of nested blocks.
 */
class Parser private constructor(
    private val tokenizer: Tokenizer,
    private var blockType: BlockType?,
    private val delimiters: Int,
) {

    companion object {

        fun new(input: ParserInput): Parser {
            return Parser(
                Tokenizer.new(input.text),
                blockType = null,
                delimiters = 0,
            )
        }
    }

    /**
     * Returns the current [ParserState] of this [Parser]. Besides the information that the state provides, the state can be
     * used to reset this parser to an earlier state.
     */
    fun state(): ParserState {
        return ParserState(tokenizer.state(), blockType)
    }

    /**
     * Resets this [Parser] to an earlier [ParserState].
     *
     * The reset performs in O(1).
     */
    fun reset(state: ParserState) {
        tokenizer.reset(state.state)
        blockType = state.blockType
    }

    /**
     * Returns the current [SourcePosition] of this Parser. The source position is the index in the char stream starting at 0.
     * The position should be used for slicing the input in order to provide more information when reporting errors.
     */
    fun sourcePosition(): SourcePosition {
        return tokenizer.position()
    }

    /**
     * Returns the current [SourceLocation] of this Parser. The source location is the line and the position within that line
     * both starting at zero. The location should be used for error reporting, as it represents human readable position.
     */
    fun sourceLocation(): SourceLocation {
        return tokenizer.location()
    }

    /**
     * Slices the input onwards from the specified [position] to the current [SourcePosition].
     */
    fun sliceFrom(position: SourcePosition): String {
        return tokenizer.sliceFrom(position)
    }

    /**
     * Slices the input from the specified [start] to the specified [end].
     */
    fun slice(start: SourcePosition, end: SourcePosition): String {
        return tokenizer.slice(start, end)
    }

    /**
     * Returns the next token excluding any whitespace or comment token. Returns [Err] if the Parser is exhausted. See
     * [isExhausted] for conditions of exhaustion.
     *
     * @see nextIncludingWhitespace for whitespaces
     * @see nextIncludingWhitespaceAndComment for whitespaces and comments
     */
    fun next(): Result<Token, ParseError> {
        skipWhitespace()
        return nextIncludingWhitespace()
    }

    /**
     * Returns the next token excluding any comment token. Returns [Err] if the Parser is exhausted. See [isExhausted] for
     * conditions of exhaustion.
     *
     * @see next for no whitespaces
     * @see nextIncludingWhitespaceAndComment for comments
     */
    fun nextIncludingWhitespace(): Result<Token, ParseError> {
        while (true) {
            when (val result = nextIncludingWhitespaceAndComment()) {
                is Err -> return result
                is Ok -> {
                    if (result.value !is Token.Comment) {
                        return result
                    }
                }
            }
        }
    }

    /**
     * Returns the next token. Returns [Err] if the Parser is exhausted. See [isExhausted] for conditions of exhaustion.
     *
     * @see next for no whitespaces and comments
     * @see nextIncludingWhitespace for no comments
     */
    fun nextIncludingWhitespaceAndComment(): Result<Token, ParseError> {
        takeBlockType()?.let { blockType ->
            tokenizer.consumeUntilEndOfBlock(blockType)
        }

        val state = state()

        val token = when (val token = tokenizer.nextToken()) {
            is Err -> {
                reset(state)
                return Err(newError(ParseErrorKind.EndOfFile))
            }
            is Ok -> token.value
        }

        if (delimiters and Delimiters.from(token).bits != 0) {
            reset(state)
            return Err(newError(ParseErrorKind.EndOfFile))
        }

        blockType = BlockType.opening(token)

        return Ok(token)
    }

    /**
     * Skips all whitespace and comment tokens.
     */
    fun skipWhitespace() {
        while (true) {
            val state = state()

            when (val result = nextIncludingWhitespace()) {
                is Err -> {
                    reset(state)
                    return
                }
                is Ok -> {
                    if (result.value !is Token.Whitespace) {
                        reset(state)
                        return
                    }
                }
            }
        }
    }

    /**
     * Returns whether the Parser is exhausted, that is when the Parser reaches the end of the file or a token that the Parser
     * is delimited by. If the Parser encounters an error during testing i.a. a illegal token this method will return false.
     */
    fun isExhausted(): Boolean {
        return expectExhausted() is Ok
    }

    /**
     * Returns [Ok] if the Parser is exhausted, otherwise [Err]. The Parser is exhausted if end of file or a delimiter has been
     * reached. If the Parser is exhausted [next], [nextIncludingWhitespace] and [nextIncludingWhitespaceAndComment] all return
     * [Err].
     */
    fun expectExhausted(): Result<Unit, ParseError> {
        val state = state()

        val tokenResult = next()

        val result = when (tokenResult) {
            is Err -> {
                if (tokenResult.value.kind is ParseErrorKind.EndOfFile) {
                    Ok()
                } else {
                    tokenResult
                }
            }
            is Ok -> {
                Err(state.location().newUnexpectedTokenError(tokenResult.value))
            }
        }

        reset(state)

        return result
    }

    /**
     * Expects the next token to be a [Token.Identifier] that matches the [text]. Returns [Ok] if the token matches, otherwise
     * returns [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectIdentifierMatching(text: String): Result<Unit, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Identifier -> {
                if (token.name.equals(text, true)) {
                    Ok()
                } else {
                    Err(location.newUnexpectedTokenError(token))
                }
            }
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Identifier]. Returns [Ok] bearing the identifier if the token matches, otherwise
     * returns [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectIdentifier(): Result<String, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Identifier -> Ok(token.name)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Url]. Returns [Ok] bearing the url if the token matches, otherwise returns [Err] of
     * type [ParseErrorKind.UnexpectedToken].
     */
    fun expectUrl(): Result<String, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Url -> Ok(token.url)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Function]. Returns [Ok] bearing the function name if the token matches, otherwise
     * returns [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectFunction(): Result<String, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Function -> Ok(token.name)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }


    /**
     * Expects the next token to be [Token.String]. Returns [Ok] bearing the string if the token matches, otherwise returns
     * [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectString(): Result<String, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.String -> Ok(token.value)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }


    /**
     * Expects the next token to be [Token.Identifier] or [Token.String]. Returns [Ok] bearing the identifier or string if the
     * token matches, otherwise returns [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectIdentifierOrString(): Result<String, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Identifier -> Ok(token.name)
            is Token.String -> Ok(token.value)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Number]. Returns [Ok] bearing the value if the token matches, otherwise returns [Err]
     * of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectNumber(): Result<Float, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Number -> Ok(token.number.float())
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Percentage]. Returns [Ok] bearing the value if the token matches, otherwise returns
     * [Err] of type [ParseErrorKind.UnexpectedToken].
     */
    fun expectPercentage(): Result<Float, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Percentage -> Ok(token.number.float())
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Comma]. Returns [Ok] if the token matches, otherwise returns [Err] of type
     * [ParseErrorKind.UnexpectedToken].
     */
    fun expectComma(): Result<Unit, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Comma -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Solidus]. Returns [Ok] if the token matches, otherwise returns [Err] of type
     * [ParseErrorKind.UnexpectedToken].
     */
    fun expectSolidus(): Result<Unit, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Solidus -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Colon]. Returns [Ok] if the token matches, otherwise returns [Err] of type
     * [ParseErrorKind.UnexpectedToken].
     */
    fun expectColon(): Result<Unit, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Colon -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Bang]. Returns [Ok] if the token matches, otherwise returns [Err] of type
     * [ParseErrorKind.UnexpectedToken].
     */
    fun expectBang(): Result<Unit, ParseError> {
        val location = sourceLocation()

        val token = when (val token = next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Bang -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Creates a [ParseError] for the current [SourceLocation] with the specified [kind].
     */
    fun newError(kind: ParseErrorKind): ParseError {
        return ParseError(kind, sourceLocation())
    }

    /**
     * Creates a [ParseError] bearing [ParseErrorKind.UnexpectedToken] for the current [SourceLocation] with the specified
     * [token] as unexpected token.
     */
    fun newUnexpectedTokenError(token: Token): ParseError {
        return ParseError(ParseErrorKind.UnexpectedToken(token), sourceLocation())
    }

    /**
     * Tries to parse using the specified [parse] function. If the function return [Err], resets the state to the original one
     * before parsing.
     */
    inline fun <T> tryParse(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val state = state()

        val result = parse(this)

        if (result is Err) {
            reset(state)
        }

        return result
    }

    /**
     * Parses using the specified [parse] function and expects the Parser to be exhausted afterwards. Returns [Err] with
     * [ParseErrorKind.Unexhausted] as error kind if the parse was not exhaustive. Otherwise returns the [Result] of the parse.
     */
    inline fun <T> parseEntirely(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val result = parse(this)

        return when {
            result is Err -> result
            !isExhausted() -> Err(newError(ParseErrorKind.Unexhausted))
            else -> result
        }
    }

    /**
     * Parses a list of sequences each separated by comma until the parser is exhausted or one of the parse functions returns
     * [Err]. For each sequence [parse] is called and expected to be exhaustive. Returns [Ok] bearing the list of parse
     * results, otherwise [Err].
     */
    fun <T> parseCommaSeparated(parse: (Parser) -> Result<T, ParseError>): Result<List<T>, ParseError> {
        val values = mutableListOf<T>()

        while (true) {
            when (val value = parseUntilBefore(Delimiters.Comma, parse)) {
                is Ok -> values.add(value.value)
                is Err -> return value
            }

            when (val token = next()) {
                is Ok -> {
                    if (token.value !is Token.Comma) {
                        throw IllegalStateException("unreachable")
                    }
                }
                is Err -> return Ok(values)
            }
        }
    }

    /**
     * Creates a nested Parser that parses until before the specified [delimiters] and advances this Parser equally. The [parse]
     * function is expected to be exhaustive. The nested Parser is delimited by both the specified delimiters and the [Delimiters]
     * imposed on this Parser.
     */
    fun <T> parseUntilBefore(delimiters: Delimiters, parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val delimitedDelimiters = this.delimiters or delimiters.bits

        val delimitedParser = Parser(tokenizer.clone(), takeBlockType(), delimitedDelimiters)

        val result = delimitedParser.parseEntirely(parse)

        delimitedParser.blockType?.let { blockType ->
            delimitedParser.tokenizer.consumeUntilEndOfBlock(blockType)
        }

        tokenizer.consumeUntilBefore(delimitedDelimiters)

        return result
    }

    /**
     * Creates a nested Parser that parses until after the specified [delimiters] and advances this Parser equally. The [parse]
     * function is expected to be exhaustive. The nested Parser is delimited by both the specified delimiters and the [Delimiters]
     * imposed on this Parser.
     */
    fun <T> parseUntilAfter(delimiters: Delimiters, parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val result = parseUntilBefore(delimiters, parse)

        val token = tokenizer.peekToken(1)

        if (token is Ok && this.delimiters and Delimiters.from(token.unwrap()).bits != 0) {
            // make sure to take the next token not some token
            tokenizer.nextToken()

            BlockType.opening(token.unwrap())?.let { blockType ->
                tokenizer.consumeUntilEndOfBlock(blockType)
            }
        }

        return result
    }

    /**
     * Creates a nested Parser that parses a nested block. The current token must be a block opening token in order to create a
     * nested block Parse upon. Advances the Parser the method was call on until the end of the block, treating it as a single
     * token. The [parse] function is expected to be exhaustive. The nested Parser is the [Delimiters] imposed on this Parser
     * and those derived from the block opening token.
     */
    fun <T> parseNestedBlock(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val blockType = takeBlockType() ?: error("not a nested block")

        val closingDelimiter = when (blockType) {
            is BlockType.Brace -> Delimiters.RightBrace
            is BlockType.Parenthesis -> Delimiters.RightParenthesis
            is BlockType.Bracket -> Delimiters.RightBracket
        }

        val nestedParser = Parser(tokenizer.clone(), blockType = null, closingDelimiter.bits)

        val result = nestedParser.parseEntirely(parse)

        nestedParser.blockType?.let { nestedBlockType ->
            nestedParser.tokenizer.consumeUntilEndOfBlock(nestedBlockType)
        }

        tokenizer.consumeUntilEndOfBlock(blockType)

        return result
    }

    /**
     * "Atomically" takes the current [BlockType], if present, and replaces it with [None].
     */
    private fun takeBlockType(): BlockType? {
        val blockType = this.blockType
        this.blockType = null
        return blockType
    }

    override fun toString(): String {
        return "Parser($tokenizer)"
    }
}

/**
 * Creates a next [ParseError] at this [SourceLocation] with the specified [kind].
 */
fun SourceLocation.newError(kind: ParseErrorKind): ParseError {
    return ParseError(kind, this)
}

/**
 * Creates a next [ParseError] bearing [ParseErrorKind.UnexpectedToken] at this [SourceLocation] with the specified
 * [token] as unexpected token.
 */
fun SourceLocation.newUnexpectedTokenError(token: Token): ParseError {
    return ParseError(ParseErrorKind.UnexpectedToken(token), this)
}

/**
 * The input for the [Parser] used both for parsing and slicing.
 */
data class ParserInput(internal val text: String)

/**
 * A single state of a [Parser], including all essential information of that very Parser's state. This should be used
 * for error reporting and state reverting.
 */
data class ParserState(internal val state: State, internal val blockType: BlockType?) {

    /**
     * Returns the current [SourcePosition] of this [ParserState]. The source position is the index in the char stream starting at 0.
     * The position should be used for slicing the input in order to provide more information when reporting errors.
     */
    fun position(): SourcePosition {
        return state.sourcePosition
    }

    /**
     * Returns the current [SourceLocation] of this [ParserState]. The source location is the line and the position within that line
     * both starting at zero. The location should be used for error reporting, as it represents human readable position.
     */
    fun location(): SourceLocation {
        return state.sourceLocation
    }
}

/**
 * Represents an error that occurred during parsing. The error is specified by a [ParseErrorKind] and a [SourceLocation]
 * at which the error occurred.
 */
data class ParseError(val kind: ParseErrorKind, val location: SourceLocation) {

    override fun toString(): String {
        return "$kind @ $location"
    }
}

/**
 * Base class for parse error kind providing basic and common error kinds.
 */
abstract class ParseErrorKind {

    override fun toString(): String {
        return "ParseErrorKind::${javaClass.simpleName}"
    }

    object EndOfFile : ParseErrorKind()

    object Unexhausted : ParseErrorKind()

    data class UnexpectedToken(val token: Token) : ParseErrorKind()

    object UnsupportedFeature : ParseErrorKind()

    object Unknown : ParseErrorKind()

    object Unspecified : ParseErrorKind()
}
