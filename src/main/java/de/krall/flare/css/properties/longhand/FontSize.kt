package de.krall.flare.css.properties.longhand

import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.properties.LonghandId
import de.krall.flare.css.properties.PropertyDeclaration
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.specified.FontSize
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result

class FontSizeId : LonghandId() {
    override fun name(): String {
        return "font-size"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontSize.parse(context,input).map(::FontSizeDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
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
}