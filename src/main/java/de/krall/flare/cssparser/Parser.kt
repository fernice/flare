package de.krall.flare.cssparser

import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.std.Some
import de.krall.flare.std.let
import de.krall.flare.std.unwrap

/**
 * A CSS assistive, partial [Parser], that provides common parse functions and keeps track of nested blocks.
 */
class Parser private constructor(private val tokenizer: Tokenizer,
                                 private var blockType: Option<BlockType>,
                                 private val delimiters: Int) {

    constructor(parserInput: ParserInput) : this(Tokenizer(parserInput.text), None(), 0)

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
            val result = nextIncludingWhitespaceAndComment()

            when (result) {
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
        if (blockType is Some) {
            tokenizer.consumeUntilEndOfBlock(takeBlockType().unwrap())
        }

        val state = state()

        val tokenResult = tokenizer.nextToken()

        val token = when (tokenResult) {
            is Err -> {
                reset(state)
                return Err(newError(ParseErrorKind.EndOfFile()))
            }
            is Ok -> tokenResult.value
        }

        if (delimiters and Delimiters.from(token).bits != 0) {
            reset(state)
            return Err(newError(ParseErrorKind.EndOfFile()))
        }

        blockType = BlockType.opening(token)

        return Ok(token)
    }

    /**
     * Skips all whitespace tokens.
     */
    fun skipWhitespace() {
        while (true) {
            val state = state()
            val result = nextIncludingWhitespaceAndComment()

            when (result) {
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
     * Returns whether the Parser is exhausted.
     */
    fun isExhausted(): Boolean {
        return expectExhausted() is Ok
    }

    /**
     * Returns [Ok] if the Parser is exhausted, otherwise [Err]. The Parser is exhausted if end of file or a delimiter has been
     * reached. If the Parser is exhausted [next], [nextIncludingWhitespace] and [nextIncludingWhitespaceAndComment] all return
     * [Err]
     */
    fun expectExhausted(): Result<Empty, ParseError> {
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
     * Expects the next token to be a [Token.Identifier] that matches the [text]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectIdentifierMatching(text: String): Result<Empty, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
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
     * Expects the next token to be [Token.Identifier]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectIdentifier(): Result<String, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Identifier -> Ok(token.name)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    fun expectUrl(): Result<String, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Url -> Ok(token.url)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Function]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectFunction(): Result<String, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Function -> Ok(token.name)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }


    /**
     * Expects the next token to be [Token.String]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectString(): Result<String, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.String -> Ok(token.value)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }


    /**
     * Expects the next token to be [Token.Identifier] or [Token.String]. Returns [Ok] bearing the text if the token matches,
     * otherwise [Err].
     */
    fun expectIdentifierOrString(): Result<String, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Identifier -> Ok(token.name)
            is Token.String -> Ok(token.value)
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Number]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectNumber(): Result<Float, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Number -> Ok(token.number.float())
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Percentage]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectPercentage(): Result<Float, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Percentage -> Ok(token.number.float())
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Comma]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectComma(): Result<Empty, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Comma -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Solidus]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectSolidus(): Result<Empty, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Solidus -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Colon]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectColon(): Result<Empty, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Colon -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Expects the next token to be [Token.Bang]. Returns [Ok] if the token matches, otherwise [Err].
     */
    fun expectBang(): Result<Empty, ParseError> {
        val location = sourceLocation()
        val tokenResult = next()

        val token = when (tokenResult) {
            is Ok -> tokenResult.value
            is Err -> return tokenResult
        }

        return when (token) {
            is Token.Bang -> Ok()
            else -> Err(location.newUnexpectedTokenError(token))
        }
    }

    /**
     * Creates a new [ParseError] at the current [SourceLocation] with the specified [kind].
     */
    fun newError(kind: ParseErrorKind): ParseError {
        return ParseError(kind, sourceLocation())
    }

    /**
     * Creates a new [ParseError] bearing [ParseErrorKind.UnexpectedToken] at the current [SourceLocation] with the specified
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
     * [ParseErrorKind.Unexhausted] as error kind if the parse was not exhaustive. Otherwise returns the [Result] of the
     * parse.
     */
    inline fun <T> parseEntirely(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val result = parse(this)

        return when {
            result is Err -> result
            !isExhausted() -> Err(newError(ParseErrorKind.Unexhausted()))
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
            val value = parseUntilBefore(Delimiters.Comma, parse)

            when (value) {
                is Ok -> values.add(value.value)
                is Err -> return value
            }

            val token = next()

            when (token) {
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

        val delimitedParser = Parser(tokenizer.copy(), takeBlockType(), delimitedDelimiters)

        val result = delimitedParser.parseEntirely(parse)

        delimitedParser.blockType.let {
            delimitedParser.tokenizer.consumeUntilEndOfBlock(it)
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

            BlockType.opening(token.unwrap()).let {
                tokenizer.consumeUntilEndOfBlock(it)
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
        val blockType = takeBlockType().expect("not a nested block")

        val nestedDelimiters = delimiters or Delimiters.from(blockType).bits
        val nestedParser = Parser(tokenizer.copy(), None(), nestedDelimiters)

        val result = nestedParser.parseEntirely(parse)

        nestedParser.blockType.let {
            nestedParser.tokenizer.consumeUntilEndOfBlock(it)
        }

        tokenizer.consumeUntilEndOfBlock(blockType)

        return result
    }

    /**
     * "Atomically" takes the current [BlockType], if present, and replaces it with [None].
     */
    private fun takeBlockType(): Option<BlockType> {
        return when (blockType) {
            is Some -> {
                val value = blockType
                blockType = None()
                value
            }
            is None -> {
                blockType
            }
        }
    }
}

/**
 * Creates a new [ParseError] at this [SourceLocation] with the specified [kind].
 */
fun SourceLocation.newError(kind: ParseErrorKind): ParseError {
    return ParseError(kind, this)
}

/**
 * Creates a new [ParseError] bearing [ParseErrorKind.UnexpectedToken] at this [SourceLocation] with the specified
 * [token] as unexpected token.
 */
fun SourceLocation.newUnexpectedTokenError(token: Token): ParseError {
    return ParseError(ParseErrorKind.UnexpectedToken(token), this)
}

/**
 * The input for the [Parser] used both for parsing and slicing.
 */
class ParserInput(internal val text: String)

/**
 * A single state of a [Parser], including all essential information of that very Parser's state. This should be used
 * for error reporting and state reverting.
 */
class ParserState(internal val state: State, internal val blockType: Option<BlockType>) {

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
class ParseError(val kind: ParseErrorKind, val location: SourceLocation) {

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

    class EndOfFile : ParseErrorKind()

    class Unexhausted : ParseErrorKind()

    class UnexpectedToken(val token: Token) : ParseErrorKind()

    class UnsupportedFeature : ParseErrorKind()

    class Unkown : ParseErrorKind()
}