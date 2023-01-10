/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.cssparser.RuleListParser
import org.fernice.flare.style.Origin
import org.fernice.flare.style.parser.ParseMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.url.Url
import org.fernice.logging.FLogging
import java.net.URI

class Stylesheet(
    val origin: Origin,
    val rules: List<CssRule>,
    val source: URI,
) : Iterable<CssRule> {

    override fun iterator(): Iterator<CssRule> = rules.iterator()

    companion object {

        fun from(origin: Origin, text: String, source: URI): Stylesheet {

            val input = Parser.from(ParserInput(text))
            val context = ParserContext(ParseMode.Default, QuirksMode.NoQuirks, Url(""), source)

            val parser = TopLevelRuleParser(context, origin)
            val iter = RuleListParser(input, parser, true)

            val rules = mutableListOf<CssRule>()

            loop@
            while (true) {
                val result = iter.next() ?: break@loop

                when (result) {
                    is Ok -> rules.add(result.value)
                    is Err -> LOG.warn("rule parse error: ${result.value.error} '${result.value.slice}'")
                }
            }

            return Stylesheet(origin, rules, source)
        }

        private val LOG = FLogging.logger { }
    }
}
