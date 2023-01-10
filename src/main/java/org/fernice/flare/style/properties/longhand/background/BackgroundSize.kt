/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.background

import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.BackgroundSize
import org.fernice.flare.style.value.toComputedValue
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize

object BackgroundSizeId : AbstractLonghandId<BackgroundSizeDeclaration>(
    name = "background-size",
    declarationType = BackgroundSizeDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundSizeDeclaration, ParseError> {
        return input.parseCommaSeparated { scopedInput -> BackgroundSize.parse(context, scopedInput) }
            .map { BackgroundSizeDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundSizeDeclaration) {
        val computed = declaration.size.toComputedValue(context)

        context.builder.setBackgroundSize(computed)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundSize()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundSize()
    }
}

class BackgroundSizeDeclaration(val size: List<BackgroundSize>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundSizeId),
) {

    override fun toCssInternally(writer: Writer) {
        size.toCssJoining(writer, ", ")
    }

    companion object {

        val InitialValue: List<ComputedBackgroundSize> by lazy { listOf(ComputedBackgroundSize.auto()) }
    }
}
