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
import org.fernice.flare.style.value.computed.Style
import fernice.std.Result
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

@PropertyEntryPoint
class BorderBottomStyleId : LonghandId() {

    override fun name(): String {
        return "border-Bottom-style"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Style.parse(input).map { style -> BorderBottomStyleDeclaration(style) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderBottomStyleDeclaration -> {
                context.builder.setBorderBottomStyle(declaration.style)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBorderBottomStyle()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBorderBottomStyle()
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

        val instance: BorderBottomStyleId by lazy { BorderBottomStyleId() }
    }
}

class BorderBottomStyleDeclaration(val style: Style) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderBottomStyleId.instance
    }

    companion object {

        val initialValue: Style by lazy { Style.NONE }
    }
}
