package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.BorderSideWidth
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

@PropertyEntryPoint
class BorderRightWidthId : LonghandId() {

    override fun name(): String {
        return "border-right-width"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderSideWidth.parse(context, input).map { width -> BorderRightWidthDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderRightWidthDeclaration -> {
                val width = declaration.width.toComputedValue(context)

                context.builder.setBorderRightWidth(width)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderRightWidth()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderRightWidth()
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

        val instance: BorderRightWidthId by lazy { BorderRightWidthId() }
    }
}

class BorderRightWidthDeclaration(val width: BorderSideWidth) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderRightWidthId.instance
    }

    companion object {

        val initialValue: ComputedNonNegativeLength by lazy { ComputedNonNegativeLength.zero() }
    }
}