package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.NonNegativeLengthOrPercentage
import modern.std.Result
import de.krall.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage

@PropertyEntryPoint
class PaddingLeftId : LonghandId() {

    override fun name(): String {
        return "padding-left"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return NonNegativeLengthOrPercentage.parse(context, input).map { width -> PaddingLeftDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is PaddingLeftDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setPaddingLeft(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetPaddingLeft()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritPaddingLeft()
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

        val instance: PaddingLeftId by lazy { PaddingLeftId() }
    }
}

class PaddingLeftDeclaration(val length: NonNegativeLengthOrPercentage) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return PaddingLeftId.instance
    }

    companion object {

        val initialValue: ComputedNonNegativeLengthOrPercentage by lazy { ComputedNonNegativeLengthOrPercentage.zero() }
    }
}