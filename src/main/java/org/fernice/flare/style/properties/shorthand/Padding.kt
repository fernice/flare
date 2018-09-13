/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.shorthand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.*
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage
import fernice.std.Empty
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result


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
