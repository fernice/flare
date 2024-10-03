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
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.style.value.specified.FontSize
import org.fernice.flare.style.value.specified.KeywordInfo
import org.fernice.flare.style.value.specified.KeywordSize
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.FontSize as ComputedFontSize

object FontSizeId : AbstractLonghandId<FontSizeDeclaration>(
    name = "font-size",
    declarationType = FontSizeDeclaration::class,
    isInherited = true,
    isEarlyProperty = true,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<FontSizeDeclaration, ParseError> {
        return FontSize.parse(context, input).map { FontSizeDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: FontSizeDeclaration) {
        val fontSize = declaration.fontSize.toComputedValue(context)

        context.builder.setFontSize(fontSize)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetFontSize()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritFontSize()
    }
}

class FontSizeDeclaration(val fontSize: FontSize) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(FontSizeId),
) {

    override fun toCssInternally(writer: Writer) = fontSize.toCss(writer)

    companion object {

        val InitialValue: ComputedFontSize by lazy {
            ComputedFontSize(
                NonNegativeLength.new(16f),
                KeywordInfo(
                    KeywordSize.Medium,
                    1f,
                    NonNegativeLength.new(0f)
                )
            )
        }
    }
}
