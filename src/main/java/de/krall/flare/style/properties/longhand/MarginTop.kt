package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.LengthOrPercentageOrAuto
import modern.std.Result
import de.krall.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto

@PropertyEntryPoint
class MarginTopId : LonghandId() {

    override fun name(): String {
        return "margin-top"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return LengthOrPercentageOrAuto.parse(context, input).map { width -> MarginTopDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is MarginTopDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setMarginTop(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetMarginTop()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritMarginTop()
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

        val instance: MarginTopId by lazy { MarginTopId() }
    }
}

class MarginTopDeclaration(val length: LengthOrPercentageOrAuto) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return MarginTopId.instance
    }

    companion object {

        val initialValue: ComputedLengthOrPercentageOrAuto by lazy { ComputedLengthOrPercentageOrAuto.zero() }
    }
}