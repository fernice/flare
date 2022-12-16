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
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Fill
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.Fill as ComputedFill

object FillId : AbstractLonghandId<FillDeclaration>(
    name = "fill",
    declarationType = FillDeclaration::class,
    isInherited = true,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<FillDeclaration, ParseError> {
        return Fill.parse(input).map { FillDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: FillDeclaration) {
        val fill = declaration.fill.toComputedValue(context)

        context.builder.setFill(fill)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetFill()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritFill()
    }
}

class FillDeclaration(val fill: Fill) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(FillId),
) {

    override fun toCssInternally(writer: Writer) {}

    companion object {

        val InitialValue by lazy { ComputedFill.None }
    }
}