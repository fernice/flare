package de.krall.flare.css.stylesheet

import de.krall.flare.css.parser.AllowQuirks
import de.krall.flare.css.parser.ParseMode
import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.parser.QuirksMode
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.ParserInput
import de.krall.flare.cssparser.RuleListParser
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Some

enum class Origin {

    USER_AGENT,

    USER,

    AUTHOR
}

class Stylesheet(private val rules: List<CssRule>,
                 origin: Origin) : Iterable<CssRule> {

    override fun iterator(): Iterator<CssRule> = rules.iterator()

    companion object {

        fun from(text: String,
                 origin: Origin): Stylesheet {

            val input = Parser(ParserInput(text))
            val context = ParserContext(ParseMode.Default(), QuirksMode.NO_QUIRKS)

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