package de.krall.flare.css.value.specified

import de.krall.flare.css.parser.AllowQuirks
import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.FontBaseSize
import de.krall.flare.css.value.SpecifiedValue
import de.krall.flare.css.value.computed.*
import de.krall.flare.css.value.computed.NonNegativeLength
import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import de.krall.flare.css.value.computed.FontFamily as ComputedFontFamily
import de.krall.flare.css.value.computed.FontSize as ComputedFontSize

sealed class FontFamily : SpecifiedValue<ComputedFontFamily> {

    companion object {

        fun parse(input: Parser): Result<FontFamily, ParseError> {
            return input.parseCommaSeparated(SingleFontFamily.Contract::parse)
                    .map { values -> FontFamily.Values(FontFamilyList(values)) }
        }
    }

    class Values(val values: FontFamilyList) : FontFamily() {
        override fun toComputedValue(context: Context): ComputedFontFamily {
            return ComputedFontFamily(values)
        }
    }
}

sealed class FontSize : SpecifiedValue<ComputedFontSize> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<FontSize, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<FontSize, ParseError> {
            val lopResult = input.tryParse { input -> LengthOrPercentage.parseNonNegativeQuirky(context, input, allowQuirks) }

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
                "smaller" -> Ok(FontSize.Smaller())
                "larger" -> Ok(FontSize.Larger())

                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }

        private const val LARGE_FONT_SIZE_RATION = 1.2f
    }

    override fun toComputedValue(context: Context): ComputedFontSize {
        return toComputedValueAgainst(context, FontBaseSize.CurrentStyle())
    }

    abstract fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize

    class Length(val lop: LengthOrPercentage) : FontSize() {
        override fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
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
                    val parent = context.style().getParentFont().getFontSize()

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

            return ComputedFontSize(
                    size,
                    info
            )
        }
    }

    class Keyword(val keyword: KeywordInfo) : FontSize() {
        override fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
            return ComputedFontSize(
                    keyword.toComputedValue(context),
                    Some(keyword)
            )
        }
    }

    class Smaller : FontSize() {
        override fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
            return ComputedFontSize(
                    FontRelativeLength.Em(1f / LARGE_FONT_SIZE_RATION)
                            .toComputedValue(context, baseSize)
                            .intoNonNegative(),
                    composeKeyword(context, 1f / LARGE_FONT_SIZE_RATION)
            )
        }
    }

    class Larger : FontSize() {
        override fun toComputedValueAgainst(context: Context, baseSize: FontBaseSize): ComputedFontSize {
            return ComputedFontSize(
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
                .getFontSize()
                .keywordInfo
                .map { info -> info.compose(factor, Au(0).intoNonNegative()) }
    }
}

class KeywordInfo(val keyword: KeywordSize,
                  val factor: Float,
                  val offset: NonNegativeLength) : SpecifiedValue<NonNegativeLength> {

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

    companion object {

        fun parse(input: Parser): Result<KeywordSize, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "xx-small" -> Ok(KeywordSize.XXSmall())
                "x-small" -> Ok(KeywordSize.XSmall())
                "small" -> Ok(KeywordSize.Small())
                "medium" -> Ok(KeywordSize.Medium())
                "large" -> Ok(KeywordSize.Large())
                "x-large" -> Ok(KeywordSize.XLarge())
                "xx-large" -> Ok(KeywordSize.XXLarge())

                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }

        private const val FONT_MEDIUM_PX = 16
    }

    class XXSmall : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 3 / 5).intoNonNegative()
        }
    }

    class XSmall : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 3 / 4).intoNonNegative()
        }
    }

    class Small : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 8 / 9).intoNonNegative()
        }
    }

    class Medium : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX)).intoNonNegative()
        }
    }

    class Large : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 6 / 5).intoNonNegative()
        }
    }

    class XLarge : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 3 / 2).intoNonNegative()
        }
    }

    class XXLarge : KeywordSize() {
        override fun toComputedValue(context: Context): NonNegativeLength {
            return (Au.fromPx(FONT_MEDIUM_PX) * 2).intoNonNegative()
        }
    }
}


