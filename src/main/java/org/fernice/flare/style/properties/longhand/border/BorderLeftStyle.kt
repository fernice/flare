/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.border

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.computed.Style
import java.io.Writer

object BorderLeftStyleId : LonghandId() {

    override val name: String = "border-left-style"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Style.parse(input).map { style -> BorderLeftStyleDeclaration(style) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderLeftStyleDeclaration -> {
                context.builder.setBorderLeftStyle(declaration.style)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderLeftStyle()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderLeftStyle()
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

class BorderLeftStyleDeclaration(val style: Style) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderLeftStyleId
    }

    override fun toCssInternally(writer: Writer) = style.toCss(writer)

    companion object {

        val initialValue: Style by lazy { Style.None }
    }
}
