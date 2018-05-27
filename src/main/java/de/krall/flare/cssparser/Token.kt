package de.krall.flare.cssparser

import kotlin.String as Str
import de.krall.flare.cssparser.Number as ParsedNumber

sealed class Token {

    class EoF : Token()

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

    class Number(val number: ParsedNumber) : Token()

    class Dimension(val number: ParsedNumber, val unit: Str) : Token()

    class Percentage(val number: ParsedNumber) : Token()

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

    class Solidus: Token()

    class Pipe : Token()

    class Tidle : Token()

    class Comma : Token()

    class Gt : Token()

    class Lt : Token()

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