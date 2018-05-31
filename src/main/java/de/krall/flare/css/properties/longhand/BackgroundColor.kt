package de.krall.flare.css.properties.longhand

import de.krall.flare.css.ParserContext
import de.krall.flare.css.properties.LonghandId
import de.krall.flare.css.properties.PropertyDeclaration
import de.krall.flare.css.properties.PropertyEntryPoint
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.specified.Color
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result

@PropertyEntryPoint
class BackgroundColorId : LonghandId() {

    override fun name(): String {
        return "background-color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BackgroundColor(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
    }

    companion object {

        val instance: BackgroundColorId by lazy { BackgroundColorId() }
    }
}

class BackgroundColor(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundColorId.instance
    }
}