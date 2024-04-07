/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.shorthand.margin

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.margin.MarginBottomDeclaration
import org.fernice.flare.style.properties.longhand.margin.MarginBottomId
import org.fernice.flare.style.properties.longhand.margin.MarginLeftDeclaration
import org.fernice.flare.style.properties.longhand.margin.MarginLeftId
import org.fernice.flare.style.properties.longhand.margin.MarginRightDeclaration
import org.fernice.flare.style.properties.longhand.margin.MarginRightId
import org.fernice.flare.style.properties.longhand.margin.MarginTopDeclaration
import org.fernice.flare.style.properties.longhand.margin.MarginTopId
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.LengthOrPercentageOrAuto

object MarginId : ShorthandId(
    name = "margin",
    longhands = listOf(
        MarginTopId,
        MarginRightId,
        MarginBottomId,
        MarginLeftId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { Rect.parseWith(context, input, LengthOrPercentageOrAuto.Companion::parse) }

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
}
