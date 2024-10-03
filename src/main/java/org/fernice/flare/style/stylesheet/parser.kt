/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.flare.cssparser.*
import org.fernice.flare.selector.ParseRelative
import org.fernice.flare.selector.SelectorList
import org.fernice.flare.selector.SelectorParser
import org.fernice.flare.style.ContextualError
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.StyleParseErrorKind
import org.fernice.flare.style.properties.DeclarationParserState
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.source.StyleRule
import org.fernice.std.*
import java.util.*

sealed class AtRulePrelude {
    // empty shells are used to at support the
    // parse-order checks.
    // v
    data object Import : AtRulePrelude()
    data object Namespace : AtRulePrelude()
    data object Layer : AtRulePrelude()
    // ^
}

sealed class CssRule {

    data class Style(val styleRule: StyleRule) : CssRule()
    data class Media(val mediaRule: MediaRule) : CssRule()

    override fun toString(): String {
        return when (this) {
            is Style -> "CssRule::Style($styleRule)"
            is Media -> "CssRule::Media($mediaRule)"
        }
    }
}

enum class RuleParserState {
    Start,
    EarlyLayers,
    Imports,
    Namespaces,
    Body,
}

class RuleParserLevelData(
    val declarationParserState: DeclarationParserState,
    val errorReportingState: LinkedList<SelectorList>,
    val rules: MutableList<CssRule>,
) {

    fun nest(): RuleParserLevelData {
        return RuleParserLevelData(
            declarationParserState = DeclarationParserState(),
            errorReportingState = errorReportingState,
            rules = mutableListOf(),
        )
    }

    companion object {
        fun root(): RuleParserLevelData {
            return RuleParserLevelData(
                declarationParserState = DeclarationParserState(),
                errorReportingState = LinkedList(),
                rules = mutableListOf(),
            )
        }
    }
}

class TopLevelRuleParser private constructor(
    val context: ParserContext,
    private var state: RuleParserState,
    var domError: Boolean,
    val level: RuleParserLevelData,
) : AtRuleParser<AtRulePrelude, SourcePosition>, QualifiedRuleParser<SelectorList, SourcePosition> {

    private fun checkState(state: RuleParserState): Boolean {
        if (state < this.state) {
            domError = true
            return false
        }

        return true
    }

    private fun nested(): NestedRuleParser {
        // I've had recurring nightmares. I was transmuting myself into
        // a transparent representation of myself, the NestedRuleParser,
        // just to take and swap parts of who I am, so I could pretend
        // to be someone else.
        return NestedRuleParser(context, level)
    }

    override fun parseAtRulePrelude(name: String, input: Parser): Result<AtRulePrelude, ParseError> {
        when (name) {
            "import" -> {
                if (!checkState(RuleParserState.Imports)) {
                    return Err(input.newError(StyleParseErrorKind.UnexpectedImportRule))
                }
                // TODO implementation (at-rule: import)
                return Ok(AtRulePrelude.Import)
            }

            "namespace" -> {
                if (!checkState(RuleParserState.Namespaces)) {
                    return Err(input.newError(StyleParseErrorKind.UnexpectedNamespaceRule))
                }
                // TODO implementation (at-rule: namespace)
                return Ok(AtRulePrelude.Namespace)
            }

            "charset" -> {
                domError = true
                return Err(input.newError(StyleParseErrorKind.UnexpectedCharsetRule))
            }

            "layer" -> {
                val stateToCheck = when {
                    state <= RuleParserState.EarlyLayers -> RuleParserState.EarlyLayers
                    else -> RuleParserState.Body
                }
                if (!checkState(stateToCheck)) {
                    return Err(input.newError(StyleParseErrorKind.UnexpectedLayerRule))
                }
                // TODO implementation (at-rule: layer)
                //      This is supposed to be a fallthrough, but the return
                //      is required for advancing the state correctly. Remove
                //      when implementing.
                return Ok(AtRulePrelude.Layer)
            }

            else -> {
                // at-rules with block are checked in parseAtRuleBlock
            }
        }

        return nested().parseAtRulePrelude(name, input)
    }

    override fun parseAtRuleBlock(start: ParserState, prelude: AtRulePrelude, input: Parser): Result<SourcePosition, ParseError> {
        if (!checkState(RuleParserState.Body)) {
            return Err(input.newError(ParseErrorKind.Unspecified))
        }
        nested().parseAtRuleBlock(start, prelude, input).propagate { return it }
        state = RuleParserState.Body
        return Ok(start.position())
    }

    override fun parseAtRuleWithoutBlock(start: ParserState, prelude: AtRulePrelude): Result<SourcePosition, Unit> {
        when (prelude) {
            AtRulePrelude.Import -> {
                // TODO implementation (at-rule: import)
                state = RuleParserState.Imports
            }

            AtRulePrelude.Namespace -> {
                // TODO implementation (at-rule: namespace)
                state = RuleParserState.Namespaces
            }

            AtRulePrelude.Layer -> {
                nested().parseAtRuleWithoutBlock(start, prelude).propagate { return it }
                state = if (state < RuleParserState.EarlyLayers) {
                    RuleParserState.EarlyLayers
                } else {
                    RuleParserState.Body
                }
            }

            else -> nested().parseAtRuleWithoutBlock(start, prelude).propagate { return it }
        }

        return Ok(start.position())
    }

    override fun parseQualifiedRulePrelude(input: Parser): Result<SelectorList, ParseError> {
        if (!checkState(RuleParserState.Body)) {
            return Err(input.newError(ParseErrorKind.Unspecified))
        }
        return nested().parseQualifiedRulePrelude(input)
    }

    override fun parseQualifiedRuleBlock(
        start: ParserState,
        prelude: SelectorList,
        input: Parser,
    ): Result<SourcePosition, ParseError> {
        nested().parseQualifiedRuleBlock(start, prelude, input)
        state = RuleParserState.Body
        return Ok(start.position())
    }

    companion object {
        fun from(
            context: ParserContext,
        ): TopLevelRuleParser {
            return TopLevelRuleParser(
                context = context,
                state = RuleParserState.Start,
                domError = false,
                level = RuleParserLevelData.root(),
            )
        }
    }
}

private class NestedRuleParser(
    private val context: ParserContext,
    private val level: RuleParserLevelData,
) : RuleBodyItemParser<AtRulePrelude, SelectorList, Unit> {

    private fun isInStyleRule(): Boolean {
        return context.ruleTypes.contains(CssRuleType.Style)
    }

    private inline fun <R> nestForRule(ruleType: CssRuleType, crossinline block: (NestedRuleParser) -> R): R {
        val nestedParser = NestedRuleParser(
            context = context,
            level = level.nest(),
        )

        return context.nestForRule(ruleType) {
            block(nestedParser)
        }
    }

    private fun parseNested(input: Parser, ruleType: CssRuleType): NestedParseResult {
        return nestForRule(ruleType) { parser ->
            val shouldParseDeclarations = parser.shouldParseDeclarations()

            val iter = RuleBodyParser(input, parser)
            while (true) {
                val result = iter.next() ?: break
                when (result) {
                    is Ok -> {}
                    is Err -> {
                        val (error, slice) = result.value
                        if (shouldParseDeclarations) {
                            parser.level.declarationParserState.didError(parser.context, error, slice)
                        } else {
                            context.reportError(error.location, ContextualError.InvalidRule(slice, error))
                        }
                    }
                }
            }
            val declarations = if (shouldParseDeclarations) {
                parser.level.declarationParserState.flushErrors(parser.context, parser.level.errorReportingState)
                parser.level.declarationParserState.takeDeclarations()
            } else {
                PropertyDeclarationBlock()
            }
            assert(!parser.level.declarationParserState.hasDeclarations()) { "declarations were parsed but not consumed" }

            NestedParseResult(
                parser.level.rules.drain(),
                declarations,
            )
        }
    }

    private inline fun <R> withErrorReportingState(selectors: SelectorList, block: () -> R): R {
        level.errorReportingState.push(selectors)
        val result = block()
        level.errorReportingState.pop()
        return result
    }

    /// AtRuleParser

    override fun parseAtRulePrelude(name: String, input: Parser): Result<AtRulePrelude, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    override fun parseAtRuleBlock(start: ParserState, prelude: AtRulePrelude, input: Parser): Result<Unit, ParseError> {
        return Err(input.newError(ParseErrorKind.UnsupportedFeature))
    }

    override fun parseAtRuleWithoutBlock(start: ParserState, prelude: AtRulePrelude): Result<Unit, Unit> {
        return Err()
    }

    /// QualifiedRuleParser

    override fun parseQualifiedRulePrelude(input: Parser): Result<SelectorList, ParseError> {
        val parser = SelectorParser()
        val parseRelative = when {
            isInStyleRule() -> ParseRelative.ForNesting
            else -> ParseRelative.No
        }
        return SelectorList.parse(parser, input, parseRelative)
    }

    override fun parseQualifiedRuleBlock(start: ParserState, prelude: SelectorList, input: Parser): Result<Unit, ParseError> {
        val result = withErrorReportingState(prelude) {
            parseNested(input, CssRuleType.Style)
        }

        val block = result.declarations
        val rules = result.rules.ifEmpty { null }
        level.rules.add(
            CssRule.Style(
                StyleRule(
                    prelude,
                    block,
                    rules,
                    start.location(),
                )
            )
        )

        return Ok()
    }

    /// DeclarationParser

    override fun parseValue(name: String, input: Parser): Result<Unit, ParseError> {
        return level.declarationParserState.parseValue(context, name, input)
    }

    /// RuleBodyItemParser

    override fun shouldParseDeclarations(): Boolean = true
    override fun shouldParseQualifiedRule(): Boolean = isInStyleRule()
}

private class NestedParseResult(
    val rules: List<CssRule>,
    val declarations: PropertyDeclarationBlock,
)
