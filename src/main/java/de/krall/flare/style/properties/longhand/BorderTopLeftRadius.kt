package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.BorderCornerRadius
import modern.std.Result
import de.krall.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius

@PropertyEntryPoint
class BorderTopLeftRadiusId : LonghandId() {

    override fun name(): String {
        return "border-top-left-radius"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderCornerRadius.parse(context, input).map { width -> BorderTopLeftRadiusDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderTopLeftRadiusDeclaration -> {
                val radius = declaration.radius.toComputedValue(context)

                context.builder.setBorderTopLeftRadius(radius)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderTopLeftRadius()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderTopLeftRadius()
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

        val instance: BorderTopLeftRadiusId by lazy { BorderTopLeftRadiusId() }
    }
}

class BorderTopLeftRadiusDeclaration(val radius: BorderCornerRadius) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderTopLeftRadiusId.instance
    }

    companion object {

        val initialValue: ComputedBorderCornerRadius by lazy { ComputedBorderCornerRadius.zero() }
    }
}