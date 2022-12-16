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
import org.fernice.flare.style.value.specified.BorderCornerRadius
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.BorderCornerRadius as ComputedBorderCornerRadius

object BorderTopRightRadiusId : AbstractLonghandId<BorderTopRightRadiusDeclaration>(
    name = "border-top-right-radius",
    declarationType = BorderTopRightRadiusDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderTopRightRadiusDeclaration, ParseError> {
        return BorderCornerRadius.parse(context, input).map { BorderTopRightRadiusDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderTopRightRadiusDeclaration) {
        val radius = declaration.radius.toComputedValue(context)

        context.builder.setBorderTopRightRadius(radius)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderTopRightRadius()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderTopRightRadius()
    }
}

class BorderTopRightRadiusDeclaration(val radius: BorderCornerRadius) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderTopRightRadiusId),
) {

    override fun toCssInternally(writer: Writer) = radius.toCss(writer)

    companion object {

        val InitialValue: ComputedBorderCornerRadius by lazy { ComputedBorderCornerRadius.zero() }
    }
}
