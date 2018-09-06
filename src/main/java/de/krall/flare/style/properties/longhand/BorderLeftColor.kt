package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.Color
import modern.std.Result
import de.krall.flare.style.value.computed.Color as ComputedColor

@PropertyEntryPoint
class BorderLeftColorId : LonghandId() {

    override fun name(): String {
        return "border-left-color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BorderLeftColorDeclaration(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderLeftColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setBorderLeftColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderLeftColor()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderLeftColor()
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

        val instance: BorderLeftColorId by lazy { BorderLeftColorId() }
    }
}

class BorderLeftColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderLeftColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}