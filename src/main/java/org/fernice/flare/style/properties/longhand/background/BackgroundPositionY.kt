/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.background

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.VerticalPosition
import org.fernice.flare.style.value.specified.Y
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Result
import org.fernice.flare.cssparser.toCssJoining
import java.io.Writer
import org.fernice.flare.style.value.computed.VerticalPosition as ComputedVerticalPosition

object BackgroundPositionYId : LonghandId() {

    override val name: String = "background-position-y"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { VerticalPosition.parseQuirky(context, it, AllowQuirks.Yes, Y.Companion) }
            .map(::BackgroundPositionYDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundPositionYDeclaration -> {
                val computed = declaration.position.toComputedValue(context)

                context.builder.setBackgroundPositionY(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundPositionY()
                    }
                    CssWideKeyword.Inherit -> {
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
}

class BackgroundPositionYDeclaration(val position: List<VerticalPosition>) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundPositionYId
    }

    override fun toCssInternally(writer: Writer) {
        position.toCssJoining(writer, ", ")
    }

    companion object {

        val initialValue: List<ComputedVerticalPosition> by lazy { listOf(ComputedVerticalPosition.zero()) }
    }
}
