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
import org.fernice.flare.style.value.specified.FontWeight
import java.io.Writer
import org.fernice.flare.style.value.computed.FontWeight as ComputedFontWeight

object FontWeightId : LonghandId() {

    override val name: String = "font-weight"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return FontWeight.parse(context, input).map(::FontWeightDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FontWeightDeclaration -> {
                val fontWeight = declaration.fontWeight.toComputedValue(context)

                context.builder.setFontWeight(fontWeight)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Initial -> {
                        context.builder.resetFontWeight()
                    }
                    CssWideKeyword.Unset,
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritFontWeight()
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

class FontWeightDeclaration(val fontWeight: FontWeight) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return FontWeightId
    }

    override fun toCssInternally(writer: Writer) {

    }

    companion object {

        val InitialValue: ComputedFontWeight by lazy {
            ComputedFontWeight.Normal
        }
    }
}