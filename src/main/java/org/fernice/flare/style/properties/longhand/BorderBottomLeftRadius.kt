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
import org.fernice.flare.style.value.specified.BorderCornerRadius
import java.io.Writer
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius

object BorderBottomLeftRadiusId : LonghandId() {

    override val name: String = "border-bottom-left-radius"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderCornerRadius.parse(context, input).map { width -> BorderBottomLeftRadiusDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderBottomLeftRadiusDeclaration -> {
                val radius = declaration.radius.toComputedValue(context)

                context.builder.setBorderBottomLeftRadius(radius)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderBottomLeftRadius()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderBottomLeftRadius()
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

class BorderBottomLeftRadiusDeclaration(val radius: BorderCornerRadius) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderBottomLeftRadiusId
    }

    override fun toCssInternally(writer: Writer) = radius.toCss(writer)


    companion object {

        val initialValue: ComputedBorderCornerRadius by lazy { ComputedBorderCornerRadius.zero() }
    }
}
