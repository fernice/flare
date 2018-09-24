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
import org.fernice.flare.style.value.specified.LengthOrPercentageOrAuto
import fernice.std.Empty
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result


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

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                MarginTopId,
                MarginRightId,
                MarginBottomId,
                MarginLeftId
            )
        }

        val instance: MarginId by lazy { MarginId() }
    }
}
