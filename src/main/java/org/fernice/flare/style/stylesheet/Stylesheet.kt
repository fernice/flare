/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.flare.style.parser.ParseMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.cssparser.RuleListParser
import org.fernice.flare.url.Url
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Some

enum class Origin {

    USER_AGENT,

    USER,

    AUTHOR
}

class Stylesheet(
        val rules: List<CssRule>,
        val origin: Origin
) : Iterable<CssRule> {

    override fun iterator(): Iterator<CssRule> = rules.iterator()

    companion object {

        fun from(text: String, origin: Origin): Stylesheet {

            val input = Parser.new(ParserInput(text))
            val context = ParserContext(ParseMode.Default, QuirksMode.NO_QUIRKS, Url(""))

            val parser = TopLevelRuleParser(context)
            val iter = RuleListParser(input, parser, true)

            val rules = mutableListOf<CssRule>()

            loop@
            while (true) {
                val next = iter.next()

                val result = when (next) {
                    is Some -> next.value
                    is None -> break@loop
                }

                when (result) {
                    is Ok -> {
                        rules.add(result.value)
                    }
                    is Err -> {
                        println("rule parse error: ${result.value.error} '${result.value.slice}'")
                    }
                }
            }

            return Stylesheet(rules, origin)
        }
    }
}
