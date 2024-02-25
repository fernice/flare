/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.margin

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.LengthOrPercentageOrAuto
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto

object MarginTopId : AbstractLonghandId<MarginTopDeclaration>(
    name = "margin-top",
    declarationType = MarginTopDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<MarginTopDeclaration, ParseError> {
        return LengthOrPercentageOrAuto.parse(context, input).map { MarginTopDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: MarginTopDeclaration) {
        val length = declaration.length.toComputedValue(context)

        context.builder.setMarginTop(length)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetMarginTop()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritMarginTop()
    }
}

class MarginTopDeclaration(val length: LengthOrPercentageOrAuto) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(MarginTopId),
) {

    override fun toCssInternally(writer: Writer) = length.toCss(writer)

    companion object {

        val InitialValue: ComputedLengthOrPercentageOrAuto by lazy { ComputedLengthOrPercentageOrAuto.zero() }
    }
}
