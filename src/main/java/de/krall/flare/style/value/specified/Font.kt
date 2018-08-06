package de.krall.flare.style.value.specified

import de.krall.flare.style.parser.AllowQuirks
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.FontBaseSize
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.*
import de.krall.flare.style.value.computed.NonNegativeLength
import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import de.krall.flare.style.value.computed.FontFamily as ComputedFontFamily
import de.krall.flare.style.value.computed.FontSize as ComputedFontSize

sealed class FontFamily : SpecifiedValue<ComputedFontFamily> {

    data class Values(val values: FontFamilyList) : FontFamily()

    override fun toComputedValue(context: Context): ComputedFontFamily {
        return when (this) {
            is FontFamily.Values -> ComputedFontFamily(values)
        }
    }

    companion object {

        fun parse(input: Parser): Result<FontFamily, ParseError> {
            return input.parseCommaSeparated(SingleFontFamily.Contract::parse)
                    .map { values -> FontFamily.Values(FontFamilyList(values)) }
        }
    }
}

sealed class FontSize : SpecifiedValue<ComputedFontSize> {

    data class Length(val lop: LengthOrPercentage) : FontSize()

    data class Keyword(val keyword: KeywordInfo) : FontSize()

    object Smaller : FontSize()

    object Larger : FontSize()

    fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
        return when (this) {
            is FontSize.Length -> {
                var info: Option<KeywordInfo> = None()
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

                            val abs = calc.toComputedValue(context, FontBaseSize.InheritStyleButStripEmUnits())
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

    internal fun composeKeyword(context: Context, factor: Float): Option<KeywordInfo> {
        return context
                .style()
                .getParentFont()
                .fontSize
                .keywordInfo
                .map { info -> info.compose(factor, Au(0).intoNonNegative()) }
    }

    override fun toComputedValue(context: Context): ComputedFontSize {
        return toComputedValueAgainst(context, FontBaseSize.CurrentStyle())
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontSize, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<FontSize, ParseError> {
            val lopResult = input.tryParse { parser -> LengthOrPercentage.parseNonNegativeQuirky(context, parser, allowQuirks) }

            if (lopResult is Ok) {
                return Ok(FontSize.Length(lopResult.value))
            }

            val keyword = input.tryParse(KeywordSize.Companion::parse)

            if (keyword is Ok) {
                return Ok(FontSize.Keyword(KeywordInfo.from(keyword.value)))
            }

            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier) {
                "smaller" -> Ok(FontSize.Smaller)
                "larger" -> Ok(FontSize.Larger)

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
) : SpecifiedValue<NonNegativeLength> {

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
}

sealed class KeywordSize : SpecifiedValue<NonNegativeLength> {

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

    companion object {

        fun parse(input: Parser): Result<KeywordSize, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "xx-small" -> Ok(KeywordSize.XXSmall)
                "x-small" -> Ok(KeywordSize.XSmall)
                "small" -> Ok(KeywordSize.Small)
                "medium" -> Ok(KeywordSize.Medium)
                "large" -> Ok(KeywordSize.Large)
                "x-large" -> Ok(KeywordSize.XLarge)
                "xx-large" -> Ok(KeywordSize.XXLarge)

                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }

        private const val FONT_MEDIUM_PX = 16
    }
}


