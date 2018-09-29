/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.FontBaseSize
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.*
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.cssparser.*
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.map
import fernice.std.mapOr
import fernice.std.unwrap
import fernice.std.unwrapOr
import java.io.Writer
import org.fernice.flare.style.value.computed.FontFamily as ComputedFontFamily
import org.fernice.flare.style.value.computed.FontSize as ComputedFontSize

sealed class FontFamily : SpecifiedValue<ComputedFontFamily>, ToCss {

    data class Values(val values: FontFamilyList) : FontFamily()

    override fun toComputedValue(context: Context): ComputedFontFamily {
        return when (this) {
            is FontFamily.Values -> ComputedFontFamily(values)
        }
    }

    override fun toCss(writer: Writer) {
        when (this) {
            is FontFamily.Values -> values.toCssJoining(writer, ", ")
        }
    }

    companion object {

        fun parse(input: Parser): Result<FontFamily, ParseError> {
            return input.parseCommaSeparated(SingleFontFamily.Contract::parse)
                .map { values -> FontFamily.Values(FontFamilyList(values)) }
        }
    }
}

sealed class FontSize : SpecifiedValue<ComputedFontSize>, ToCss {

    data class Length(val lop: LengthOrPercentage) : FontSize()

    data class Keyword(val keyword: KeywordInfo) : FontSize()

    object Smaller : FontSize()

    object Larger : FontSize()

    fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
        return when (this) {
            is FontSize.Length -> {
                var info: Option<KeywordInfo> = None
                val size = when (lop) {
                    is LengthOrPercentage.Length -> {
                        val length = lop.length

                        when (length) {
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

                        if (calc.em.isSome() || calc.percentage.isSome() && parent.keywordInfo.isSome()) {
                            val ratio = calc.em.unwrapOr(0f) + calc.percentage.mapOr({ p -> p.value }, 0f)

                            val abs = calc.toComputedValue(context, FontBaseSize.InheritStyleButStripEmUnits)
                                .lengthComponent()
                                .intoNonNegative()

                            info = parent.keywordInfo.map { i -> i.compose(ratio, abs) }
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
            is FontSize.Keyword -> {
                ComputedFontSize(
                    keyword.toComputedValue(context),
                    Some(keyword)
                )
            }
            is FontSize.Smaller -> {
                ComputedFontSize(
                    FontRelativeLength.Em(1f / LARGE_FONT_SIZE_RATION)
                        .toComputedValue(context, baseSize)
                        .intoNonNegative(),
                    composeKeyword(context, 1f / LARGE_FONT_SIZE_RATION)
                )
            }
            is FontSize.Larger -> ComputedFontSize(
                FontRelativeLength.Em(LARGE_FONT_SIZE_RATION)
                    .toComputedValue(context, baseSize)
                    .intoNonNegative(),
                composeKeyword(context, LARGE_FONT_SIZE_RATION)
            )
        }
    }

    private fun composeKeyword(context: Context, factor: Float): Option<KeywordInfo> {
        return context
            .style()
            .getParentFont()
            .fontSize
            .keywordInfo
            .map { info -> info.compose(factor, Au(0).intoNonNegative()) }
    }

    final override fun toComputedValue(context: Context): ComputedFontSize {
        return toComputedValueAgainst(context, FontBaseSize.CurrentStyle)
    }

    final override fun toCss(writer: Writer) {
        when (this) {
            is FontSize.Length -> lop.toCss(writer)
            is FontSize.Keyword -> keyword.toCss(writer)
            is FontSize.Larger -> writer.append("larger")
            is FontSize.Smaller -> writer.append("smaller")
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontSize, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<FontSize, ParseError> {
            val lopResult = input.tryParse { parser -> LengthOrPercentage.parseNonNegativeQuirky(context, parser, allowQuirks) }

            if (lopResult is Ok) {
                return Ok(Length(lopResult.value))
            }

            val keyword = input.tryParse(KeywordSize.Companion::parse)

            if (keyword is Ok) {
                return Ok(Keyword(KeywordInfo.from(keyword.value)))
            }

            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
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
                is KeywordSize.XXSmall -> "xx-small"
                is KeywordSize.XSmall -> "x-small"
                is KeywordSize.Small -> "small"
                is KeywordSize.Medium -> "medium"
                is KeywordSize.Large -> "large"
                is KeywordSize.XLarge -> "x-large"
                is KeywordSize.XXLarge -> "xx-large"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<KeywordSize, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
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


