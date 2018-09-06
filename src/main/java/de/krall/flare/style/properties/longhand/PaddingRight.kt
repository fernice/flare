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
class PaddingRightId : LonghandId() {

    override fun name(): String {
        return "padding-right"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return NonNegativeLengthOrPercentage.parse(context, input).map { width -> PaddingRightDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is PaddingRightDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setPaddingRight(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetPaddingRight()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritPaddingRight()
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

        val instance: PaddingRightId by lazy { PaddingRightId() }
    }
}

class PaddingRightDeclaration(val length: NonNegativeLengthOrPercentage) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return PaddingRightId.instance
    }

    companion object {

        val initialValue: ComputedNonNegativeLengthOrPercentage by lazy { ComputedNonNegativeLengthOrPercentage.zero() }
    }
}