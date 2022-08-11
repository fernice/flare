/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.longhand.color

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Fill
import java.io.Writer
import org.fernice.flare.style.value.computed.Fill as ComputedFill

object FillId : LonghandId() {

    override val name: String = "fill"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Fill.parse(input).map(::FillDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is FillDeclaration -> {
                val fill = declaration.fill.toComputedValue(context)

                context.builder.setFill(fill)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Initial -> {
                        context.builder.resetFill()
                    }
                    CssWideKeyword.Unset,
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritFill()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return false
    }
}

class FillDeclaration(val fill: Fill) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return FillId
    }

    override fun toCssInternally(writer: Writer) {}

    companion object {

        val InitialValue by lazy { ComputedFill.None }
    }
}