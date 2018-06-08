package de.krall.flare.style.properties.shorthand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.properties.ShorthandId
import de.krall.flare.style.properties.longhand.*
import de.krall.flare.style.value.generic.Rect
import de.krall.flare.style.value.specified.NonNegativeLengthOrPercentage


@PropertyEntryPoint
class PaddingId : ShorthandId() {

    override fun name(): String {
        return "padding"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = Rect.parseWith(context, input, NonNegativeLengthOrPercentage.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(PaddingTopDeclaration(
                sides.top
        ))
        declarations.add(PaddingRightDeclaration(
                sides.right
        ))
        declarations.add(PaddingBottomDeclaration(
                sides.bottom
        ))
        declarations.add(PaddingLeftDeclaration(
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
                    PaddingTopId.instance,
                    PaddingRightId.instance,
                    PaddingBottomId.instance,
                    PaddingLeftId.instance
            )
        }

        val instance: PaddingId by lazy { PaddingId() }
    }
}