/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import kotlin.String
import kotlin.String as Str

sealed class Token {

    data class Comment(val text: Str) : Token()

    data class String(val value: Str) : Token()

    data class BadString(val value: Str) : Token()

    data class Url(val url: Str) : Token()
    data class UnquotedUrl(val url: Str) : Token()

    data class BadUrl(val url: Str) : Token()

    data class Hash(val value: Str) : Token()

    data class IdHash(val value: Str) : Token()

    data class Identifier(val name: Str) : Token()

    data class Function(val name: Str) : Token()

    data class AtKeyword(val name: Str) : Token()

    data class Number(val number: org.fernice.flare.cssparser.Number) : Token()

    data class Dimension(val number: org.fernice.flare.cssparser.Number, val unit: Str) : Token()

    data class Percentage(val number: org.fernice.flare.cssparser.Number) : Token()

    data class UnicodeRange(val start: Int, val end: Int) : Token()

    // Matches

    data object SuffixMatch : Token()

    data object SubstringMatch : Token()

    data object PrefixMatch : Token()

    data object DashMatch : Token()

    data object IncludeMatch : Token()

    // Delimiters

    data class Delimiter(val char: Char) : Token()

    data object Whitespace : Token()

    data object Asterisk : Token()

    data object Minus : Token()

    data object Plus : Token()

    data object Dot : Token()

    data object Colon : Token()

    data object SemiColon : Token()

    data object Solidus : Token()

    data object Pipe : Token()

    data object Tidle : Token()

    data object Comma : Token()

    data object Gt : Token()

    data object Lt : Token()

    data object Equal : Token()

    data object Bang : Token()

    data object Ampersand : Token()

    data object LParen : Token()

    data object RParen : Token()

    data object LBrace : Token()

    data object RBrace : Token()

    data object LBracket : Token()

    data object RBracket : Token()

    data object CDC : Token()

    data object CDO : Token()

    data object Column : Token()

    final override fun toString(): kotlin.String {
        return when (this) {
            is Token.Delimiter -> "Token::Delimiter($char)"
            is Token.Comment -> "Token::Comment($text)"
            is Token.Url -> "Token::Url($url)"
            is Token.BadUrl -> "Token::BadUrl($url)"
            is Token.Hash -> "Token::Hash($value)"
            is Token.IdHash -> "Token::IdHash($value)"
            is Token.Identifier -> "Token::Identifier($name)"
            is Token.Function -> "Token::Function($name)"
            is Token.AtKeyword -> "Token::AtKeyword($name)"
            is Token.Number -> "Token::Number($number)"
            is Token.Dimension -> "Token::Dimension($number)"
            is Token.Percentage -> "Token::Percentage($number)"
            is Token.UnicodeRange -> "Token::UnicodeRange($start, $end)"
            else -> "Token::${javaClass.simpleName}"
        }
    }
}

/**
 * CSS specification conform representation of a parse number.
 */
data class Number(
    val type: Str,
    val text: Str,
    val value: Double,
    val negative: Boolean,
) {

    fun int(): Int {
        return value.toInt()
    }

    fun float(): Float {
        return value.toFloat()
    }
}

/**
 * Type for describing the essence of which a logical block is made of in CSS.
 */
sealed class BlockType {

    /**
     * Block consisting out of opening ('(') and a closing parenthesis (')')
     */
    data object Parenthesis : BlockType()

    /**
     * Block consisting out of opening ('[') and a closing bracket (']')
     */
    data object Bracket : BlockType()

    /**
     * Block consisting out of opening ('{') and a closing brace ('}')
     */
    data object Brace : BlockType()

    val opening: String
        get() = when (this) {
            Parenthesis -> "("
            Bracket -> "["
            Brace -> "{"
        }

    val closing: String
        get() = when (this) {
            Parenthesis -> ")"
            Bracket -> "]"
            Brace -> "}"
        }

    override fun toString(): String = "$opening...$closing"

    companion object {

        /**
         * Returns the matching [BlockType] for the specified [token] if it opens a block, otherwise returns [None].
         */
        fun opening(token: Token): BlockType? {
            return when (token) {
                is Token.LParen -> Parenthesis
                is Token.LBracket -> Bracket
                is Token.LBrace -> Brace
                is Token.Function -> Parenthesis
                else -> null
            }
        }

        /**
         * Returns the matching [BlockType] for the specified [token] if it closes a block, otherwise returns [None].
         */
        fun closing(token: Token): BlockType? {
            return when (token) {
                is Token.RParen -> Parenthesis
                is Token.RBracket -> Bracket
                is Token.RBrace -> Brace
                else -> null
            }
        }
    }
}

/**
 * Bit mask that identifies certain token as delimiters for a [Parser]. Such Parser may be limited in its capability
 * to fully parse a token stream in such way, that it is only able to parse a nested block before reaching a virtual end of
 * file. The delimiters of the Parser define at which token it may halt.
 */
class Delimiters private constructor(val bits: Int) {

    infix fun or(delimiters: Delimiters): Delimiters {
        return Delimiters(bits or delimiters.bits)
    }

    companion object {

        val None = Delimiters(0)

        val LeftBrace = Delimiters(1 shl 1)
        val SemiColon = Delimiters(1 shl 2)
        val Bang = Delimiters(1 shl 3)
        val Comma = Delimiters(1 shl 4)

        val RightParenthesis = Delimiters(1 shl 5)
        val RightBrace = Delimiters(1 shl 6)
        val RightBracket = Delimiters(1 shl 7)

        fun from(token: Token): Delimiters {
            return when (token) {
                is Token.LBrace -> LeftBrace
                is Token.SemiColon -> SemiColon
                is Token.Bang -> Bang
                is Token.Comma -> Comma

                is Token.RParen -> RightParenthesis
                is Token.RBrace -> RightBrace
                is Token.RBracket -> RightBracket

                else -> None
            }
        }

        fun from(blockType: BlockType): Delimiters {
            return when (blockType) {
                BlockType.Parenthesis -> RightParenthesis
                BlockType.Brace -> RightBrace
                BlockType.Bracket -> RightBracket
            }
        }
    }
}
