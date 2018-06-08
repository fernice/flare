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
import de.krall.flare.style.value.specified.LengthOrPercentage
import de.krall.flare.style.value.computed.LengthOrPercentage as ComputedLengthOrPercentage

@PropertyEntryPoint
class BorderBottomRightRadiusId : LonghandId() {

    override fun name(): String {
        return "border-bottom-right-radius"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return LengthOrPercentage.parse(context, input).map { width -> BorderBottomRightRadiusDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderBottomRightRadiusDeclaration -> {
                val radius = declaration.radius.toComputedValue(context)

                context.builder.setBorderBottomRightRadius(radius)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderBottomRightRadius()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderBottomRightRadius()
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

        val instance: BorderBottomRightRadiusId by lazy { BorderBottomRightRadiusId() }
    }
}

class BorderBottomRightRadiusDeclaration(val radius: LengthOrPercentage) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderBottomRightRadiusId.instance
    }

    companion object {

        val initialValue: ComputedLengthOrPercentage by lazy { ComputedLengthOrPercentage.zero() }
    }
}