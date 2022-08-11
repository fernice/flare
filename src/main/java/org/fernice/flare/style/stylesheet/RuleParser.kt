/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.flare.cssparser.AtRuleParser
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.QualifiedRuleParser
import org.fernice.flare.cssparser.SourceLocation
import org.fernice.flare.selector.SelectorList
import org.fernice.flare.selector.SelectorParser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.properties.parsePropertyDeclarationList
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.toCssString
import java.net.URI

class AtRulePrelude

class QualifiedRulePrelude(val selectors: SelectorList, val location: SourceLocation)

sealed class CssRule {

    class AtRule : CssRule()

    data class Style(val styleRule: StyleRule) : CssRule()

    override fun toString(): String {
        return when(this){
            is AtRule -> "CssRule::AtRule"
            is Style -> "CssRule::Style($styleRule)"
        }
    }
}

class StyleRule(
    val selectors: SelectorList,
    val declarations: PropertyDeclarationBlock,
    val location: SourceLocation,
    val source: URI?
) {

    override fun toString(): String {
        return "StyleRule(${selectors.toCssString()}, ${declarations.count} declarations, location: $location source: $source)"
    }
}

class TopLevelRuleParser(private val context: ParserContext) : AtRuleParser<AtRulePrelude, CssRule>, QualifiedRuleParser<QualifiedRulePrelude, CssRule> {

    private fun nested(): NestedRuleParser {
        return NestedRuleParser(context)
    }

    override fun parseQualifiedRulePrelude(input: Parser): Result<QualifiedRulePrelude, ParseError> {
        return nested().parseQualifiedRulePrelude(input)
    }

    override fun parseQualifiedRule(input: Parser, prelude: QualifiedRulePrelude): Result<CssRule, ParseError> {
        return nested().parseQualifiedRule(input, prelude)
    }
}

class NestedRuleParser(private val context: ParserContext) : AtRuleParser<AtRulePrelude, CssRule>, QualifiedRuleParser<QualifiedRulePrelude, CssRule> {

    override fun parseQualifiedRulePrelude(input: Parser): Result<QualifiedRulePrelude, ParseError> {
        val parser = SelectorParser()
        val location = input.sourceLocation()

        val selectorList = when (val selectorList = SelectorList.parse(parser, input)) {
            is Ok -> selectorList.value
            is Err -> return selectorList
        }

        return Ok(QualifiedRulePrelude(selectorList, location))
    }

    override fun parseQualifiedRule(input: Parser, prelude: QualifiedRulePrelude): Result<CssRule, ParseError> {
        val declarations = parsePropertyDeclarationList(context, input)

        return Ok(
            CssRule.Style(
                StyleRule(
                    prelude.selectors,
                    declarations,
                    prelude.location,
                    context.source
                )
            )
        )
    }
}
