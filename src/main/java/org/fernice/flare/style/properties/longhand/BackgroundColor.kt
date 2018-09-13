/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Color
import org.fernice.flare.style.value.computed.Color as ComputedColor
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import fernice.std.Result

@PropertyEntryPoint
class BackgroundColorId : LonghandId() {

    override fun name(): String {
        return "background-color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BackgroundColorDeclaration(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setBackgroundColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundColor()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundColor()
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

        val instance: BackgroundColorId by lazy { BackgroundColorId() }
    }
}

class BackgroundColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}
