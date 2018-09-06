package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.ColorPropertyValue
import modern.std.Result
import de.krall.flare.style.value.specified.Color as ComputedColor

@PropertyEntryPoint
class ColorId : LonghandId() {

    override fun name(): String {
        return "color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return ColorPropertyValue.parse(context, input).map(::ColorDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is ColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetColor()
                    }
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritColor()
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

        val instance: ColorId by lazy { ColorId() }
    }
}

class ColorDeclaration(val color: ColorPropertyValue) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return ColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}