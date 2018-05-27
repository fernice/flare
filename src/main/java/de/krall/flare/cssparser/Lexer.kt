package de.krall.flare.cssparser

import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

class SourcePosition(val position: Int)

class SourceLocation(val line: Int, val column: Int)

/**
 * This is a implementation of the Css Level 3 syntax specification.
 * <br/>The specification can be found <a href="https://www.w3.org/TR/css-syntax-3/">here</a>.
 */
class Lexer(private val reader: CssReader) {

    fun position(): SourcePosition {
        return SourcePosition(reader.bp)
    }

    fun location(): SourceLocation {
        return SourceLocation(reader.line, reader.column)
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-token
    fun nextToken(): Result<Token, Nothing> {
        if (reader.isEoF) {
            return Err()
        }

        val token = when (reader.c) {
            ' ', '\n', '\t' -> {
                do {
                    reader.nextChar()
                } while (reader.c == ' ' || reader.c == '\t')

                Token.Whitespace()
            }
            '"', '\'' -> {
                consumeString(reader.c)
            }
            '#' -> {
                reader.nextChar()

                if (isName(reader.c) || isEscape(reader.c, reader.peekChar())) {
                    consumeHash()
                } else {
                    Token.Delimiter('#')
                }
            }
            '$' -> {
                reader.nextChar()

                if (reader.c == '=') {
                    Token.SuffixMatch()
                } else {
                    Token.Delimiter('$')
                }
            }
            '(' -> {
                reader.nextChar()
                Token.LParen()
            }
            ')' -> {
                reader.nextChar()
                Token.RParen()
            }
            '*' -> {
                reader.nextChar()

                if (reader.c == '=') {
                    Token.SubstringMatch()
                } else {
                    Token.Asterisk()
                }
            }
            '+' -> {
                if (startsNumber(reader.c, reader.peekChar(), reader.peekChar(2))) {
                    consumeNumeric()
                } else {
                    reader.nextChar()
                    Token.Plus()
                }
            }
            ',' -> {
                reader.nextChar()
                Token.Comma()
            }
            '-' -> {
                when {
                    startsNumber(reader.c, reader.peekChar(), reader.peekChar(2)) -> {
                        consumeNumeric()
                    }
                    startsIdentifier(reader.c, reader.peekChar(), reader.peekChar(2)) -> {
                        consumeIdentifier()
                    }
                    reader.peekChar() == '-' && reader.peekChar(2) == '>' -> {
                        reader.nextChar(3)
                        Token.CDC()
                    }
                    else -> {
                        reader.nextChar()
                        Token.Minus()
                    }
                }
            }
            '.' -> {
                if (startsNumber(reader.c, reader.peekChar(), reader.peekChar(2))) {
                    consumeNumeric()
                } else {
                    Token.Dot()
                }
            }
            '/' -> {
                reader.nextChar()

                if (reader.c == '*') {
                    consumeComment()
                } else {
                    Token.Solidus()
                }
            }
            ':' -> {
                reader.nextChar()
                Token.Colon()
            }
            ';' -> {
                reader.nextChar()
                Token.SemiColon()
            }
            '<' -> {
                reader.nextChar()
                Token.Lt()
            }
            '>' -> {
                reader.nextChar()
                Token.Gt()
            }
            '@' -> {
                reader.nextChar()

                if (startsIdentifier(reader.c, reader.peekChar(), reader.peekChar(2))) {
                    consumeIdentifier()
                } else {
                    Token.Delimiter('@')
                }
            }
            '\\' -> {
                if (isEscape(reader.c, reader.peekChar())) {
                    consumeIdentifier()
                } else {
                    Token.Delimiter('\\')
                }
            }
            '^' -> {
                if (reader.c == '=') {
                    Token.PrefixMatch()
                } else {
                    Token.Delimiter('^')
                }
            }
            'u', 'U' -> {
                reader.nextChar()

                if (reader.c == '+' && (isHexDigit(reader.peekChar()) || reader.peekChar() == '?')) {
                    reader.nextChar()
                    consumeUnicodeRange()
                } else {
                    consumeIdentifier()
                }
            }
            '|' -> {
                reader.nextChar()

                when (reader.c) {
                    '=' -> {
                        reader.nextChar()
                        Token.DashMatch()
                    }
                    '|' -> {
                        reader.nextChar()
                        Token.Column()
                    }
                    else -> {
                        Token.Pipe()
                    }
                }
            }
            '~' -> {
                reader.nextChar()

                if (reader.c == '=') {
                    reader.nextChar()
                    Token.IncludeMatch()
                } else {
                    Token.Tidle()
                }
            }
            '{' -> {
                reader.nextChar()
                Token.LBrace()
            }
            '}' -> {
                reader.nextChar()
                Token.RBrace()
            }
            '[' -> {
                reader.nextChar()
                Token.LBracket()
            }
            ']' -> {
                reader.nextChar()
                Token.RBracket()
            }
            in '0'..'9' -> {
                consumeNumeric()
            }
            else -> {
                if (isLetter(reader.c)) {
                    consumeIdentifier()
                } else {
                    Token.Delimiter(reader.c)
                }
            }
        }

        return Ok(token)
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-token
    private fun consumeHash(): Token {
        val identifier = startsIdentifier(reader.c, reader.peekChar(), reader.peekChar(2))

        val name = consumeName()

        return if (identifier) {
            Token.IdHash(name)
        } else {
            Token.Hash(name)
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-an-ident-like-token0
    private fun consumeIdentifier(): Token {
        val name = consumeName()

        return if (name.equals("url", true) && reader.c == 'c') {
            reader.nextChar()
            consumeUrl()
        } else if (reader.c == 'c') {
            Token.Function(name)
        } else {
            Token.Identifier(name)
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-name0
    private fun consumeName(): String {
        while (true) {
            when {
                isName(reader.c) -> {
                    reader.putChar()
                }
                isEscape(reader.c, reader.peekChar()) -> {
                    // skip the escape character
                    reader.nextChar()
                    reader.putChar(readEscaped(), false)
                }
                else -> {
                    return reader.text()
                }
            }
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-url-token0
    private fun consumeUrl(): Token {
        while (isWhitespace(reader.c)) {
            reader.nextChar()
        }

        if (reader.isEoF) {
            return Token.BadUrl(reader.text())
        }

        if (reader.c == '"' || reader.c == '\'') {
            val endChar = reader.c
            reader.nextChar()

            val token = consumeString(endChar)

            when (token) {
                is Token.BadString -> {
                    consumeUrlRemnants()
                    return Token.BadUrl(token.value)
                }
                is Token.String -> {
                    while (isWhitespace(reader.c)) {
                        reader.nextChar()
                    }

                    return if (reader.c == ')' || reader.isEoF) {
                        Token.Url(token.value)
                    } else {
                        consumeUrlRemnants()
                        Token.BadUrl(token.value)
                    }
                }
            }
        }

        while (true) {
            when (reader.c) {
                '\u001a' -> {
                    if (reader.isEoF) {
                        return Token.Url(reader.text())
                    }
                }
                ')' -> {
                    return Token.Url(reader.text())
                }
                '\\' -> {
                    if (isEscape(reader.c, reader.peekChar())) {
                        reader.nextChar()
                        reader.putChar(readEscaped(), true)
                    } else {
                        consumeUrlRemnants()
                        return Token.BadString(reader.text())
                    }
                }
                ' ', '\t' -> {
                    do {
                        reader.nextChar()
                    } while (isWhitespace(reader.c))

                    return if (reader.c == ')' || reader.isEoF) {
                        Token.Url(reader.text())
                    } else {
                        consumeUrlRemnants()
                        Token.BadString(reader.text())
                    }
                }
                '"', '\'', '(' -> {
                    consumeUrlRemnants()
                    Token.BadString(reader.text())
                }
                else -> {
                    if (isNonPrintable(reader.c)) {
                        reader.nextChar()
                        consumeUrlRemnants()
                        Token.BadString(reader.text())
                    }

                    reader.putChar()
                }
            }
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-the-remnants-of-a-bad-url0
    private fun consumeUrlRemnants() {
        while (true) {
            if (reader.c == ')' || reader.isEoF) {
                return
            }

            if (isEscape(reader.c, reader.peekChar())) {
                reader.nextChar()
                readEscaped()
            }

            reader.nextChar()
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-numeric-token0
    private fun consumeNumeric(): Token {
        val number = consumeNumber()

        return when {
            startsIdentifier(reader.c, reader.peekChar(), reader.peekChar(2)) -> {
                val unit = consumeName()

                Token.Dimension(number, unit)
            }
            reader.c == '%' -> {
                Token.Percentage(number)
            }
            else -> {
                Token.Number(number)
            }
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-number0
    private fun consumeNumber(): Number {
        var type = "int"

        val negative = if (reader.c == '+' || reader.c == '-') {
            reader.putChar()
            true
        } else {
            false
        }

        while (isDigit(reader.c)) {
            reader.putChar()
        }

        if (reader.c == '.' && isDigit(reader.c)) {
            reader.putChar(2)
            type = "number"

            while (isDigit(reader.c)) {
                reader.putChar()
            }
        }

        if (reader.c == 'E' || reader.c == 'e') {
            val consume = if (isDigit(reader.peekChar())) {
                reader.putChar(2)
                true
            } else if ((reader.peekChar() == '+' || reader.peekChar() == '-') && isDigit(reader.peekChar(2))) {
                reader.putChar(3)
                true
            } else {
                false
            }

            if (consume) {
                type = "number"
                while (isDigit(reader.c)) {
                    reader.putChar()
                }
            }
        }

        val number = reader.text()

        return Number(type, number, convertNumber(number), negative)
    }

    // https://www.w3.org/TR/css-syntax-3/#convert-a-string-to-a-number0
    private fun convertNumber(number: String): Double {
        val reader = CssReader(number.toCharArray(), number.length)

        var s = 1

        when (reader.c) {
            '-' -> {
                s = -1
                reader.nextChar()
            }
            '+' -> {
                reader.nextChar()
            }
        }

        i@ while (true) {
            when (reader.c) {
                '.', 'e', 'E', '\u001a' -> {
                    break@i
                }
                else -> {
                    reader.putChar()
                }
            }
        }

        var text = reader.text()

        val i = if (text.isNotEmpty()) Integer.parseInt(text) else 0

        var d = 0
        fd@ while (true) {
            when (reader.c) {
                'e', 'E', '\u001a' -> {
                    break@fd
                }
                else -> {
                    d++
                    reader.putChar()
                }
            }
        }

        val f = if (d > 0) Integer.parseInt(reader.text()) else 0

        var t = 1
        te@ while (true) {
            when (reader.c) {
                'e', 'E', '+' -> reader.nextChar()
                '-' -> {
                    t = -1
                    break@te
                }
                '\u001a' -> {
                    break@te
                }
                else -> {
                    reader.putChar()
                }
            }
        }

        text = reader.text()

        val e = if (text.isNotEmpty()) Integer.parseInt(text) else 0

        return s.toDouble() * (i + f * Math.pow(10.0, (-d).toDouble())) * Math.pow(10.0, (-t * e).toDouble())
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-string-token0
    private fun consumeString(delimiter: Char): Token {
        reader.nextChar()

        loop@
        while (!reader.isEoF) {
            when (reader.c) {
                '\n' -> {
                    return Token.BadString(reader.text())
                }
                '\\' -> {
                    if (isEscape(reader.c, reader.peekChar())) {
                        reader.nextChar()
                        reader.putChar(readEscaped(), false)
                    }
                }
                '"', '\'' -> {
                    if (reader.c == delimiter) {
                        break@loop
                    }
                }
            }
        }

        return Token.String(reader.text())
    }

    private fun consumeComment(): Token {
        reader.nextChar()

        do {
            reader.putChar()
        } while (reader.c != '*' && reader.peekChar() != '/' || reader.isEoF)
        // also consume the /
        reader.nextChar()

        return Token.Comment(reader.text())
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-a-unicode-range-token0
    private fun consumeUnicodeRange(): Token {
        var containsWildcard = false
        for (i in 0..5) {
            if (isHexDigit(reader.c)) {
                reader.putChar()
            } else if (reader.c == '?') {
                reader.putChar()
                containsWildcard = true
            } else {
                break
            }
        }

        val word = reader.text()

        val startRange: Int
        val endRange: Int
        if (containsWildcard) {
            startRange = Integer.parseInt(word.replace("\\?".toRegex(), "0"), 16)
            endRange = Integer.parseInt(word.replace("\\?".toRegex(), "F"), 16)

            return Token.UnicodeRange(startRange, endRange)
        }

        startRange = Integer.parseInt(word, 16)

        return if (reader.c == '-' && isHexDigit(reader.peekChar())) {
            reader.nextChar()

            for (i in 0..5) {
                if (isHexDigit(reader.c)) {
                    reader.putChar()
                } else {
                    break
                }
            }

            Token.UnicodeRange(startRange, Integer.parseInt(reader.text(), 16))
        } else {
            Token.UnicodeRange(startRange, startRange)
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-an-escaped-code-point0
    private fun readEscaped(): Char {
        if (isHexDigit(reader.c)) {
            for (i in 0..5) {
                if (isHexDigit(reader.c)) {
                    reader.putChar()
                } else {
                    break
                }
            }

            if (isWhitespace(reader.c)) {
                reader.nextChar()
            }

            val code = Integer.parseInt(reader.text(), 16)

            return if (code == 0 || isSurrogate(code.toChar()) || code > 0x10ffff) {
                '\ufffd'
            } else {
                code.toChar()
            }
        } else if (reader.isEoF) {
            return '\ufffd'
        } else {
            val c = reader.c
            reader.nextChar()
            return c
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#non-printable-code-point
    private fun isNonPrintable(c: Char): Boolean {
        return c <= '\u0008' || c == '\u000B' || c in '\u000E'..'\u001F' || c == '\u007E'
    }

    // https://www.w3.org/TR/css-syntax-3/#whitespace
    private fun isWhitespace(c: Char): Boolean {
        return c == ' ' || c == '\t'
    }

    // https://www.w3.org/TR/css-syntax-3/#name-code-point
    private fun isName(c: Char): Boolean {
        return isNameStart(c) || isDigit(c) || c == '-'
    }

    // https://www.w3.org/TR/css-syntax-3/#name-start-code-point
    private fun isNameStart(c: Char): Boolean {
        return isLetter(c) || c.toInt() > 128 || c == '_'
    }

    // https://www.w3.org/TR/css-syntax-3/#letter
    private fun isLetter(c: Char): Boolean {
        return c in 'A'..'Z' || c in 'a'..'z'
    }

    // https://www.w3.org/TR/css-syntax-3/#digit
    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun isHexDigit(c: Char): Boolean {
        return isDigit(c) || c in 'A'..'F' || c in 'a'..'f'
    }

    // https://www.w3.org/TR/css-syntax-3/#check-if-two-code-points-are-a-valid-escape
    private fun isEscape(c0: Char, c1: Char): Boolean {
        return c0 == '\\' && c1 != '\n'
    }

    // https://www.w3.org/TR/css-syntax-3/#check-if-three-code-points-would-start-an-identifier
    private fun startsIdentifier(c0: Char, c1: Char, c2: Char): Boolean {
        return if (c0 == '-') {
            isNameStart(c1) || isEscape(c1, c2)
        } else {
            isNameStart(c0) || isEscape(c0, c1)
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#check-if-three-code-points-would-start-a-number
    private fun startsNumber(c0: Char, c1: Char, c2: Char): Boolean {
        return if (c0 == '+' || c0 == '-') {
            isDigit(c1) || c1 == '.' && isDigit(c2)
        } else if (c0 == '.') {
            isDigit(c1)
        } else {
            isDigit(c0)
        }
    }

    // https://www.w3.org/TR/css-syntax-3/#surrogate-code-point
    private fun isSurrogate(c: Char): Boolean {
        return c in '\ud800'..'\udfff'
    }
}