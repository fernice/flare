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
import org.fernice.flare.style.value.specified.BorderSideWidth
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength

object BorderLeftWidthId : AbstractLonghandId<BorderLeftWidthDeclaration>(
    name = "border-left-width",
    declarationType = BorderLeftWidthDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BorderLeftWidthDeclaration, ParseError> {
        return BorderSideWidth.parse(context, input).map { BorderLeftWidthDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BorderLeftWidthDeclaration) {
        val width = declaration.width.toComputedValue(context)

        context.builder.setBorderLeftWidth(width)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBorderLeftWidth()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBorderLeftWidth()
    }
}

class BorderLeftWidthDeclaration(val width: BorderSideWidth) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BorderLeftWidthId),
) {

    override fun toCssInternally(writer: Writer) = width.toCss(writer)

    companion object {

        val InitialValue: ComputedNonNegativeLength by lazy { ComputedNonNegativeLength.zero() }
    }
}
