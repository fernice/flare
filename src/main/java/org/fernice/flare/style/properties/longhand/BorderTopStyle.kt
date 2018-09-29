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
import org.fernice.flare.style.value.computed.Style
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

object BorderTopStyleId : LonghandId() {

    override val name: String = "border-top-style"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Style.parse(input).map { style -> BorderTopStyleDeclaration(style) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderTopStyleDeclaration -> {
                context.builder.setBorderTopStyle(declaration.style)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderTopStyle()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderTopStyle()
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

class BorderTopStyleDeclaration(val style: Style) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderTopStyleId
    }

    override fun toCssInternally(writer: Writer) = style.toCss(writer)

    companion object {

        val initialValue: Style by lazy { Style.None }
    }
}
