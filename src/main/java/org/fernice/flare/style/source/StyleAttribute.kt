/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.source

import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.dom.Element
import org.fernice.flare.style.Origin
import org.fernice.flare.style.ParseMode
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.QuirksMode
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.stylesheet.CssRuleType
import org.fernice.flare.url.Url

class StyleAttribute(
    override val declarations: PropertyDeclarationBlock,
) : StyleSource {

    companion object {

        fun from(
            value: String,
            urlData: Url,
            quirksMode: QuirksMode,
            ruleType: CssRuleType,
        ): StyleAttribute {
            val input = Parser.from(ParserInput(value))
            val context = ParserContext.from(
                Origin.Author,
                urlData,
                ruleType,
                ParseMode.Default,
                quirksMode,
            )

            val declarations = PropertyDeclarationBlock.parsePropertyDeclarationList(context, input, listOf())

            return StyleAttribute(declarations)
        }
    }
}
