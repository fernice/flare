/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.std.First
import org.fernice.flare.std.Second
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Image
import org.fernice.flare.style.value.specified.ImageLayer
import java.io.Writer
import org.fernice.flare.style.value.computed.ImageLayer as ComputedImageLayer

object BackgroundImageId : LonghandId() {

    override val name: String = "background-image"

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { Image.parse(context, it).map(::Second) }.map { BackgroundImageDeclaration(it) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundImageDeclaration -> {
                val computed = declaration.image.map { layer ->
                    when (layer) {
                        is First -> First(Unit)
                        is Second -> Second(layer.value.toComputedValue(context))
                    }
                }

                context.builder.setBackgroundImage(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundImage()
                    }
                    CssWideKeyword.Inherit -> {
                        context.builder.inheritBackgroundImage()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return false
    }

    override fun toString(): String {
        return "LonghandId::BackgroundImage"
    }
}

class BackgroundImageDeclaration(val image: List<ImageLayer>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundImageId
    }

    override fun toCssInternally(writer: Writer) = image.filter { it is Second }.map { (it as Second).value }.toCssJoining(writer, ", ")

    companion object {

        val initialValue: List<ComputedImageLayer> by lazy { listOf<ComputedImageLayer>() }
        val InitialSingleValue by lazy { First(Unit) }
    }
}
