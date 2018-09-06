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
class MarginBottomId : LonghandId() {

    override fun name(): String {
        return "margin-bottom"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return LengthOrPercentageOrAuto.parse(context, input).map { width -> MarginBottomDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is MarginBottomDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setMarginBottom(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetMarginBottom()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritMarginBottom()
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

        val instance: MarginBottomId by lazy { MarginBottomId() }
    }
}

class MarginBottomDeclaration(val length: LengthOrPercentageOrAuto) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return MarginBottomId.instance
    }

    companion object {

        val initialValue: ComputedLengthOrPercentageOrAuto by lazy { ComputedLengthOrPercentageOrAuto.zero() }
    }
}