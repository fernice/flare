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
import de.krall.flare.style.value.specified.BackgroundSize
import de.krall.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize
import de.krall.flare.style.value.toComputedValue

@PropertyEntryPoint
class BackgroundSizeId : LonghandId() {

    override fun name(): String {
        return "background-size"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { BackgroundSize.parse(context, it) }
                .map(::BackgroundSizeDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundSizeDeclaration -> {
                val computed = declaration.size.toComputedValue(context)

                context.builder.setBackgroundSize(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundSize()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundSize()
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

        val instance: BackgroundSizeId by lazy { BackgroundSizeId() }
    }
}

class BackgroundSizeDeclaration(val size: List<BackgroundSize>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundSizeId.instance
    }

    companion object {

        val initialValue: List<ComputedBackgroundSize> by lazy { listOf(ComputedBackgroundSize.auto()) }
    }
}