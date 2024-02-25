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
import org.fernice.flare.style.value.specified.BorderCornerRadius
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius

object BorderTopLeftRadiusId : AbstractLonghandId<BorderTopLeftRadiusDeclaration>(
    name = "border-top-left-radius",
    declarationType = BorderTopLeftRadiusDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderTopLeftRadiusDeclaration, ParseError> {
        return BorderCornerRadius.parse(context, input).map { BorderTopLeftRadiusDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderTopLeftRadiusDeclaration) {
        val radius = declaration.radius.toComputedValue(context)

        context.builder.setBorderTopLeftRadius(radius)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderTopLeftRadius()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderTopLeftRadius()
    }
}

class BorderTopLeftRadiusDeclaration(val radius: BorderCornerRadius) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderTopLeftRadiusId),
) {

    override fun toCssInternally(writer: Writer) = radius.toCss(writer)

    companion object {

        val InitialValue: ComputedBorderCornerRadius by lazy { ComputedBorderCornerRadius.zero() }
    }
}
