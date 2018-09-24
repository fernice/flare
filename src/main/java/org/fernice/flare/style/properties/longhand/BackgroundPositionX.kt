/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.HorizontalPosition
import org.fernice.flare.style.value.specified.X
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Result
import org.fernice.flare.cssparser.toCssJoining
import java.io.Writer
import org.fernice.flare.style.value.computed.HorizontalPosition as ComputedHorizontalPosition

@PropertyEntryPoint(legacy = false)
object BackgroundPositionXId : LonghandId() {

    override fun name(): String {
        return "background-position-x"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { HorizontalPosition.parseQuirky(context, it, AllowQuirks.Yes, X.Companion) }
            .map(::BackgroundPositionXDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundPositionXDeclaration -> {
                val computed = declaration.position.toComputedValue(context)

                context.builder.setBackgroundPositionX(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundPositionX()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBackgroundPositionX()
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

class BackgroundPositionXDeclaration(val position: List<HorizontalPosition>) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundPositionXId
    }

    override fun toCssInternally(writer: Writer) = position.toCssJoining(writer, ", ")

    companion object {

        val initialValue: List<ComputedHorizontalPosition> by lazy { listOf(ComputedHorizontalPosition.zero()) }
    }
}
