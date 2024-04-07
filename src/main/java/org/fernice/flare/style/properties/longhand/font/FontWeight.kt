/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.longhand.font

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.FontWeight
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.FontWeight as ComputedFontWeight

object FontWeightId : AbstractLonghandId<FontWeightDeclaration>(
    name = "font-weight",
    declarationType = FontWeightDeclaration::class,
    isInherited = true,
    isEarlyProperty = true,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<FontWeightDeclaration, ParseError> {
        return FontWeight.parse(context, input).map { FontWeightDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: FontWeightDeclaration) {
        val fontWeight = declaration.fontWeight.toComputedValue(context)

        context.builder.setFontWeight(fontWeight)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetFontWeight()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritFontWeight()
    }
}

class FontWeightDeclaration(val fontWeight: FontWeight) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(FontWeightId),
) {

    override fun toCssInternally(writer: Writer) {
    }

    companion object {

        val InitialValue: ComputedFontWeight by lazy {
            ComputedFontWeight.Normal
        }
    }
}