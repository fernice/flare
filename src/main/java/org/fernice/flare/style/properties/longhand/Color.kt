/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.ColorPropertyValue
import fernice.std.Result
import org.fernice.flare.style.value.specified.Color as ComputedColor

@PropertyEntryPoint
class ColorId : LonghandId() {

    override fun name(): String {
        return "color"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return ColorPropertyValue.parse(context, input).map(::ColorDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is ColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetColor()
                    }
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritColor()
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

        val instance: ColorId by lazy { ColorId() }
    }
}

class ColorDeclaration(val color: ColorPropertyValue) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return ColorId.instance
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}
