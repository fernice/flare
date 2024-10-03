/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.color

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.ColorPropertyValue
import org.fernice.std.map
import java.io.Writer

object ColorId : AbstractLonghandId<ColorDeclaration>(
    name = "color",
    declarationType = ColorDeclaration::class,
    isInherited = true,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<ColorDeclaration, ParseError> {
        return ColorPropertyValue.parse(context, input).map { ColorDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: ColorDeclaration) {
        val color = declaration.color.toComputedValue(context)

        context.builder.setColor(color)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetColor()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritColor()
    }
}

class ColorDeclaration(val color: ColorPropertyValue) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(ColorId),
) {

    override fun toCssInternally(writer: Writer) = color.toCss(writer)

    companion object {

        val InitialValue: RGBA = RGBA(0f, 0f, 0f, 1f)
    }
}
