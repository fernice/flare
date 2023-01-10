/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.systemFlag
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
    private var state: State,
) {

    private var seenVarFunctions = SeenStatus.Ignore

    init {
        if (print_token) {
            var iter = state

            while (true) {
                iter.token ?: break

                LOG.trace("${iter.token} ${iter.sourceLocation}")

                iter = when (val next = iter.next) {
                    null -> {
                        val nextState = State.next(lexer)
                        state.next = nextState
                        nextState
                    }

                    else -> next
                }
            }
        }
    }

    /**
     * Advances the token stream by one [Token] and returns it. Returns [Err], if the stream is exhausted or an error occurred.
     */
    fun nextToken(): Token? {
        val token = state.token ?: return null

        state = when (val next = state.next) {
            null -> {
                val nextState = State.next(lexer)
                state.next = nextState
                nextState
            }

            else -> next
        }

        if (seenVarFunctions == SeenStatus.Looking) {
            if (token is Token.Function && token.name == "var") {
                seenVarFunctions = SeenStatus.Seen
            }
        }

        return token
    }

    /**
     * Retrieves the next [Token] without advancing the token stream and returns it. Returns [Err], if the stream is exhausted
     * or an error occurred.
     */
    fun peekToken(count: Int): Token? {
        var state = state

        for (i in 1 until count) {
            state.token ?: return null

            state = when (val next = state.next) {
                null -> {
                    val nextState = State.next(lexer)
                    state.next = nextState
                    nextState
                }

                else -> next
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

    fun lookForVarFunctions() {
        seenVarFunctions = SeenStatus.Looking
    }

    fun seenVarFunctions(): Boolean {
        val seenVarFunctions = this.seenVarFunctions
        this.seenVarFunctions = SeenStatus.Ignore
        return seenVarFunctions == SeenStatus.Seen
    }

    /**
     * Advances the token stream until a closing [Token] occurs, that matches the specified [BlockType]. Keeps track of any
     * additional nested blocks, to prevent early returns on closing Tokens.
     */
    fun consumeUntilEndOfBlock(type: BlockType) {
        val stack = Stack<BlockType>()
        stack.push(type)

        while (true) {
            val token = nextToken() ?: break

            val opening = BlockType.opening(token)
            if (opening != null) {
                stack.push(opening)
                continue
            }

            val closing = BlockType.closing(token)
            if (closing != null && stack.peek() == closing) {
                stack.pop()

                if (stack.isEmpty()) return
            }
        }
    }

    /**
     * Advances the token stream until the next [Token] is one of specified [delimiters] or the end of file is reached.
     */
    fun consumeUntilBefore(delimiters: Int) {
        while (true) {
            val peekToken = peekToken(0) ?: break

            if (delimiters and Delimiters.from(peekToken).bits != 0) break

            val nextToken = nextToken() ?: break

            val blockType = BlockType.opening(nextToken)
            if (blockType != null) {
                consumeUntilEndOfBlock(blockType)
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
    val token: Token?,
    val sourcePosition: SourcePosition,
    val sourceLocation: SourceLocation,
) {

    /**
     * Actual implementation of the token stream as a linked list of States. Each State holds a lazy reference to next state
     * but non to the previous. Such a reference must be held by any person of interest in order for him to be able to revert
     * to a previous state. By not holding a reference to a previous State memory management becomes a trivial case, where
     * tokens are automatically released after the have been passed by the tokenizer also long as no one else holds a reference
     * to them.
     */
    internal var next: State? = null

    companion object {

        internal fun next(lexer: Lexer): State {
            val sourcePosition = lexer.position()
            val sourceLocation = lexer.location()

            return State(lexer.nextToken(), sourcePosition, sourceLocation)
        }
    }
}
