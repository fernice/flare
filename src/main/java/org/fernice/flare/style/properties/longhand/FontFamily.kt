/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.computed.FontFamilyList
import org.fernice.flare.style.value.computed.SingleFontFamily
import org.fernice.flare.style.value.specified.FontFamily
import fernice.std.Result
import java.io.Writer
import org.fernice.flare.style.value.computed.FontFamily as ComputedFontFamily

@PropertyEntryPoint(legacy = false)
object FontFamilyId : LonghandId() {
    override fun name(): String {
        return "font-family"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontFamily.parse(input).map(::FontFamilyDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FontFamilyDeclaration -> {
                val fontFamily = declaration.fontFamily.toComputedValue(context)

                context.builder.setFontFamily(fontFamily)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Initial -> {
                        context.builder.resetFontFamily()
                    }
                    CssWideKeyword.Unset,
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritFontFamily()
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

class FontFamilyDeclaration(val fontFamily: FontFamily) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return FontFamilyId
    }

    override fun toCssInternally(writer: Writer) = fontFamily.toCss(writer)

    companion object {

        val initialValue: ComputedFontFamily by lazy {
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
