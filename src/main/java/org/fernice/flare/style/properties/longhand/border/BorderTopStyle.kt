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
import org.fernice.flare.style.value.computed.BorderStyle
import org.fernice.std.map
import java.io.Writer

object BorderTopStyleId : AbstractLonghandId<BorderTopStyleDeclaration>(
    name = "border-top-style",
    declarationType = BorderTopStyleDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderTopStyleDeclaration, ParseError> {
        return BorderStyle.parse(input).map { BorderTopStyleDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderTopStyleDeclaration) {
        context.builder.setBorderTopStyle(declaration.style)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderTopStyle()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderTopStyle()
    }
}

class BorderTopStyleDeclaration(val style: BorderStyle) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderTopStyleId),
) {

    override fun toCssInternally(writer: Writer) = style.toCss(writer)

    companion object {

        val initialValue: BorderStyle by lazy { BorderStyle.None }
    }
}
