package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.computed.FontFamilyList
import de.krall.flare.style.value.computed.SingleFontFamily
import de.krall.flare.style.value.specified.FontFamily
import modern.std.Result
import de.krall.flare.style.value.computed.FontFamily as ComputedFontFamily

@PropertyEntryPoint
class FontFamilyId : LonghandId() {
    override fun name(): String {
        return "font-family"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontFamily.parse(input).map(::FontFamilyDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FontFamilyDeclaration -> {
                val fontFamily = declaration.fontFamily.toComputedValue(context)

                context.builder.setFontFamily(fontFamily)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetFontFamily()
                    }
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritFontFamily()
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

        val instance: FontFamilyId by lazy { FontFamilyId() }
    }
}

class FontFamilyDeclaration(val fontFamily: FontFamily) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return FontFamilyId.instance
    }

    companion object {

        val initialValue: ComputedFontFamily by lazy {
            ComputedFontFamily(FontFamilyList(listOf(
                    SingleFontFamily.Generic("serif")
            )))
        }
    }
}