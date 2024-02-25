/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.background

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.AllowQuirks
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.VerticalPosition
import org.fernice.flare.style.value.specified.Y
import org.fernice.flare.style.value.toComputedValue
import org.fernice.std.Result
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.VerticalPosition as ComputedVerticalPosition

object BackgroundPositionYId : AbstractLonghandId<BackgroundPositionYDeclaration>(
    name = "background-position-y",
    declarationType = BackgroundPositionYDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundPositionYDeclaration, ParseError> {
        return input.parseCommaSeparated { scopedInput -> VerticalPosition.parseQuirky(context, scopedInput, AllowQuirks.Yes, Y.Companion) }
            .map { BackgroundPositionYDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundPositionYDeclaration) {
        val computed = declaration.position.toComputedValue(context)

        context.builder.setBackgroundPositionY(computed)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundPositionY()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundPositionY()
    }
}

class BackgroundPositionYDeclaration(val position: List<VerticalPosition>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundPositionYId),
) {

    override fun toCssInternally(writer: Writer) {
        position.toCssJoining(writer, ", ")
    }

    companion object {

        val InitialValue: List<ComputedVerticalPosition> by lazy { listOf(ComputedVerticalPosition.zero()) }
    }
}
