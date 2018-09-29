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
import org.fernice.flare.style.value.specified.BorderSideWidth
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

object BorderBottomWidthId : LonghandId() {

    override val name: String = "border-bottom-width"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderSideWidth.parse(context, input).map { width -> BorderBottomWidthDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderBottomWidthDeclaration -> {
                val width = declaration.width.toComputedValue(context)

                context.builder.setBorderBottomWidth(width)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderBottomWidth()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderBottomWidth()
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

class BorderBottomWidthDeclaration(val width: BorderSideWidth) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderBottomWidthId
    }

    override fun toCssInternally(writer: Writer) = width.toCss(writer)

    companion object {

        val initialValue: ComputedNonNegativeLength by lazy { ComputedNonNegativeLength.zero() }
    }
}
