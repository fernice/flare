package de.krall.flare.css.properties.longhand

import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.properties.CssWideKeyword
import de.krall.flare.css.properties.LonghandId
import de.krall.flare.css.properties.PropertyDeclaration
import de.krall.flare.css.properties.PropertyEntryPoint
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.computed.NonNegativeLength
import de.krall.flare.css.value.specified.FontSize
import de.krall.flare.css.value.specified.KeywordInfo
import de.krall.flare.css.value.specified.KeywordSize
import de.krall.flare.css.value.computed.FontSize as ComputedFontSize
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result
import de.krall.flare.std.Some

@PropertyEntryPoint
class FontSizeId : LonghandId() {
    override fun name(): String {
        return "font-size"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontSize.parse(context, input).map(::FontSizeDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FontSizeDeclaration -> {
                val fontSize = declaration.fontSize.toComputedValue(context)

                context.builder.setFontSize(fontSize)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetFontSize()
                    }
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritFontSize()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return true
    }

    companion object {

        val instance: FontSizeId by lazy { FontSizeId() }
    }
}

class FontSizeDeclaration(val fontSize: FontSize) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return FontSizeId.instance
    }

    companion object {

        val initialValue: ComputedFontSize by lazy {
            ComputedFontSize(
                    NonNegativeLength.new(16f),
                    Some(KeywordInfo(
                            KeywordSize.Medium(),
                            1f,
                            NonNegativeLength.new(0f)
                    ))
            )
        }
    }
}