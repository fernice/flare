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
import org.fernice.flare.style.value.specified.BackgroundRepeat
import org.fernice.flare.style.value.specified.HorizontalPosition
import org.fernice.flare.style.value.specified.X
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Result
import org.fernice.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat

@PropertyEntryPoint
class BackgroundRepeatId : LonghandId() {

    override fun name(): String {
        return "background-repeat"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { HorizontalPosition.parseQuirky(context, it, AllowQuirks.Yes, X.Companion) }
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
