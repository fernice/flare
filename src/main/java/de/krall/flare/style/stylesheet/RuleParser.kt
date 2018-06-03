package de.krall.flare.style.stylesheet

import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.PropertyDeclarationBlock
import de.krall.flare.style.properties.parsePropertyDeclarationList
import de.krall.flare.selector.SelectorList
import de.krall.flare.selector.SelectorParser
import de.krall.flare.cssparser.*
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

class AtRulePrelude

class QualifiedRulePrelude(val selectors: SelectorList, val location: SourceLocation)

sealed class CssRule {

    class AtRule : CssRule()

    class Style(val styleRule: StyleRule) : CssRule()
}

class StyleRule(val selectors: SelectorList,
                val declarations: PropertyDeclarationBlock,
                val location: SourceLocation)

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
        val location = input.sourceLocation()

        val parser = SelectorParser()
        val selectorResult = SelectorList.parse(parser, input)

        val selectorList = when (selectorResult) {
            is Ok -> selectorResult.value
            is Err -> return selectorResult
        }

        return Ok(QualifiedRulePrelude(selectorList, location))
    }

    override fun parseQualifiedRule(input: Parser, prelude: QualifiedRulePrelude): Result<CssRule, ParseError> {
        val declarations = parsePropertyDeclarationList(context, input)

        return Ok(CssRule.Style(StyleRule(
                prelude.selectors,
                declarations,
                prelude.location
        )))
    }
}