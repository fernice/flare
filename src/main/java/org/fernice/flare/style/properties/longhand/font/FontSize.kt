/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.font

import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.style.value.specified.FontSize
import org.fernice.flare.style.value.specified.KeywordInfo
import org.fernice.flare.style.value.specified.KeywordSize
import java.io.Writer
import org.fernice.flare.style.value.computed.FontSize as ComputedFontSize

object FontSizeId : LonghandId() {

    override val name: String = "font-size"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontSize.parse(context, input).map(::FontSizeDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FontSizeDeclaration -> {
                val fontSize = declaration.fontSize.toComputedValue(context)

                context.builder.setFontSize(fontSize)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Initial -> {
                        context.builder.resetFontSize()
                    }
                    CssWideKeyword.Unset,
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritFontSize()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return true
    }
}

class FontSizeDeclaration(val fontSize: FontSize) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return FontSizeId
    }

    override fun toCssInternally(writer: Writer) = fontSize.toCss(writer)

    companion object {

        val initialValue: ComputedFontSize by lazy {
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
