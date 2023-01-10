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
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage

object PaddingRightId : AbstractLonghandId<PaddingRightDeclaration>(
    name = "padding-right",
    declarationType = PaddingRightDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<PaddingRightDeclaration, ParseError> {
        return NonNegativeLengthOrPercentage.parse(context, input).map { PaddingRightDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: PaddingRightDeclaration) {
        val length = declaration.length.toComputedValue(context)

        context.builder.setPaddingRight(length)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetPaddingRight()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritPaddingRight()
    }
}

class PaddingRightDeclaration(val length: NonNegativeLengthOrPercentage) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(PaddingRightId),
) {

    override fun toCssInternally(writer: Writer) = length.toCss(writer)

    companion object {

        val InitialValue: ComputedNonNegativeLengthOrPercentage by lazy { ComputedNonNegativeLengthOrPercentage.zero() }
    }
}
