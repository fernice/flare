/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import fernice.std.Some
import fernice.std.mapOr
import fernice.std.unwrap
import fernice.std.unwrapOr
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.std.max
import org.fernice.flare.std.min
import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.Parse
import org.fernice.flare.style.parser.ParseQuirky
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.FontBaseSize
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.style.value.computed.FontFamilyList
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.flare.style.value.computed.SingleFontFamily
import org.fernice.flare.style.value.computed.intoNonNegative
import java.io.Writer
import org.fernice.flare.style.value.computed.FontFamily as ComputedFontFamily
import org.fernice.flare.style.value.computed.FontSize as ComputedFontSize
import org.fernice.flare.style.value.computed.FontWeight as ComputedFontWeight

sealed class FontWeight : SpecifiedValue<ComputedFontWeight> {

    data class Absolute(val value: AbsoluteFontWeight) : FontWeight()

    object Bolder : FontWeight()

    object Lighter : FontWeight()

    final override fun toComputedValue(context: Context): ComputedFontWeight {
        return when (this) {
            is Absolute -> value.toComputedValue(context)
            is Bolder -> context.builder.getParentFont().fontWeight.lighter()
            is Lighter -> context.builder.getParentFont().fontWeight.bolder()
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontWeight, ParseError> {
            when (val result = input.tryParse { parser -> AbsoluteFontWeight.parse(context, parser) }) {
                is Ok -> return Ok(Absolute(result.value))
            }

            val location = input.sourceLocation()
            val ident = when (val result = input.expectIdentifier()) {
                is Ok -> result.value
                is Err -> return result
            }

            return Ok(
                when (ident.lowercase()) {
                    "bolder" -> Bolder
                    "lighter" -> Lighter
                    else -> return Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
                }
            )
        }
    }
}

private const val MIN_FONT_WEIGHT = 1f
private const val MAX_FONT_WEIGHT = 1000f

sealed class AbsoluteFontWeight : SpecifiedValue<ComputedFontWeight> {

    data class Weight(val value: Number) : AbsoluteFontWeight()

    object Normal : AbsoluteFontWeight()

    object Bold : AbsoluteFontWeight()

    override fun toComputedValue(context: Context): ComputedFontWeight {
        return when (this) {
            is Weight -> ComputedFontWeight(value.value.max(MIN_FONT_WEIGHT).min(MAX_FONT_WEIGHT))
            is Normal -> ComputedFontWeight.Normal
            is Bold -> ComputedFontWeight.Bold
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<AbsoluteFontWeight, ParseError> {
            when (val result = input.tryParse { parser -> Number.parse(context, parser) }) {
                is Ok -> {
                    val number = result.value

                    if (!number.wasCalc() && (number.value < MIN_FONT_WEIGHT || number.value > MAX_FONT_WEIGHT)) {
                        return Err(input.newError(ParseErrorKind.Unspecified))
                    }
                    return Ok(Weight(number))
                }
            }

            val location = input.sourceLocation()
            val ident = when (val result = input.expectIdentifier()) {
                is Ok -> result.value
                is Err -> return result
            }

            return Ok(
                when (ident.lowercase()) {
                    "normal" -> Normal
                    "bold" -> Bold
                    else -> return Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
                }
            )
        }
    }
}

sealed class FontFamily : SpecifiedValue<ComputedFontFamily>, ToCss {

    data class Values(val values: FontFamilyList) : FontFamily()

    override fun toComputedValue(context: Context): ComputedFontFamily {
        return when (this) {
            is Values -> ComputedFontFamily(values)
        }
    }

    override fun toCss(writer: Writer) {
        when (this) {
            is Values -> values.toCssJoining(writer, ", ")
        }
    }

    companion object {

        fun parse(input: Parser): Result<FontFamily, ParseError> {
            return input.parseCommaSeparated(SingleFontFamily.Contract::parse)
                .map { values -> Values(FontFamilyList(values)) }
        }
    }
}

sealed class FontSize : SpecifiedValue<ComputedFontSize>, ToCss {

    data class Length(val lop: LengthOrPercentage) : FontSize()

    data class Keyword(val keyword: KeywordInfo) : FontSize()

    object Smaller : FontSize()

    object Larger : FontSize()

    fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
        fun composeKeyword(context: Context, factor: Float): KeywordInfo? {
            return context
                .style()
                .getParentFont()
                .fontSize
                .keywordInfo
                ?.compose(factor, Au(0).intoNonNegative())
        }

        return when (this) {
            is Length -> {
                var info: KeywordInfo? = null

                val size = when (lop) {
                    is LengthOrPercentage.Length -> {
                        when (val length = lop.length) {
                            is NoCalcLength.FontRelative -> {
                                if (length.length is FontRelativeLength.Em) {
                                    info = composeKeyword(context, length.length.value)
                                }

                                length.length.toComputedValue(context, baseSize).intoNonNegative()
                            }
                            else -> length.toComputedValue(context).intoNonNegative()
                        }
                    }
                    is LengthOrPercentage.Percentage -> {
                        info = composeKeyword(context, lop.percentage.value)
                        baseSize.resolve(context)
                            .scaleBy(lop.percentage.value)
                            .intoNonNegative()
                    }
                    is LengthOrPercentage.Calc -> {
                        val calc = lop.calc
                        val parent = context.style().getParentFont().fontSize

                        if (calc.em.isSome() || calc.percentage.isSome() && parent.keywordInfo != null) {
                            val ratio = calc.em.unwrapOr(0f) + calc.percentage.mapOr({ p -> p.value }, 0f)

                            val abs = calc.toComputedValue(context, FontBaseSize.InheritStyleButStripEmUnits)
                                .lengthComponent()
                                .intoNonNegative()

                            info = parent.keywordInfo?.compose(ratio, abs)
                        }

                        val computed = calc.toComputedValue(context, baseSize)
                        computed.toUsedValue(Some(baseSize.resolve(context)))
                            .unwrap()
                            .intoNonNegative()
                    }
                }

                ComputedFontSize(
                    size,
                    info
                )
            }
            is Keyword -> {
                ComputedFontSize(
                    keyword.toComputedValue(context),
                    keyword
                )
            }
            is Smaller -> {
                ComputedFontSize(
                    FontRelativeLength.Em(1f / LARGE_FONT_SIZE_RATION)
                        .toComputedValue(context, baseSize)
                        .intoNonNegative(),
                    composeKeyword(context, 1f / LARGE_FONT_SIZE_RATION)
                )
            }
            is Larger -> ComputedFontSize(
                FontRelativeLength.Em(LARGE_FONT_SIZE_RATION)
                    .toComputedValue(context, baseSize)
                    .intoNonNegative(),
                composeKeyword(context, LARGE_FONT_SIZE_RATION)
            )
        }
    }

    final override fun toComputedValue(context: Context): ComputedFontSize {
        return toComputedValueAgainst(context, FontBaseSize.CurrentStyle)
    }

    final override fun toCss(writer: Writer) {
        when (this) {
            is Length -> lop.toCss(writer)
            is Keyword -> keyword.toCss(writer)
            is Larger -> writer.append("larger")
            is Smaller -> writer.append("smaller")
        }
    }

    companion object : Parse<FontSize>, ParseQuirky<FontSize> {

        override fun parse(context: ParserContext, input: Parser): Result<FontSize, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        override fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<FontSize, ParseError> {
            val lopResult = input.tryParse { parser -> LengthOrPercentage.parseNonNegativeQuirky(context, parser, allowQuirks) }

            if (lopResult is Ok) {
                return Ok(Length(lopResult.value))
            }

            val keyword = input.tryParse(KeywordSize.Companion::parse)

            if (keyword is Ok) {
                return Ok(Keyword(KeywordInfo.from(keyword.value)))
            }

            val location = input.sourceLocation()

            val identifier = when (val identifier = input.expectIdentifier()) {
                is Ok -> identifier.value
                is Err -> return identifier
            }

            return when (identifier) {
                "smaller" -> Ok(Smaller)
                "larger" -> Ok(Larger)

                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }

        private const val LARGE_FONT_SIZE_RATION = 1.2f
    }
}

data class KeywordInfo(
    val keyword: KeywordSize,
    val factor: Float,
    val offset: NonNegativeLength
) : SpecifiedValue<NonNegativeLength>, ToCss {

    companion object {

        fun from(keyword: KeywordSize): KeywordInfo {
            return KeywordInfo(
                keyword,
                1f,
                NonNegativeLength(PixelLength(0f))
            )
        }
    }

    fun compose(factor: Float, offset: NonNegativeLength): KeywordInfo {
        return KeywordInfo(
            keyword,
            this.factor * factor,
            this.offset + offset
        )
    }

    override fun toComputedValue(context: Context): NonNegativeLength {
        val base = keyword.toComputedValue(context)
        return base.scaleBy(factor) + offset
    }

    override fun toCss(writer: Writer) = keyword.toCss(writer)
}

sealed class KeywordSize : SpecifiedValue<NonNegativeLength>, ToCss {

    object XXSmall : KeywordSize()

    object XSmall : KeywordSize()

    object Small : KeywordSize()

    object Medium : KeywordSize()

    object Large : KeywordSize()

    object XLarge : KeywordSize()

    object XXLarge : KeywordSize()

    final override fun toComputedValue(context: Context): NonNegativeLength {
        return when (this) {
            is XXSmall -> (Au.fromPx(FONT_MEDIUM_PX) * 3 / 5).intoNonNegative()
            is XSmall -> (Au.fromPx(FONT_MEDIUM_PX) * 3 / 4).intoNonNegative()
            is Small -> (Au.fromPx(FONT_MEDIUM_PX) * 8 / 9).intoNonNegative()
            is Medium -> (Au.fromPx(FONT_MEDIUM_PX)).intoNonNegative()
            is Large -> (Au.fromPx(FONT_MEDIUM_PX) * 6 / 5).intoNonNegative()
            is XLarge -> (Au.fromPx(FONT_MEDIUM_PX) * 3 / 2).intoNonNegative()
            is XXLarge -> (Au.fromPx(FONT_MEDIUM_PX) * 2).intoNonNegative()
        }
    }

    final override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is XXSmall -> "xx-small"
                is XSmall -> "x-small"
                is Small -> "small"
                is Medium -> "medium"
                is Large -> "large"
                is XLarge -> "x-large"
                is XXLarge -> "xx-large"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<KeywordSize, ParseError> {
            val location = input.sourceLocation()

            val identifier = when (val identifier = input.expectIdentifier()) {
                is Ok -> identifier.value
                is Err -> return identifier
            }

            return when (identifier.lowercase()) {
                "xx-small" -> Ok(XXSmall)
                "x-small" -> Ok(XSmall)
                "small" -> Ok(Small)
                "medium" -> Ok(Medium)
                "large" -> Ok(Large)
                "x-large" -> Ok(XLarge)
                "xx-large" -> Ok(XXLarge)

                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }

        private const val FONT_MEDIUM_PX = 16
    }
}


