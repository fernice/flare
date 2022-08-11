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
import org.fernice.flare.style.value.specified.BorderCornerRadius
import java.io.Writer
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius

object BorderTopLeftRadiusId : LonghandId() {

    override val name: String = "border-top-left-radius"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return BorderCornerRadius.parse(context, input).map { width -> BorderTopLeftRadiusDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BorderTopLeftRadiusDeclaration -> {
                val radius = declaration.radius.toComputedValue(context)

                context.builder.setBorderTopLeftRadius(radius)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBorderTopLeftRadius()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBorderTopLeftRadius()
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

class BorderTopLeftRadiusDeclaration(val radius: BorderCornerRadius) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return BorderTopLeftRadiusId
    }

    override fun toCssInternally(writer: Writer) = radius.toCss(writer)

    companion object {

        val initialValue: ComputedBorderCornerRadius by lazy { ComputedBorderCornerRadius.zero() }
    }
}
