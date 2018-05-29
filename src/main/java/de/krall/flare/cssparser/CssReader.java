package de.krall.flare.cssparser;

import de.krall.flare.css.properties.longhand.BackgroundAttachment;
import de.krall.flare.css.properties.longhand.BackgroundAttachmentId;

public final class CssReader {

    {
        BackgroundAttachmentId.Companion.getInstance();
    }

    private char[] buffer;
    int bp;
    private int bufLen;

    public char c;

    public int line;
    public int column = -1;

    public char[] word;
    public int wordPos;

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

    public void nextChar(final int count) {
        for (int i = 0; i < count; i++) {
            nextChar();
        }
    }

    public char peekChar() {
        return peekChar(1);
    }

    public char peekChar(final int pos) {
        if (bp + pos >= bufLen) {
            return buffer[bufLen];
        }
        return buffer[bp + pos];
    }

    public boolean isEoF() {
        return bp >= bufLen;
    }

    public void putChar(final int count) {
        for (int i = 0; i < count; i++) {
            putChar();
        }
    }

    public void putChar() {
        putChar(true);
    }

    public void putChar(final boolean read) {
        putChar(c, read);
    }

    public void putChar(final char c, final boolean read) {
        word = ensureCapacity(word, wordPos);

        word[wordPos++] = c;

        if (read) {
            nextChar();
        }
    }

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
