/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Color
import java.io.Writer
import org.fernice.flare.style.value.computed.Color as ComputedColor

object BackgroundColorId : LonghandId() {

    override val name: String = "background-color"

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
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundColor()
                    }
                    CssWideKeyword.Inherit -> {
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
}

class BackgroundColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BackgroundColorId
    }

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }

    override fun toCssInternally(writer: Writer) = color.toCss(writer)
}
