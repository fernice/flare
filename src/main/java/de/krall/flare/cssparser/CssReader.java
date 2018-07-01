package de.krall.flare.cssparser;

/**
 * Special implementation of a string reader for a css tokenizer. Allows direct access to the current char,
 * line, column and position as well as building a string inline.
 */
public final class CssReader {

    private char[] buffer;

    /**
     * The current position of the reader within the buffer.
     */
    // package-private to prevent external manipulation
    int bp;
    private int bufLen;

    /**
     * The current char. If the end of file has been reached, this char will always be \u001a and {@link #isEoF()} will
     * return true.
     */
    public char c;

    /**
     * The current line in the text starting at 0. This can be used for error reporting.
     */
    public int line;

    /**
     * The current column or position in the current line starting at 0. This can be used for error reporting.
     */
    public int column = -1;

    private char[] word;
    private int wordPos;

    CssReader(String text) {
        this(text.toCharArray(), text.length());
    }

    CssReader(char[] buffer, final int length) {
        if (length == buffer.length) {
            final char[] newBuffer = new char[length + 1];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }

        this.word = new char[128];
        this.buffer = buffer;
        this.bufLen = length;
        this.bp = -1;
        this.buffer[bufLen] = 26;
        nextChar();
    }

    /**
     * Advances the char stream virtually by one char. Automatically converts all line break into \n. If end of file is
     * reached, the next char will always \u001a
     */
    public void nextChar() {
        if (bp < bufLen) {
            c = buffer[++bp];
            column++;

            if (c == '\r') {
                if (peekChar() == '\n') {
                    bp++;
                    column++;
                }
                c = '\n';
            } else if (c == '\f') {
                c = '\n';
            }

            if (c == '\n') {
                line++;
                column = 0;
            }
        }
    }

    /**
     * Advances the char stream virtually by the amount of chars specified. Automatically converts all line break into \n.
     *
     * @param count chars to advance
     * @see #nextChar() called internally
     */
    public void nextChar(final int count) {
        for (int i = 0; i < count; i++) {
            nextChar();
        }
    }

    /**
     * Returns the next char without advancing the char stream.
     *
     * @return the next char
     */
    public char peekChar() {
        return peekChar(1);
    }

    /**
     * Returns the nth next char with advancing the char stream.
     *
     * @param nth the relative index of the next char
     * @return the nth next char
     */
    public char peekChar(final int nth) {
        if (bp + nth >= bufLen) {
            return buffer[bufLen];
        }
        return buffer[bp + nth];
    }

    /**
     * Returns if the reader reached end of file. If true, the current char will always be \u001a.
     *
     * @return true if end of file, otherwise false
     */
    public boolean isEoF() {
        return bp >= bufLen;
    }

    /**
     * Puts the nth next chars into the text buffer and advances the char stream.
     *
     * @param nth the n next chars
     */
    public void putChar(final int nth) {
        for (int i = 0; i < nth; i++) {
            putChar();
        }
    }

    /**
     * Puts the current char into the text buffer and advances the char stream.
     */
    public void putChar() {
        putChar(true);
    }

    /**
     * Puts the current char into the text buffer and optionally advances the char stream.
     *
     * @param read whether to advance the char stream
     */
    public void putChar(final boolean read) {
        putChar(c, read);
    }

    /**
     * Puts the specified char into the text buffer and optionally advances the char stream.
     *
     * @param c the char to put into the text buffer
     * @param read whether to advances the char stream
     */
    public void putChar(final char c, final boolean read) {
        word = ensureCapacity(word, wordPos);

        word[wordPos++] = c;

        if (read) {
            nextChar();
        }
    }

    /**
     * Returns the string composed in the text buffer and clear the text buffer.
     *
     * @return the string of the text buffer
     */
    public String text() {
        final String text = new String(word, 0, wordPos);
        wordPos = 0;
        return text;
    }

    private static char[] ensureCapacity(final char[] buffer, final int position) {
        if (position >= buffer.length) {
            int length = buffer.length;

            while (length < position + 1) {
                length *= 2;
            }

            final char[] newBuffer = new char[length];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            return newBuffer;
        }
        return buffer;
    }
}
