package de.krall.flare.cssparser

import de.krall.flare.std.*

class Parser private constructor(val tokenizer: Tokenizer, var blockType: Option<BlockType>, val delimiters: Int) {

    constructor(parserInput: ParserInput) : this(Tokenizer(parserInput.text), None(), 0)

    fun state(): ParserState {
        return ParserState(tokenizer.state(), blockType)
    }

    fun reset(state: ParserState) {
        tokenizer.reset(state.state)
        blockType = state.blockType
    }

    fun sourcePosition(): SourcePosition {
        return tokenizer.position()
    }

    fun sourceLocation(): SourceLocation {
        return tokenizer.location()
    }

    fun sliceFrom(position: SourcePosition): String {
        return tokenizer.sliceFrom(position)
    }

    fun slice(start: SourcePosition, end: SourcePosition): String {
        return tokenizer.slice(start, end)
    }

    fun next(): Result<Token, ParseError> {
        skipWhitespace()
        return nextIncludingWhitespace()
    }

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

    fun skipWhitespace(): Result<Token, ParseError> {
        while (true) {
            val state = state()
            val result = nextIncludingWhitespaceAndComment()

            when (result) {
                is Err -> {
                    reset(state)
                    return result
                }
                is Ok -> {
                    if (result.value !is Token.Whitespace) {
                        reset(state)
                        return result
                    }
                }
            }
        }
    }

    fun isExhausted(): Boolean {
        return expectExhausted() is Ok
    }

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
                Err(state.sourceLocation().newUnexpectedTokenError(tokenResult.value))
            }
        }

        reset(state)

        return result
    }

    fun expectIdentifierMatching(text: String): Result<Empty, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Identifier && tokenResult.value.name.equals(text, true)) {
                    Ok()
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectIdentifier(): Result<String, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Identifier) {
                    Ok(tokenResult.value.name)
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectNumber(): Result<Float, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Number) {
                    Ok(tokenResult.value.number.float())
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectPercentage(): Result<Float, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Percentage) {
                    Ok(tokenResult.value.number.float())
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectComma(): Result<Empty, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Comma) {
                    Ok()
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectSolidus(): Result<Empty, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Solidus) {
                    Ok()
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun expectBang(): Result<Empty, ParseError> {
        val tokenResult = next()

        return when (tokenResult) {
            is Ok -> {
                if (tokenResult.value is Token.Bang) {
                    Ok()
                } else {
                    Err(newUnexpectedTokenError(tokenResult.value))
                }
            }
            is Err -> {
                tokenResult
            }
        }
    }

    fun newError(kind: ParseErrorKind): ParseError {
        return ParseError(kind, sourceLocation())
    }

    fun newUnexpectedTokenError(token: Token): ParseError {
        return ParseError(ParseErrorKind.UnexpectedToken(token), sourceLocation())
    }

    fun <T> tryParse(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val state = state()

        val result = parse(this)

        if (result is Err) {
            reset(state)
        }

        return result
    }

    fun <T> parseEntirely(parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val result = parse(this)

        return when {
            result is Err -> result
            !isExhausted() -> Err(newError(ParseErrorKind.Unexhausted()))
            else -> result
        }
    }

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

    fun <T> parseUntilBefore(delimiters: Delimiters, parse: (Parser) -> Result<T, ParseError>): Result<T, ParseError> {
        val delimitedDelimiters = this.delimiters or delimiters.bits

        val delimitedParser = Parser(tokenizer.copy(), takeBlockType(), delimitedDelimiters)

        val result = delimitedParser.parseEntirely(parse)

        delimitedParser.blockType.let {
            delimitedParser.tokenizer.consumeUntilEndOfBlock(it)
        }

        tokenizer.consumeUntilBefore(delimiters.bits)

        return result
    }

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

    private fun takeBlockType(): Option<BlockType> {
        return when (blockType) {
            is Some -> {
                val value = blockType;
                blockType = None()
                value
            }
            is None -> {
                blockType
            }
        }
    }
}

fun SourceLocation.newError(kind: ParseErrorKind): ParseError {
    return ParseError(kind, this)
}

fun SourceLocation.newUnexpectedTokenError(token: Token): ParseError {
    return ParseError(ParseErrorKind.UnexpectedToken(token), this)
}

class ParserInput(internal val text: String)

class ParserState(internal val state: State, internal val blockType: Option<BlockType>) {

    fun sourcePosition(): SourcePosition {
        return state.sourcePosition
    }

    fun sourceLocation(): SourceLocation {
        return state.sourceLocation
    }
}

class ParseError(val kind: ParseErrorKind, val location: SourceLocation) {

    override fun toString(): String {
        return "$kind @ $location"
    }
}

open class ParseErrorKind {

    override fun toString(): String {
        return "ParseErrorKind::${javaClass.simpleName}"
    }

    class EndOfFile : ParseErrorKind()

    class Unexhausted : ParseErrorKind()

    class UnexpectedToken(val token: Token) : ParseErrorKind()
}