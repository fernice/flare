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
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Color
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.Color as ComputedColor

object BorderLeftColorId : AbstractLonghandId<BorderLeftColorDeclaration>(
    name = "border-left-color",
    declarationType = BorderLeftColorDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderLeftColorDeclaration, ParseError> {
        return Color.parse(context, input).map { BorderLeftColorDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderLeftColorDeclaration) {
        val color = declaration.color.toComputedValue(context)

        context.builder.setBorderLeftColor(color)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderLeftColor()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderLeftColor()
    }
}

class BorderLeftColorDeclaration(val color: Color) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderLeftColorId),
) {

    override fun toCssInternally(writer: Writer) = color.toCss(writer)

    companion object {

        val InitialValue: ComputedColor = ComputedColor.Transparent
    }
}
