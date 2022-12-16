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
import org.fernice.flare.style.value.specified.BorderSideWidth
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

object BorderBottomWidthId : AbstractLonghandId<BorderBottomWidthDeclaration>(
    name = "border-bottom-width",
    declarationType = BorderBottomWidthDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderBottomWidthDeclaration, ParseError> {
        return BorderSideWidth.parse(context, input).map { BorderBottomWidthDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderBottomWidthDeclaration) {
        val width = declaration.width.toComputedValue(context)

        context.builder.setBorderBottomWidth(width)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderBottomWidth()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderBottomWidth()
    }
}

class BorderBottomWidthDeclaration(val width: BorderSideWidth) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderBottomWidthId),
) {

    override fun toCssInternally(writer: Writer) = width.toCss(writer)

    companion object {

        val InitialValue: ComputedNonNegativeLength by lazy { ComputedNonNegativeLength.zero() }
    }
}
