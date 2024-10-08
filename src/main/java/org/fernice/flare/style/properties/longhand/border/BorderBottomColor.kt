/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.border

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Color
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.Color as ComputedColor

object BorderBottomColorId : AbstractLonghandId<BorderBottomColorDeclaration>(
    name = "border-bottom-color",
    declarationType = BorderBottomColorDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderBottomColorDeclaration, ParseError> {
        return Color.parse(context, input).map { BorderBottomColorDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderBottomColorDeclaration) {
        val color = declaration.color.toComputedValue(context)

        context.builder.setBorderBottomColor(color)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderBottomColor()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderBottomColor()
    }
}

class BorderBottomColorDeclaration(val color: Color) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderBottomColorId),
) {

    override fun toCssInternally(writer: Writer) {
        color.toCss(writer)
    }

    companion object {

        val InitialValue: ComputedColor = ComputedColor.Transparent
    }
}
