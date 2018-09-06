package de.krall.flare.style.properties.shorthand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.properties.ShorthandId
import de.krall.flare.style.properties.longhand.*
import de.krall.flare.style.value.generic.Rect
import de.krall.flare.style.value.specified.LengthOrPercentageOrAuto
import modern.std.Empty
import modern.std.Err
import modern.std.Ok
import modern.std.Result


@PropertyEntryPoint
class MarginId : ShorthandId() {

    override fun name(): String {
        return "margin"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = Rect.parseWith(context, input, LengthOrPercentageOrAuto.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(MarginTopDeclaration(
                sides.top
        ))
        declarations.add(MarginRightDeclaration(
                sides.right
        ))
        declarations.add(MarginBottomDeclaration(
                sides.bottom
        ))
        declarations.add(MarginLeftDeclaration(
                sides.left
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    MarginTopId.instance,
                    MarginRightId.instance,
                    MarginBottomId.instance,
                    MarginLeftId.instance
            )
        }

        val instance: MarginId by lazy { MarginId() }
    }
}