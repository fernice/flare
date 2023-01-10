/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.font

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.computed.FontFamilyList
import org.fernice.flare.style.value.computed.SingleFontFamily
import org.fernice.flare.style.value.specified.FontFamily
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.FontFamily as ComputedFontFamily

object FontFamilyId : AbstractLonghandId<FontFamilyDeclaration>(
    name = "font-family",
    declarationType = FontFamilyDeclaration::class,
    isInherited = true,
    isEarlyProperty = true,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<FontFamilyDeclaration, ParseError> {
        return FontFamily.parse(input).map { FontFamilyDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: FontFamilyDeclaration) {
        val fontFamily = declaration.fontFamily.toComputedValue(context)

        context.builder.setFontFamily(fontFamily)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetFontFamily()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritFontFamily()
    }
}

class FontFamilyDeclaration(val fontFamily: FontFamily) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(FontFamilyId),
) {

    override fun toCssInternally(writer: Writer) = fontFamily.toCss(writer)

    companion object {

        val InitialValue: ComputedFontFamily by lazy {
            ComputedFontFamily(
                FontFamilyList(
                    listOf(
                        SingleFontFamily.Generic("serif")
                    )
                )
            )
        }
    }
}
