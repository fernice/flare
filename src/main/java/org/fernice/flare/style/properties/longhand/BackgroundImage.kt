/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.Image
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Result
import org.fernice.flare.style.value.computed.Image as ComputedImage

@PropertyEntryPoint
class BackgroundImageId : LonghandId() {

    override fun name(): String {
        return "background-image"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { Image.parse(context, it) }.map { BackgroundImageDeclaration(it) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundImageDeclaration -> {
                val computed = declaration.image.toComputedValue(context)

                context.builder.setBackgroundImage(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundImage()
                    }
                    CssWideKeyword.INHERIT -> {
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

    companion object {

        val instance: BackgroundImageId by lazy { BackgroundImageId() }
    }
}

class BackgroundImageDeclaration(val image: List<Image>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundImageId.instance
    }

    companion object {

        val initialValue: List<ComputedImage> by lazy { listOf<ComputedImage>() }
    }
}
