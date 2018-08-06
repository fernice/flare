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
import de.krall.flare.style.value.specified.VerticalPosition
import de.krall.flare.style.value.specified.Y
import de.krall.flare.style.value.toComputedValue
import de.krall.flare.style.value.computed.VerticalPosition as ComputedVerticalPosition

@PropertyEntryPoint
class BackgroundPositionYId : LonghandId() {

    override fun name(): String {
        return "background-position-y"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { VerticalPosition.parseQuirky(context, it, AllowQuirks.Yes(), Y.Companion) }
                .map(::BackgroundPositionYDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundPositionXDeclaration -> {
                val computed = declaration.position.toComputedValue(context)

                context.builder.setBackgroundPositionY(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundPositionY()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundPositionY()
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

        val instance: BackgroundPositionYId by lazy { BackgroundPositionYId() }
    }
}

class BackgroundPositionYDeclaration(val position: List<VerticalPosition>) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundPositionYId.instance
    }

    companion object {

        val initialValue: List<ComputedVerticalPosition> by lazy { listOf(ComputedVerticalPosition.zero()) }
    }
}