/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.cssparser

class Reader internal constructor(buffer: CharArray, private val bufLen: Int) {

    private val buffer: CharArray

    /**
     * The current position of the reader within the buffer.
     */
    // package-private to prevent external manipulation
    @JvmField
    internal var bp: Int = 0

    /**
     * The current char. If the end of file has been reached, this char will always be \u001a and [.isEoF] will
     * return true.
     */
    @JvmField
    var c: Char = ' '

    /**
     * The current line in the text starting at 0. This can be used for error reporting.
     */
    @JvmField
    var line: Int = 0

    /**
     * The current column or position in the current line starting at 0. This can be used for error reporting.
     */
    @JvmField
    var column = -1

    private var word: CharArray
    private var wordPos: Int = 0

    /**
     * Returns if the reader reached end of file. If true, the current char will always be \u001a.
     *
     * @return true if end of file, otherwise false
     */
    val isEoF: Boolean
        get() = bp >= bufLen

    internal constructor(text: String) : this(text.toCharArray(), text.length) {}

    init {
        var buffer = buffer
        if (bufLen == buffer.size) {
            val newBuffer = CharArray(bufLen + 1)
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.size)
            buffer = newBuffer
        }

        this.word = CharArray(128)
        this.buffer = buffer
        this.bp = -1
        this.buffer[bufLen] = 26.toChar()
        nextChar()
    }

    /**
     * Advances the char stream virtually by one char. Automatically converts all line break into \n. If end of file is
     * reached, the next char will always \u001a
     */
    fun nextChar() {
        if (bp < bufLen) {
            c = buffer[++bp]
            column++

            if (c == '\r') {
                if (peekChar() == '\n') {
                    bp++
                    column++
                }
                c = '\n'
            } else if (c == '\u000C') {
                c = '\n'
            }

            if (c == '\n') {
                line++
                column = 0
            }
        }
    }

    /**
     * Advances the char stream virtually by the amount of chars specified. Automatically converts all line break into \n.
     *
     * @param count chars to advance
     * @see .nextChar
     */
    fun nextChar(count: Int) {
        for (i in 0 until count) {
            nextChar()
        }
    }

    /**
     * Returns the nth next char with advancing the char stream.
     *
     * @param nth the relative index of the next char
     * @return the nth next char
     */
    @JvmOverloads
    fun peekChar(nth: Int = 1): Char {
        return if (bp + nth >= bufLen) {
            buffer[bufLen]
        } else buffer[bp + nth]
    }

    /**
     * Puts the nth next chars into the text buffer and advances the char stream.
     *
     * @param nth the n next chars
     */
    fun putChar(nth: Int) {
        for (i in 0 until nth) {
            putChar()
        }
    }

    /**
     * Puts the current char into the text buffer and optionally advances the char stream.
     *
     * @param read whether to advance the char stream
     */
    @JvmOverloads
    fun putChar(read: Boolean = true) {
        putChar(c, read)
    }

    /**
     * Puts the specified char into the text buffer and optionally advances the char stream.
     *
     * @param c    the char to put into the text buffer
     * @param read whether to advances the char stream
     */
    fun putChar(c: Char, read: Boolean) {
        word = ensureCapacity(word, wordPos)

        word[wordPos++] = c

        if (read) {
            nextChar()
        }
    }

    /**
     * Returns the string composed in the text buffer and clear the text buffer.
     *
     * @return the string of the text buffer
     */
    fun text(): String {
        val text = String(word, 0, wordPos)
        wordPos = 0
        return text
    }

    private fun ensureCapacity(buffer: CharArray, position: Int): CharArray {
        if (position >= buffer.size) {
            var length = buffer.size

            while (length < position + 1) {
                length *= 2
            }

            val newBuffer = CharArray(length)
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.size)
            return newBuffer
        }
        return buffer
    }
}