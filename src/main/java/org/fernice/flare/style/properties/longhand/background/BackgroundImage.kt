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
import org.fernice.std.First
import org.fernice.std.Second
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Image
import org.fernice.flare.style.value.specified.ImageLayer
import org.fernice.std.map
import java.io.Writer
import org.fernice.flare.style.value.computed.ImageLayer as ComputedImageLayer

object BackgroundImageId : AbstractLonghandId<BackgroundImageDeclaration>(
    name = "background-image",
    declarationType = BackgroundImageDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundImageDeclaration, ParseError> {
        return input.parseCommaSeparated { scopedInput -> Image.parse(context, scopedInput).map { Second(it) } }
            .map { BackgroundImageDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundImageDeclaration) {
        val computed = declaration.image.map { layer ->
            when (layer) {
                is First -> First(Unit)
                is Second -> Second(layer.value.toComputedValue(context))
            }
        }

        context.builder.setBackgroundImage(computed)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundImage()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundImage()
    }
}

class BackgroundImageDeclaration(val image: List<ImageLayer>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundImageId)
) {

    override fun toCssInternally(writer: Writer) {
        image.filterIsInstance<Second<Image>>().map { it.value }.toCssJoining(writer, ", ")
    }

    companion object {

        val InitialValue: List<ComputedImageLayer> by lazy { listOf() }
        val InitialSingleValue by lazy { First(Unit) }
    }
}
