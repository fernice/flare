package de.krall.flare.cssparser

import de.krall.flare.std.*
import java.util.*

class Tokenizer private constructor(private val text: String, private val lexer: Lexer, private var state: State) {

    private constructor(text: String, lexer: Lexer) : this(text, lexer, State.new(lexer))

    constructor(text: String) : this(text, Lexer(CssReader(text)))

    fun nextToken(): Result<Token, Empty> {
        val token = state.token

        if (token is Err) {
            return token
        }

        if (state.next == null) {
            state.next = State.new(lexer)
        }

        state = state.next!!

        return token
    }

    fun peekToken(count: Int): Result<Token, Empty> {
        var state = state

        for (i in 0 until count) {
            if (state.token !is Err && state.next == null) {
                state.next = State.new(lexer)
            }

            state = state.next!!
        }

        return state.token
    }

    fun state(): State {
        return state;
    }

    fun reset(state: State) {
        this.state = state
    }

    fun position(): SourcePosition {
        return state.sourcePosition
    }

    fun location(): SourceLocation {
        return state.sourceLocation
    }

    fun sliceFrom(position: SourcePosition): String {
        return text.substring(position.position)
    }

    fun slice(start: SourcePosition, end: SourcePosition): String {
        return text.substring(start.position, end.position)
    }

    fun copy(): Tokenizer {
        return Tokenizer(text, lexer, state)
    }

    fun consumeUntilEndOfBlock(type: BlockType) {
        val stack = Stack<BlockType>()
        stack.push(type)

        loop@
        while (true) {
            val tokenResult = nextToken()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> break@loop
            }

            val closing = BlockType.closing(token)

            if (stack.peek() === closing) {
                stack.pop()

                if (stack.isEmpty()) {
                    return
                }
            } else {
                val opening = BlockType.opening(token)

                if (opening is Some) {
                    stack.push(opening.value)
                }
            }
        }
    }

    fun consumeUntilBefore(delimiters: Int) {
        loop@
        while (true) {
            val peekResult = peekToken(1)

            if (peekResult is Err || delimiters and Delimiters.from(peekResult.unwrap()).bits != 0) {
                break
            }

            val tokenResult = nextToken()

            val blockType = when (tokenResult) {
                is Ok -> BlockType.opening(tokenResult.value)
                is Err -> break@loop
            }

            if (blockType is Some) {
                consumeUntilEndOfBlock(blockType.value)
            }
        }
    }
}

class State(val token: Result<Token, Empty>, val sourcePosition: SourcePosition, val sourceLocation: SourceLocation) {

    var next: State? = null

    companion object {

        internal fun new(lexer: Lexer): State {
            val sourcePosition = lexer.position()
            val sourceLocation = lexer.location()

            return State(lexer.nextToken(), sourcePosition, sourceLocation)
        }
    }
}