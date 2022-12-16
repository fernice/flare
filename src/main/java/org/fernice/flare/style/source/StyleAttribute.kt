/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.source

import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.dom.Element
import org.fernice.flare.style.ApplicableDeclarationBlock
import org.fernice.flare.style.Importance
import org.fernice.flare.style.Origin
import org.fernice.flare.style.parser.ParseMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.url.Url

class StyleAttribute(
    override val declarations: PropertyDeclarationBlock,
    val source: Element,
) : StyleSource {

    override val origin: Origin
        get() = Origin.Author

    private val importantDeclarations = ApplicableDeclarationBlock(this, Importance.Important)
    private val normalDeclarations = ApplicableDeclarationBlock(this, Importance.Normal)

    override fun getApplicableDeclarations(importance: Importance): ApplicableDeclarationBlock {
        return when (importance) {
            Importance.Important -> importantDeclarations
            Importance.Normal -> normalDeclarations
        }
    }

    companion object {

        fun from(value: String, element: Element): StyleAttribute {
            val input = Parser.from(ParserInput(value))
            val context = ParserContext(ParseMode.Default, QuirksMode.NoQuirks, Url(""))

            val declarations = PropertyDeclarationBlock.parse(context, input)

            return StyleAttribute(declarations, element)
        }
    }
}
