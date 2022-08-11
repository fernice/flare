/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.margin

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.LengthOrPercentageOrAuto
import java.io.Writer
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto

object MarginTopId : LonghandId() {

    override val name: String = "margin-top"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return LengthOrPercentageOrAuto.parse(context, input).map { width -> MarginTopDeclaration(width) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is MarginTopDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setMarginTop(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetMarginTop()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritMarginTop()
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

class MarginTopDeclaration(val length: LengthOrPercentageOrAuto) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return MarginTopId
    }

    override fun toCssInternally(writer: Writer) = length.toCss(writer)

    companion object {

        val initialValue: ComputedLengthOrPercentageOrAuto by lazy { ComputedLengthOrPercentageOrAuto.zero() }
    }
}
