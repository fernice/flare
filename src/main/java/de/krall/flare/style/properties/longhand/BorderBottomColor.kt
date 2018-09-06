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
class BorderBottomColorId : LonghandId() {

    override fun name(): String {
        return "border-bottom-color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BorderBottomColorDeclaration(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderBottomColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setBorderBottomColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderBottomColor()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderBottomColor()
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

        val instance: BorderBottomColorId by lazy { BorderBottomColorId() }
    }
}

class BorderBottomColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderBottomColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}