package de.krall.flare.css.value.specified

import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.SpecifiedValue
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.cssparser.Color as ParserColor
import de.krall.flare.css.value.computed.Color as ComputedColor

sealed class Color : SpecifiedValue<ComputedColor> {

    class RGBA(val rgba: de.krall.flare.cssparser.RGBA, keyword: Option<String>) : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.RGBA(rgba)
        }
    }

    class CurrentColor : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.CurrentColor()
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Color, ParseError> {
            val state = input.state()
            val keyword = input.expectIdentifier().ok()
            input.reset(state)

            val colorResult = ParserColor.parse(input)

            val color = when (colorResult) {
                is Ok -> colorResult.value
                is Err -> return colorResult
            }

            return when (color) {
                is ParserColor.RGBA -> Ok(Color.RGBA(color.rgba, keyword))
                is ParserColor.CurrentColor -> Ok(Color.CurrentColor())
            }
        }
    }
}

