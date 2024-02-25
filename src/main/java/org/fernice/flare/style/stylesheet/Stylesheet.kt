/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.cssparser.StylesheetParser
import org.fernice.flare.style.*
import org.fernice.flare.url.Url
import org.fernice.std.Err
import org.fernice.std.Ok
import java.net.URI

class Stylesheet(
    val origin: Origin,
    val rules: List<CssRule>,
    val source: URI,
) : Iterable<CssRule> {

    override fun iterator(): Iterator<CssRule> = rules.iterator()

    companion object {

        fun from(
            text: String,
            urlData: Url,
            origin: Origin,
            quirksMode: QuirksMode,
            source: URI,
        ): Stylesheet {
            val input = Parser.from(ParserInput(text))

            val context = ParserContext.from(
                origin,
                urlData,
                ruleType = null,
                ParseMode.Default,
                quirksMode,
            )

            val parser = TopLevelRuleParser.from(
                context,
            )

            val iter = StylesheetParser(input, parser)
            while (true) {
                when (val result = iter.next() ?: break) {
                    is Ok -> {}
                    is Err -> {
                        val (error, slice) = result.value
                        context.reportError(error.location, ContextualError.InvalidRule(slice, error))
                    }
                }
            }

            return Stylesheet(origin, parser.level.rules, source)
        }
    }
}
