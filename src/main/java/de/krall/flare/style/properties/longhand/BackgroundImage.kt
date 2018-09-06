package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.specified.Image
import de.krall.flare.style.value.toComputedValue
import modern.std.Result
import de.krall.flare.style.value.computed.Image as ComputedImage

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