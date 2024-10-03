/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.custom

import org.fernice.flare.cssparser.BlockType
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.cssparser.Token
import org.fernice.flare.style.Origin
import org.fernice.flare.style.ParseMode
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.QuirksMode
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.CssWideKeywordDeclaration
import org.fernice.flare.style.properties.CustomPropertiesList
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.url.Url
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.std.map
import org.fernice.std.unwrap
import org.fernice.std.unwrapErr
import org.fernice.std.unwrapOrElse
import org.fernice.std.unwrapOrNull
import java.util.LinkedList
import kotlin.text.StringBuilder

class VariableValue(
    val block: TemplateValue.Block,
) {

    override fun toString(): String = "VariableValue($block)"
}

class UnparsedValue(
    val block: TemplateValue.Block,
    val url: Url,
    val fromShorthand: ShorthandId?,
) {

    fun substituteVariables(
        longhandId: LonghandId,
        customProperties: CustomPropertiesList?,
        substitutionCache: SubstitutionCache,
    ): PropertyDeclaration {
        val shorthandId = fromShorthand
        if (shorthandId != null) {
            val key = (shorthandId to longhandId)
            val declaration = substitutionCache.find(key)
            if (declaration != null) return declaration
        }

        val invalidAtComputedValueTime = {
            val keyword = if (longhandId.isInherited) CssWideKeyword.Inherit else CssWideKeyword.Initial
            PropertyDeclaration.CssWideKeyword(CssWideKeywordDeclaration(longhandId, keyword))
        }

        val css = performSubstitution(customProperties) ?: return invalidAtComputedValueTime()

        val context = ParserContext.from(
            origin = Origin.Author, // technically not correct
            urlData = url,
            ruleType = null, // technically not correct
            parseMode = ParseMode.Default, // technically not correct
            quirksMode = QuirksMode.NoQuirks, // technically not correct
        )

        val input = Parser.from(ParserInput(css))
        input.skipWhitespace()

        input.tryParse { org.fernice.flare.style.properties.CssWideKeyword.parse(it) }
            .map { PropertyDeclaration.CssWideKeyword(CssWideKeywordDeclaration(longhandId, it)) }
            .unwrapErr { return it.value }

        if (shorthandId == null) {
            return input.parseEntirely { longhandId.parseValue(context, input) }
                .unwrapOrElse { invalidAtComputedValueTime() }
        }

        val declarations = mutableListOf<PropertyDeclaration>()

        if (shorthandId.parseInto(declarations, context, input).isErr()) {
            return invalidAtComputedValueTime()
        }

        for (declaration in declarations) {
            val id = declaration.id as PropertyDeclarationId.Longhand
            val key = (shorthandId to id.id)
            substitutionCache.put(key, declaration)
        }

        val key = (shorthandId to longhandId)
        return substitutionCache.find(key) ?: error("shorthand $shorthandId did not yield longhand $longhandId")
    }

    private fun performSubstitution(customProperties: CustomPropertiesList?): String? {
        if (customProperties == null) return null

        return substitute(block, customProperties.toMap(), LinkedList())
    }

    private fun substitute(block: TemplateValue.Block, variables: Map<Name, VariableValue>, stack: LinkedList<Name>): String? {
        return buildString {
            for (value in block.values) {
                when (value) {
                    is TemplateValue.Text -> {
                        if (requiresDelimiter()) append(" ")
                        append(value.text)
                    }

                    is TemplateValue.Block -> error("template value should have been simplified")
                    is TemplateValue.Variable -> {
                        if (requiresDelimiter()) append(" ")
                        append(resolve(value, variables, stack) ?: return null)
                    }
                }
            }
        }
    }

    private fun resolve(variable: TemplateValue.Variable, variables: Map<Name, VariableValue>, stack: LinkedList<Name>): String? {
        val name = variable.name
        val fallback = variable.fallback

        if (!stack.contains(name)) {
            val value = variables[name]?.block
            if (value != null) {
                stack.push(name)
                val substitution = substitute(value, variables, stack)
                stack.pop()
                if (substitution != null) return substitution
            }
        }

        if (fallback != null) {
            return substitute(fallback, variables, stack)
        }

        return null
    }

    private fun StringBuilder.requiresDelimiter(): Boolean {
        return isNotEmpty() && get(lastIndex) != ' '
    }

    override fun toString(): String = "UnparsedValue($block, fromShorthand: $fromShorthand)"
}

sealed class TemplateValue {
    data class Text(val text: String) : TemplateValue()
    data class Block(val values: List<TemplateValue>) : TemplateValue()
    data class Variable(val name: Name, val fallback: Block?) : TemplateValue()

    fun toCss(): String {
        return when (this) {
            is Text -> text
            is Block -> buildString {
                for (value in values) {
                    append(value.toCss())
                }
            }

            is Variable -> buildString {
                append("var(")
                append(name)
                if (fallback != null) {
                    append(",")
                    append(fallback.toCss())
                }
                append(")")
            }
        }
    }

    companion object {

        fun parse(input: Parser): Result<Block, ParseError> {
            return input.parseUntilBefore(Delimiters.Bang or Delimiters.SemiColon) { scopedInput ->
                val state = input.state()
                input.nextIncludingWhitespace().unwrap { return@parseUntilBefore it }
                input.reset(state)

                parseBlock(scopedInput)
                    .map { it.simplify() }
            }
        }

        private fun parseBlock(input: Parser): Result<Block, ParseError> {
            val values = mutableListOf<TemplateValue>()

            var segmentStart = input.sourcePosition()
            var tokenStart = input.sourcePosition()
            var token = input.nextIncludingWhitespaceAndComment()
                .unwrapOrNull()

            val missingClosingCharacters = StringBuilder()

            while (token != null) {
                when (token) {
                    is Token.Comment -> {
                        val slice = input.sliceFrom(tokenStart)
                        if (!slice.endsWith("*/")) {
                            missingClosingCharacters.append(if (slice.endsWith("*")) "/" else "*/")
                        }
                    }

                    is Token.BadUrl -> {
                        return Err(input.newError(ValueParseErrorKind.BadUrlInDeclarationValueBlock(token.url)))
                    }

                    is Token.BadString -> {
                        return Err(input.newError(ValueParseErrorKind.BadStringInDeclarationValueBlock(token.value)))
                    }

                    is Token.RParen -> {
                        return Err(input.newError(ValueParseErrorKind.UnbalancedCloseParenthesisInDeclarationValueBlock))
                    }

                    is Token.RBracket -> {
                        return Err(input.newError(ValueParseErrorKind.UnbalancedCloseBracketInDeclarationValueBlock))
                    }

                    is Token.RBrace -> {
                        return Err(input.newError(ValueParseErrorKind.UnbalancedCloseBraceInDeclarationValueBlock))
                    }

                    is Token.Function,
                    is Token.LParen,
                    is Token.LBracket,
                    is Token.LBrace,
                    -> {
                        if (token is Token.Function && token.name.equals("var", ignoreCase = true)) {
                            values.add(Text(input.slice(segmentStart, tokenStart)))

                            values.add(input.parseNestedBlock { scopedInput ->
                                parseVarFunction(scopedInput)
                            }.unwrap { return it })

                            segmentStart = input.sourcePosition()
                        } else {
                            val blockType = BlockType.opening(token) ?: error("expected block type")

                            values.add(Text(input.sliceFrom(segmentStart)))
                            values.add(input.parseNestedBlock { scopedInput ->
                                parseBlock(scopedInput)
                            }.unwrap { return it })
                            values.add(Text(blockType.closing))

                            segmentStart = input.sourcePosition()
                        }
                    }

                    is Token.Identifier,
                    is Token.AtKeyword,
                    is Token.Hash,
                    is Token.IdHash,
                    is Token.UnquotedUrl,
                    is Token.Dimension,
                    -> {
                        val value = when (token) {
                            is Token.Identifier -> token.name
                            is Token.AtKeyword -> token.name
                            is Token.Hash -> token.value
                            is Token.IdHash -> token.value
                            is Token.UnquotedUrl -> token.url
                            is Token.Dimension -> token.unit
                            else -> error("no value access defined for token $token")
                        }

                        if (value.endsWith('\uFFFD') && input.sliceFrom(tokenStart).endsWith("\\")) {
                            missingClosingCharacters.append('\uFFFD')
                        }

                        if (token is Token.UnquotedUrl) {
                            val slice = input.sliceFrom(tokenStart)
                            if (!slice.endsWith(")")) {
                                missingClosingCharacters.append(")")
                            }
                        }
                    }

                    else -> {}
                }

                tokenStart = input.sourcePosition()
                token = input.nextIncludingWhitespaceAndComment()
                    .unwrapOrNull()
            }

            values.add(Text(input.slice(segmentStart, tokenStart)))
            values.add(Text(missingClosingCharacters.toString()))

            return Ok(Block(values))
        }

        private fun parseVarFunction(input: Parser): Result<Variable, ParseError> {
            val identifier = input.expectIdentifier().unwrap { return it }
            val name = Name.parse(identifier).unwrap { return Err(input.newError(ValueParseErrorKind.ExpectedCustomPropertyName)) }
            val fallback = if (input.tryParse { it.expectComma() }.isOk()) {
                parseFallback(input).unwrap { return it }
            } else {
                null
            }
            return Ok(Variable(name, fallback))
        }

        private fun parseFallback(input: Parser): Result<Block, ParseError> {
            return parseBlock(input)
        }
    }
}


@Suppress("UNCHECKED_CAST")
fun <T : TemplateValue> T.simplify(): T {
    return when (this) {
        is TemplateValue.Text -> this
        is TemplateValue.Block -> {
            val inlined = mutableListOf<TemplateValue>()
            for (value in values) {
                val simplifiedValue = value.simplify()

                if (simplifiedValue is TemplateValue.Block) {
                    inlined.addAll(simplifiedValue.values)
                } else {
                    inlined.add(simplifiedValue)
                }
            }

            val coerced = mutableListOf<TemplateValue>()
            var previousText: TemplateValue.Text? = null
            for (value in inlined) {
                if (value is TemplateValue.Text) {
                    previousText = if (previousText != null) {
                        TemplateValue.Text(previousText.text + value.text)
                    } else {
                        value
                    }
                    continue
                }
                if (previousText != null && previousText.text.isNotEmpty()) {
                    coerced.add(previousText)
                    previousText = null
                }
                coerced.add(value)
            }
            if (previousText != null && previousText.text.isNotEmpty()) {
                coerced.add(previousText)
            }

            TemplateValue.Block(coerced) as T
        }

        is TemplateValue.Variable -> TemplateValue.Variable(name, fallback?.simplify()) as T
        else -> error("unreachable")
    }
}

sealed class ValueParseErrorKind : ParseErrorKind() {

    data class BadUrlInDeclarationValueBlock(val url: String) : ValueParseErrorKind()
    data class BadStringInDeclarationValueBlock(val url: String) : ValueParseErrorKind()
    object UnbalancedCloseParenthesisInDeclarationValueBlock : ValueParseErrorKind()
    object UnbalancedCloseBracketInDeclarationValueBlock : ValueParseErrorKind()
    object UnbalancedCloseBraceInDeclarationValueBlock : ValueParseErrorKind()
    object ExpectedCustomPropertyName : ValueParseErrorKind()
}
