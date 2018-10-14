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
import org.fernice.flare.style.properties.longhand.MarginBottomDeclaration
import org.fernice.flare.style.properties.longhand.MarginBottomId
import org.fernice.flare.style.properties.longhand.MarginLeftDeclaration
import org.fernice.flare.style.properties.longhand.MarginLeftId
import org.fernice.flare.style.properties.longhand.MarginRightDeclaration
import org.fernice.flare.style.properties.longhand.MarginRightId
import org.fernice.flare.style.properties.longhand.MarginTopDeclaration
import org.fernice.flare.style.properties.longhand.MarginTopId
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.LengthOrPercentageOrAuto

object MarginId : ShorthandId() {

    override val name: String = "margin"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = Rect.parseWith(context, input, LengthOrPercentageOrAuto.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            MarginTopDeclaration(
                sides.top
            )
        )
        declarations.add(
            MarginRightDeclaration(
                sides.right
            )
        )
        declarations.add(
            MarginBottomDeclaration(
                sides.bottom
            )
        )
        declarations.add(
            MarginLeftDeclaration(
                sides.left
            )
        )

        return Ok()
    }

    override val longhands: List<LonghandId> by lazy {
        listOf(
            MarginTopId,
            MarginRightId,
            MarginBottomId,
            MarginLeftId
        )
    }
}
