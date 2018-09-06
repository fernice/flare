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
class MarginRightId : LonghandId() {

    override fun name(): String {
        return "margin-right"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return LengthOrPercentageOrAuto.parse(context, input).map { width -> MarginRightDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is MarginRightDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setMarginRight(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetMarginRight()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritMarginRight()
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

        val instance: MarginRightId by lazy { MarginRightId() }
    }
}

class MarginRightDeclaration(val length: LengthOrPercentageOrAuto) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return MarginRightId.instance
    }

    companion object {

        val initialValue: ComputedLengthOrPercentageOrAuto by lazy { ComputedLengthOrPercentageOrAuto.zero() }
    }
}