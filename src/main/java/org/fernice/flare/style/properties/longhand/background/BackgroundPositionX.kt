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
import org.fernice.flare.style.AllowQuirks
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.HorizontalPosition
import org.fernice.flare.style.value.specified.X
import org.fernice.flare.style.value.toComputedValue
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.HorizontalPosition as ComputedHorizontalPosition

object BackgroundPositionXId : AbstractLonghandId<BackgroundPositionXDeclaration>(
    name = "background-position-x",
    declarationType = BackgroundPositionXDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundPositionXDeclaration, ParseError> {
        return input.parseCommaSeparated { HorizontalPosition.parseQuirky(context, it, AllowQuirks.Yes, X.Companion) }
            .map { BackgroundPositionXDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundPositionXDeclaration) {
        val computed = declaration.position.toComputedValue(context)

        context.builder.setBackgroundPositionX(computed)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundPositionX()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundPositionX()
    }
}

class BackgroundPositionXDeclaration(val position: List<HorizontalPosition>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundPositionXId),
) {

    override fun toCssInternally(writer: Writer) = position.toCssJoining(writer, ", ")

    companion object {

        val InitialValue: List<ComputedHorizontalPosition> by lazy { listOf(ComputedHorizontalPosition.zero()) }
    }
}
