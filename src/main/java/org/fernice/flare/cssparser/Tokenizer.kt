/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.unwrap
import org.fernice.flare.std.systemFlag
import org.fernice.logging.FLogging
import java.util.Stack

/**
 * VM parameter flag for processing the input completely on tokenizer creation and printing all generated
 * token to stdout.
 */
private val print_token: Boolean by lazy { systemFlag("fernice.flare.printToken") }

/**
 * Represents a resettable stream of tokens, that also provides special methods for advanced stream advancing,
 * such as nested block skipping.
 */
class Tokenizer private constructor(
    private val text: String,
    private val lexer: Lexer,
    private var state: State
) {

    init {
        if (print_token) {
            var iter = state

            while (true) {
                if (iter.token is Err) {
                    break
                }

                LOG.trace("${iter.token} ${iter.sourceLocation}")

                iter = when (val next = iter.next) {
                    is None -> {
                        val nextState = State.next(lexer)
                        state.next = Some(nextState)
                        nextState
                    }
                    is Some -> next.value
                }
            }
        }
    }

    /**
     * Advances the token stream by one [Token] and returns it. Returns [Err], if the stream is exhausted or an error occurred.
     */
    fun nextToken(): Result<Token, Unit> {
        val token = state.token

        if (token is Err) {
            return token
        }

        state = when (val next = state.next) {
            is None -> {
                val nextState = State.next(lexer)
                state.next = Some(nextState)
                nextState
            }
            is Some -> next.value
        }

        return token
    }

    /**
     * Retrieves the next [Token] without advancing the token stream and returns it. Returns [Err], if the stream is exhausted
     * or an error occurred.
     */
    fun peekToken(count: Int): Result<Token, Unit> {
        var state = state

        for (i in 0 until count) {
            if (state.token is Err) {
                return state.token
            }

            state = when (val next = state.next) {
                is None -> {
                    val nextState = State.next(lexer)
                    state.next = Some(nextState)
                    nextState
                }
                is Some -> next.value
            }
        }

        return state.token
    }

    /**
     * Returns the current [State] of the Tokenizer. The State may be used in combination with [reset] to restore a previous
     * position in the token stream.
     */
    fun state(): State {
        return state
    }

    /**
     * Resets the [State] of the Tokenizer to a earlier one defined by the specified [state]. The State of the Tokenizer may
     * be obtained by calling [state].
     *
     * Resetting the state of the Tokenizer performs in O(1).
     */
    fun reset(state: State) {
        this.state = state
    }

    /**
     * Returns the current [SourcePosition] of this Tokenizer. The source position is the index in the char stream starting at 0.
     * The position should be used for slicing the input in order to provide more information when reporting errors.
     */
    fun position(): SourcePosition {
        return state.sourcePosition
    }

    /**
     * Returns the current [SourceLocation] of this Tokenizer. The source location is the line and the position within that line
     * both starting at zero. The location should be used for error reporting, as it represents human readable position.
     */
    fun location(): SourceLocation {
        return state.sourceLocation
    }

    /**
     * Slices the input onwards from the specified [position] to the current [SourcePosition].
     */
    fun sliceFrom(start: SourcePosition): String {
        return text.substring(start.position, position().position)
    }

    /**
     * Slices the input from the specified [start] to the specified [end].
     */
    fun slice(start: SourcePosition, end: SourcePosition): String {
        return text.substring(start.position, end.position)
    }

    /**
     * Clones the tokenizer returning a identical instance of such.
     */
    fun clone(): Tokenizer {
        return Tokenizer(text, lexer, state)
    }

    /**
     * Advances the token stream until a closing [Token] occurs, that matches the specified [BlockType]. Keeps track of any
     * additional nested blocks, to prevent early returns on closing Tokens.
     */
    fun consumeUntilEndOfBlock(type: BlockType) {
        val stack = Stack<BlockType>()
        stack.push(type)

        loop@
        while (true) {
            val token = when (val token = nextToken()) {
                is Ok -> token.value
                is Err -> break@loop
            }

            val closing = BlockType.closing(token)

            if (closing is Some && stack.peek() == closing.value) {
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

    /**
     * Advances the token stream until the next [Token] is one of specified [delimiters] or the end of file is reached.
     */
    fun consumeUntilBefore(delimiters: Int) {
        loop@
        while (true) {
            val peekResult = peekToken(0)

            if (peekResult is Err || delimiters and Delimiters.from(peekResult.unwrap()).bits != 0) {
                break
            }

            val blockType = when (val token = nextToken()) {
                is Ok -> BlockType.opening(token.value)
                is Err -> break@loop
            }

            if (blockType is Some) {
                consumeUntilEndOfBlock(blockType.value)
            }
        }
    }

    override fun toString(): String {
        return "Tokenizer($state)"
    }


    companion object {

        /**
         * Create a new Tokenizer using the specified [text] as the input for the token stream.
         */
        fun new(text: String): Tokenizer {
            val lexer = Lexer(Reader(text))

            return Tokenizer(
                text,
                lexer,
                State.next(lexer)
            )
        }

        private val LOG = FLogging.logger { }
    }
}

/**
 * Represents a concrete state of a [Tokenizer] bearing the [Token], [SourcePosition] and [SourceLocation] at that very state.
 */
data class State(
    val token: Result<Token, Unit>,
    val sourcePosition: SourcePosition,
    val sourceLocation: SourceLocation
) {

    /**
     * Actual implementation of the token stream as a linked list of States. Each State holds a lazy reference to next state
     * but non to the previous. Such a reference must be held by any person of interest in order for him to be able to revert
     * to a previous state. By not holding a reference to a previous State memory management becomes a trivial case, where
     * tokens are automatically released after the have been passed by the tokenizer also long as no one else holds a reference
     * to them.
     */
    internal var next: Option<State> = None

    companion object {

        internal fun next(lexer: Lexer): State {
            val sourcePosition = lexer.position()
            val sourceLocation = lexer.location()

            return State(lexer.nextToken(), sourcePosition, sourceLocation)
        }
    }
}
