package de.krall.flare.css.properties.longhand

import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.properties.CssWideKeyword
import de.krall.flare.css.properties.LonghandId
import de.krall.flare.css.properties.PropertyDeclaration
import de.krall.flare.css.properties.PropertyEntryPoint
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.specified.Color
import de.krall.flare.css.value.computed.Color as ComputedColor
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result

@PropertyEntryPoint
class BackgroundColorId : LonghandId() {

    override fun name(): String {
        return "background-color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BackgroundColorDeclaration(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setBackgroundColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundColor()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundColor()
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

        val instance: BackgroundColorId by lazy { BackgroundColorId() }
    }
}

class BackgroundColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}