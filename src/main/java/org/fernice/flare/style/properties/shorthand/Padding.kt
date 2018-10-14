/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.shorthand

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.PaddingBottomDeclaration
import org.fernice.flare.style.properties.longhand.PaddingBottomId
import org.fernice.flare.style.properties.longhand.PaddingLeftDeclaration
import org.fernice.flare.style.properties.longhand.PaddingLeftId
import org.fernice.flare.style.properties.longhand.PaddingRightDeclaration
import org.fernice.flare.style.properties.longhand.PaddingRightId
import org.fernice.flare.style.properties.longhand.PaddingTopDeclaration
import org.fernice.flare.style.properties.longhand.PaddingTopId
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage

object PaddingId : ShorthandId() {

    override val name: String = "padding"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = Rect.parseWith(context, input, NonNegativeLengthOrPercentage.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            PaddingTopDeclaration(
                sides.top
            )
        )
        declarations.add(
            PaddingRightDeclaration(
                sides.right
            )
        )
        declarations.add(
            PaddingBottomDeclaration(
                sides.bottom
            )
        )
        declarations.add(
            PaddingLeftDeclaration(
                sides.left
            )
        )

        return Ok()
    }

    override val longhands: List<LonghandId> by lazy {
        listOf(
            PaddingTopId,
            PaddingRightId,
            PaddingBottomId,
            PaddingLeftId
        )
    }
}
