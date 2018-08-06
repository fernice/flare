package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result
import de.krall.flare.style.parser.AllowQuirks
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.BackgroundRepeat
import de.krall.flare.style.value.specified.HorizontalPosition
import de.krall.flare.style.value.specified.X
import de.krall.flare.style.value.toComputedValue
import de.krall.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat

@PropertyEntryPoint
class BackgroundRepeatId : LonghandId() {

    override fun name(): String {
        return "background-repeat"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { HorizontalPosition.parseQuirky(context, it, AllowQuirks.Yes(), X.Companion) }
                .map(::BackgroundPositionXDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundRepeatDeclaration -> {
                val computed = declaration.repeat.toComputedValue(context)

                context.builder.setBackgroundRepeat(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundRepeat()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundRepeat()
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

        val instance: BackgroundRepeatId by lazy { BackgroundRepeatId() }
    }
}

class BackgroundRepeatDeclaration(val repeat: List<BackgroundRepeat>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundRepeatId.instance
    }

    companion object {

        val initialValue: List<ComputedBackgroundRepeat> by lazy { listOf(ComputedBackgroundRepeat.repeat()) }
    }
}