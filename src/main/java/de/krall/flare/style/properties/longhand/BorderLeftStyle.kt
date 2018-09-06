package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.computed.Style
import modern.std.Result
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

@PropertyEntryPoint
class BorderLeftStyleId : LonghandId() {

    override fun name(): String {
        return "border-Left-style"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Style.parse(input).map { style -> BorderLeftStyleDeclaration(style) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderLeftStyleDeclaration -> {
                context.builder.setBorderLeftStyle(declaration.style)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderLeftStyle()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderLeftStyle()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return false
    }

    companion object {

        val instance: BorderLeftStyleId by lazy { BorderLeftStyleId() }
    }
}

class BorderLeftStyleDeclaration(val style: Style) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderLeftStyleId.instance
    }

    companion object {

        val initialValue: Style by lazy { Style.NONE }
    }
}