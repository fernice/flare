/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.background

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.BackgroundRepeat
import org.fernice.flare.style.value.toComputedValue
import org.fernice.std.Result
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat

object BackgroundRepeatId : AbstractLonghandId<BackgroundRepeatDeclaration>(
    name = "background-repeat",
    declarationType = BackgroundRepeatDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundRepeatDeclaration, ParseError> {
        return input.parseCommaSeparated { scopedInput -> BackgroundRepeat.parse(context, scopedInput) }
            .map { BackgroundRepeatDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundRepeatDeclaration) {
        val computed = declaration.repeat.toComputedValue(context)

        context.builder.setBackgroundRepeat(computed)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundRepeat()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundRepeat()
    }
}

class BackgroundRepeatDeclaration(val repeat: List<BackgroundRepeat>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundRepeatId)
) {

    override fun toCssInternally(writer: Writer) {
        repeat.toCssJoining(writer, ", ")
    }

    companion object {

        val InitialValue: List<ComputedBackgroundRepeat> by lazy { listOf(ComputedBackgroundRepeat.repeat()) }
        val InitialSingleValue: BackgroundRepeat by lazy { BackgroundRepeat.Repeat }

    }
}
