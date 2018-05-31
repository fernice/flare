package de.krall.flare.cssparser

import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import kotlin.String as Str

sealed class Token {

    class Delimiter(val char: Char) : Token()

    class Whitespace : Token()

    class Comment(val text: Str) : Token()

    class String(val value: Str) : Token()

    class BadString(val value: Str) : Token()

    class Url(val url: Str) : Token()

    class BadUrl(val url: Str) : Token()

    class Hash(val value: Str) : Token()

    class IdHash(val value: Str) : Token()

    class Identifier(val name: Str) : Token()

    class Function(val name: Str) : Token()

    class AtKeyword(val name: Str) : Token()

    class Number(val number: de.krall.flare.cssparser.Number) : Token()

    class Dimension(val number: de.krall.flare.cssparser.Number, val unit: Str) : Token()

    class Percentage(val number: de.krall.flare.cssparser.Number) : Token()

    class UnicodeRange(val start: Int, val end: Int) : Token()

    // Matches

    class SuffixMatch : Token()

    class SubstringMatch : Token()

    class PrefixMatch : Token()

    class DashMatch : Token()

    class IncludeMatch : Token()

    // Delimiters

    class Asterisk : Token()

    class Minus : Token()

    class Plus : Token()

    class Dot : Token()

    class Colon : Token()

    class SemiColon : Token()

    class Solidus : Token()

    class Pipe : Token()

    class Tidle : Token()

    class Comma : Token()

    class Gt : Token()

    class Lt : Token()

    class Equal : Token()

    class Bang : Token()

    class LParen : Token()

    class RParen : Token()

    class LBrace : Token()

    class RBrace : Token()

    class LBracket : Token()

    class RBracket : Token()

    class CDC : Token()

    class CDO : Token()

    class Column : Token()
}

class Number(val type: Str,
             val text: Str,
             val value: Double,
             val negative: Boolean) {

    fun int(): Int {
        return value.toInt()
    }

    fun float(): Float {
        return value.toFloat()
    }
}

enum class BlockType {

    PARENTHESIS,

    BRACKET,

    BRACE;

    companion object {

        fun opening(token: Token): Option<BlockType> {
            return when (token) {
                is Token.LParen -> Some(PARENTHESIS)
                is Token.LBracket -> Some(BRACKET)
                is Token.LBrace -> Some(BRACE)
                is Token.Function -> Some(PARENTHESIS)
                else -> None()
            }
        }

        fun closing(token: Token): Option<BlockType> {
            return when (token) {
                is Token.RParen -> Some(PARENTHESIS)
                is Token.RBracket -> Some(BRACKET)
                is Token.RBrace -> Some(BRACE)
                else -> None()
            }
        }
    }
}

class Delimiters private constructor(val bits: Int) {

    infix fun or(delimiters: Delimiters): Delimiters {
        return Delimiters(bits or delimiters.bits)
    }

    companion object {

        val None: Delimiters by lazy { Delimiters(0) }

        val LeftBrace: Delimiters by lazy { Delimiters(1 shl 1) }
        val SemiColon: Delimiters by lazy { Delimiters(1 shl 2) }
        val Bang: Delimiters by lazy { Delimiters(1 shl 3) }
        val Comma: Delimiters by lazy { Delimiters(1 shl 4) }

        val RightParenthesis: Delimiters by lazy { Delimiters(1 shl 5) }
        val RightBrace: Delimiters by lazy { Delimiters(1 shl 6) }
        val RightBracket: Delimiters by lazy { Delimiters(1 shl 7) }

        fun from(token: Token): Delimiters {
            return when (token) {
                is Token.LBrace -> LeftBrace
                is Token.SemiColon -> SemiColon
                is Token.Bang -> Bang

                is Token.RParen -> RightParenthesis
                is Token.RBrace -> RightBrace
                is Token.RBracket -> RightBracket

                else -> None
            }
        }

        fun from(blockType: BlockType): Delimiters {
            return when (blockType) {
                BlockType.PARENTHESIS -> RightParenthesis
                BlockType.BRACE -> RightBrace
                BlockType.BRACKET -> RightBracket
            }
        }
    }
}