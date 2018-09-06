package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.BorderSideWidth
import modern.std.Result
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

@PropertyEntryPoint
class BorderLeftWidthId : LonghandId() {

    override fun name(): String {
        return "border-left-width"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderSideWidth.parse(context, input).map { width -> BorderLeftWidthDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderLeftWidthDeclaration -> {
                val width = declaration.width.toComputedValue(context)

                context.builder.setBorderLeftWidth(width)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderLeftWidth()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderLeftWidth()
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

        val instance: BorderLeftWidthId by lazy { BorderLeftWidthId() }
    }
}

class BorderLeftWidthDeclaration(val width: BorderSideWidth) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderLeftWidthId.instance
    }

    companion object {

        val initialValue: ComputedNonNegativeLength by lazy { ComputedNonNegativeLength.zero() }
    }
}