/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import fernice.std.Err
import fernice.std.Ok
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.cssparser.RuleListParser
import org.fernice.flare.style.parser.ParseMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.url.Url
import org.fernice.logging.FLogging
import java.net.URI

enum class Origin {

    USER_AGENT,

    USER,

    AUTHOR
}

class Stylesheet(
    val rules: List<CssRule>,
    val origin: Origin,
    val source: URI,
) : Iterable<CssRule> {

    override fun iterator(): Iterator<CssRule> = rules.iterator()

    companion object {

        fun from(text: String, origin: Origin, source: URI): Stylesheet {

            val input = Parser.new(ParserInput(text))
            val context = ParserContext(ParseMode.Default, QuirksMode.NO_QUIRKS, Url(""), source)

            val parser = TopLevelRuleParser(context)
            val iter = RuleListParser(input, parser, true)

            val rules = mutableListOf<CssRule>()

            loop@
            while (true) {
                val result = iter.next() ?: break@loop

                when (result) {
                    is Ok -> {
                        rules.add(result.value)
                    }
                    is Err -> {
                        LOG.warn("rule parse error: ${result.value.error} '${result.value.slice}'")
                    }
                }
            }

            return Stylesheet(rules, origin, source)
        }

        private val LOG = FLogging.logger { }
    }
}
