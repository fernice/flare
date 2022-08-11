/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.padding

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage

object PaddingBottomId : LonghandId() {

    override val name: String = "padding-bottom"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return NonNegativeLengthOrPercentage.parse(context, input).map { width ->
            PaddingBottomDeclaration(
                width
            )
        }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is PaddingBottomDeclaration -> {
                val length = declaration.length.toComputedValue(context)

                context.builder.setPaddingBottom(length)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetPaddingBottom()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritPaddingBottom()
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

class PaddingBottomDeclaration(val length: NonNegativeLengthOrPercentage) : PropertyDeclaration() {
    override fun id(): LonghandId {
        return PaddingBottomId
    }

    override fun toCssInternally(writer: Writer) = length.toCss(writer)

    companion object {

        val initialValue: ComputedNonNegativeLengthOrPercentage by lazy { ComputedNonNegativeLengthOrPercentage.zero() }
    }
}
