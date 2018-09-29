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

object BorderLeftColorId : LonghandId() {

    override val name: String = "border-left-color"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Color.parse(context, input).map { color -> BorderLeftColorDeclaration(color) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderLeftColorDeclaration -> {
                val color = declaration.color.toComputedValue(context)

                context.builder.setBorderLeftColor(color)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderLeftColor()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderLeftColor()
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

class BorderLeftColorDeclaration(val color: Color) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderLeftColorId
    }

    override fun toCssInternally(writer: Writer) = color.toCss(writer)

    companion object {

        val initialValue: ComputedColor by lazy { ComputedColor.transparent() }
    }
}
