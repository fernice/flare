/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.cssparser.Nth
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.SourceLocation
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newError
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.cssparser.parseNth
import org.fernice.flare.style.parser.QuirksMode
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.let

/**
 * The selector specific error kinds
 */
sealed class SelectorParseErrorKind : ParseErrorKind() {

    object UnknownSelector : SelectorParseErrorKind()
    object DanglingCombinator : SelectorParseErrorKind()
    object PseudoElementExpectedColon : SelectorParseErrorKind()
    object NoIdentifierForPseudo : SelectorParseErrorKind()
    object ExpectedNamespace : SelectorParseErrorKind()
    object ExpectedBarAttributeSelector : SelectorParseErrorKind()
    object InvalidQualifiedNameInAttributeSelector : SelectorParseErrorKind()
    object ExplicitNamespaceUnexpectedToken : SelectorParseErrorKind()
    object ClassNeedsIdentifier : SelectorParseErrorKind()
    object PseudoNeedsIdentifier : SelectorParseErrorKind()
    object EmptyNegation : SelectorParseErrorKind()
    object NonSimpleSelectorInNegation : SelectorParseErrorKind()
    object NoQualifiedNameInAttributeSelector : SelectorParseErrorKind()
    data class UnexpectedTokenInAttributeSelector(val token: Token) : SelectorParseErrorKind()

    override fun toString(): String {
        return "SelectorParseErrorKind::${javaClass.simpleName}"
    }
}

interface SelectorParserContext {

    fun pseudoElementAllowsSingleColon(name: String): Boolean

    fun defaultNamespace(): Option<NamespaceUrl>

    fun namespacePrefix(prefix: String): NamespacePrefix

    fun namespaceForPrefix(prefix: NamespacePrefix): Option<NamespaceUrl>
}

fun parseSelector(context: SelectorParserContext, input: Parser): Result<Selector, ParseError> {
    val builder = SelectorBuilder()

    var hasPseudoElement: Boolean

    outer@
    while (true) {
        val compoundOption = when (val compoundResult = parseCompoundSelector(context, input, builder)) {
            is Ok -> compoundResult.value
            is Err -> return compoundResult
        }

        if (compoundOption is Some) {
            hasPseudoElement = compoundOption.value.hasPseudoElement
        } else {
            return if (builder.hasDanglingCombinator()) {
                Err(input.newError(SelectorParseErrorKind.DanglingCombinator))
            } else {
                Err(input.newError(SelectorParseErrorKind.UnknownSelector))
            }
        }

        if (hasPseudoElement) {
            break
        }

        val combinator: Combinator
        var seenWhitespace = false

        inner@
        while (true) {
            val state = input.state()

            val token = when (val token = input.nextIncludingWhitespace()) {
                is Ok -> token.value
                is Err -> break@outer
            }

            when (token) {
                is Token.Whitespace -> {
                    seenWhitespace = true
                }
                is Token.Gt -> {
                    combinator = Combinator.Child
                    break@inner
                }
                is Token.Plus -> {
                    combinator = Combinator.NextSibling
                    break@inner
                }
                is Token.Tidle -> {
                    combinator = Combinator.LaterSibling
                    break@inner
                }
                else -> {
                    input.reset(state)
                    if (seenWhitespace) {
                        combinator = Combinator.Descendant
                        break@inner
                    } else {
                        break@outer
                    }
                }
            }
        }
        builder.pushCombinator(combinator)
    }

    return Ok(builder.build(hasPseudoElement))
}

private class ParseResult(val hasPseudoElement: Boolean)

private fun parseCompoundSelector(
    context: SelectorParserContext,
    input: Parser,
    builder: SelectorBuilder
): Result<Option<ParseResult>, ParseError> {
    input.skipWhitespace()

    var empty = true
    var pseudo = false

    when (val typeSelectorResult = parseTypeSelector(context, input, builder::pushSimpleSelector)) {
        is Ok -> {
            if (!typeSelectorResult.value) {
                context.defaultNamespace().let {
                    builder.pushSimpleSelector(Component.DefaultNamespace(it))
                }
            } else {
                empty = false
            }
        }
        is Err -> {
            return typeSelectorResult
        }
    }

    loop@
    while (true) {
        val selector = when (val selector = parseOneSimpleSelector(context, input, false)) {
            is Ok -> {
                val option = selector.value

                if (option is Some) {
                    option.value
                } else {
                    break@loop
                }
            }
            is Err -> return selector
        }

        when (selector) {
            is SimpleSelectorParseResult.SimpleSelector -> {
                builder.pushSimpleSelector(selector.component)

                empty = false
            }
            is SimpleSelectorParseResult.PseudoElement -> {
                val stateSelectors = mutableListOf<Component>()

                inner@
                while (true) {
                    var location = input.sourceLocation()

                    var token = when (val token = input.nextIncludingWhitespace()) {
                        is Ok -> token.value
                        is Err -> break@inner
                    }

                    when (token) {
                        is Token.Whitespace -> break@inner
                        is Token.Colon -> {
                        }
                        else -> {
                            return Err(location.newError(SelectorParseErrorKind.PseudoElementExpectedColon))
                        }
                    }

                    location = input.sourceLocation()

                    token = when (val tokenResult = input.nextIncludingWhitespace()) {
                        is Ok -> tokenResult.value
                        is Err -> return tokenResult
                    }

                    when (token) {
                        is Token.Identifier -> {
                            when (val pseudoClassResult = parseNonTSPseudoClass(location, token.name)) {
                                is Ok -> {
                                    stateSelectors.add(Component.NonTSPseudoClass(pseudoClassResult.value))
                                }
                                is Err -> {
                                    return pseudoClassResult
                                }
                            }
                        }
                        else -> {
                            return Err(location.newError(SelectorParseErrorKind.NoIdentifierForPseudo))
                        }
                    }
                }

                if (!builder.isEmpty()) {
                    builder.pushCombinator(Combinator.PseudoElement)
                }

                builder.pushSimpleSelector(Component.PseudoElement(selector.pseudoElement))

                for (component in stateSelectors) {
                    builder.pushSimpleSelector(component)
                }

                empty = false
                pseudo = true

                break@loop
            }
        }
    }

    return if (empty) {
        Ok(None)
    } else {
        Ok(Some(ParseResult(pseudo)))
    }
}

private sealed class QualifiedNamePrefix {

    object ImplicitNoNamespace : QualifiedNamePrefix()

    object ImplicitAnyNamespace : QualifiedNamePrefix()

    class ImplicitDefaultNamespace(val url: NamespaceUrl) : QualifiedNamePrefix()

    object ExplicitNoNamespace : QualifiedNamePrefix()

    object ExplicitAnyNamespace : QualifiedNamePrefix()

    class ExplicitNamespace(val prefix: NamespacePrefix, val url: NamespaceUrl) : QualifiedNamePrefix()
}

private sealed class QualifiedName {

    object None : QualifiedName()

    class Some(val prefix: QualifiedNamePrefix, val localName: Option<String>) : QualifiedName()
}

private fun parseTypeSelector(
    context: SelectorParserContext,
    input: Parser,
    sink: (Component) -> Unit
): Result<Boolean, ParseError> {
    val qualifiedName = when (val qualifiedName = parseQualifiedName(context, input, false)) {
        is Ok -> qualifiedName.value
        is Err -> {
            return if (input.isExhausted()) {
                Ok(false)
            } else {
                qualifiedName
            }
        }
    }

    return when (qualifiedName) {
        is QualifiedName.Some -> {
            when (qualifiedName.prefix) {
                is QualifiedNamePrefix.ImplicitNoNamespace -> throw IllegalStateException("unreachable")
                is QualifiedNamePrefix.ImplicitDefaultNamespace -> {
                    sink(Component.ExplicitNoNamespace)
                }
                is QualifiedNamePrefix.ExplicitNoNamespace -> {
                    sink(Component.ExplicitNoNamespace)
                }
                is QualifiedNamePrefix.ExplicitAnyNamespace -> {
                    when (val defaultNamespace = context.defaultNamespace()) {
                        is Some -> {
                            sink(Component.DefaultNamespace(defaultNamespace.value))
                        }
                        is None -> {
                            sink(Component.ExplicitAnyNamespace)
                        }
                    }
                }
                is QualifiedNamePrefix.ExplicitNamespace -> {
                    when (val defaultNamespace = context.defaultNamespace()) {
                        is Some -> {
                            if (defaultNamespace.value == qualifiedName.prefix.url) {
                                sink(Component.DefaultNamespace(qualifiedName.prefix.url))
                            } else {
                                sink(Component.Namespace(qualifiedName.prefix.prefix, qualifiedName.prefix.url))
                            }
                        }
                        is None -> {
                            sink(Component.DefaultNamespace(qualifiedName.prefix.url))
                        }
                    }
                }
                else -> {
                }
            }

            when (qualifiedName.localName) {
                is Some -> {
                    val localName = qualifiedName.localName.value
                    val localNameLower = localName.toLowerCase()

                    sink(Component.LocalName(localName, localNameLower))
                }
                is None -> {
                    sink(Component.ExplicitUniversalType)
                }
            }

            Ok(true)
        }
        is QualifiedName.None -> {
            Ok(false)
        }
    }
}

private fun parseQualifiedName(
    context: SelectorParserContext,
    input: Parser,
    attributeSelector: Boolean
): Result<QualifiedName, ParseError> {
    val state = input.state()

    val token = when (val token = input.nextIncludingWhitespace()) {
        is Ok -> token.value
        is Err -> {
            input.reset(state)
            return token
        }
    }

    return when (token) {
        is Token.Identifier -> {
            val afterIdentState = input.state()

            val innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                is Ok -> innerToken.value
                is Err -> {
                    input.reset(afterIdentState)

                    return if (attributeSelector) {
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace, Some(token.name)))
                    } else {
                        defaultNamespace(context, Some(token.name))
                    }
                }
            }

            when (innerToken) {
                is Token.Pipe -> {
                    val prefix = context.namespacePrefix(token.name)

                    when (val namespace = context.namespaceForPrefix(prefix)) {
                        is Some -> {
                            explicitNamespace(input, QualifiedNamePrefix.ExplicitNamespace(prefix, namespace.value), attributeSelector)
                        }
                        is None -> {
                            Err(afterIdentState.location().newError(SelectorParseErrorKind.ExpectedNamespace))
                        }
                    }
                }
                else -> {
                    input.reset(afterIdentState)
                    if (attributeSelector) {
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace, Some(token.name)))
                    } else {
                        defaultNamespace(context, Some(token.name))
                    }
                }
            }
        }
        is Token.Asterisk -> {
            val afterAsteriskState = input.state()

            val innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                is Ok -> innerToken.value
                is Err -> {
                    input.reset(afterAsteriskState)

                    return if (attributeSelector) {
                        innerToken
                    } else {
                        defaultNamespace(context, None)
                    }
                }
            }

            when (innerToken) {
                is Token.Pipe -> {
                    explicitNamespace(input, QualifiedNamePrefix.ExplicitAnyNamespace, attributeSelector)
                }
                else -> {
                    input.reset(afterAsteriskState)

                    if (attributeSelector) {
                        Err(afterAsteriskState.location().newError(SelectorParseErrorKind.ExpectedBarAttributeSelector))
                    } else {
                        defaultNamespace(context, None)
                    }
                }
            }
        }
        is Token.Pipe -> {
            explicitNamespace(input, QualifiedNamePrefix.ExplicitNoNamespace, attributeSelector)
        }
        else -> {
            input.reset(state)
            Ok(QualifiedName.None)
        }
    }
}

private fun explicitNamespace(
    input: Parser,
    prefix: QualifiedNamePrefix,
    attributeSelector: Boolean
): Result<QualifiedName, ParseError> {
    val location = input.sourceLocation()

    val token = when (val token = input.nextIncludingWhitespace()) {
        is Ok -> token.value
        is Err -> return token
    }

    return when (token) {
        is Token.Identifier -> {
            Ok(QualifiedName.Some(prefix, Some(token.name)))
        }
        is Token.Asterisk -> {
            if (!attributeSelector) {
                Ok(QualifiedName.Some(prefix, None))
            } else {
                Err(location.newError(SelectorParseErrorKind.InvalidQualifiedNameInAttributeSelector))
            }
        }
        else -> {
            if (attributeSelector) {
                Err(location.newError(SelectorParseErrorKind.InvalidQualifiedNameInAttributeSelector))
            } else {
                Err(location.newError(SelectorParseErrorKind.ExplicitNamespaceUnexpectedToken))
            }
        }
    }
}

private fun defaultNamespace(context: SelectorParserContext, name: Option<String>): Result<QualifiedName, ParseError> {
    val namespace = when (val namespace = context.defaultNamespace()) {
        is Some -> QualifiedNamePrefix.ImplicitDefaultNamespace(namespace.value)
        is None -> QualifiedNamePrefix.ImplicitAnyNamespace
    }

    return Ok(QualifiedName.Some(namespace, name))
}

private sealed class SimpleSelectorParseResult {

    class SimpleSelector(val component: Component) : SimpleSelectorParseResult()

    class PseudoElement(val pseudoElement: org.fernice.flare.selector.PseudoElement) : SimpleSelectorParseResult()
}

private fun parseOneSimpleSelector(
    context: SelectorParserContext,
    input: Parser,
    negated: Boolean
): Result<Option<SimpleSelectorParseResult>, ParseError> {
    val state = input.state()

    val token = when (val token = input.nextIncludingWhitespace()) {
        is Ok -> token.value
        is Err -> {
            input.reset(state)
            return Ok(None)
        }
    }

    when (token) {
        is Token.IdHash -> {
            val component = Component.ID(token.value)

            return Ok(Some(SimpleSelectorParseResult.SimpleSelector(component)))
        }
        is Token.Dot -> {
            val location = input.sourceLocation()

            val innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                is Ok -> innerToken.value
                is Err -> return innerToken
            }

            return when (innerToken) {
                is Token.Identifier -> {
                    val component = Component.Class(innerToken.name)

                    Ok(Some(SimpleSelectorParseResult.SimpleSelector(component)))
                }
                else -> {
                    Err(location.newError(SelectorParseErrorKind.ClassNeedsIdentifier))
                }
            }
        }
        is Token.LBracket -> {
            return when (val attributeSelector = input.parseNestedBlock { parseAttributeSelector(context, it) }) {
                is Ok -> Ok(Some(SimpleSelectorParseResult.SimpleSelector(attributeSelector.value)))
                is Err -> attributeSelector
            }
        }
        is Token.Colon -> {
            val location = input.sourceLocation()

            var innerToken = when (val innerToken = input.nextIncludingWhitespace()) {
                is Ok -> innerToken.value
                is Err -> return innerToken
            }

            val doubleColon = when (innerToken) {
                is Token.Colon -> {
                    innerToken = when (val innerTokenResult = input.nextIncludingWhitespace()) {
                        is Ok -> innerTokenResult.value
                        is Err -> return innerTokenResult
                    }

                    true
                }
                else -> {
                    false
                }
            }

            val (name, functional) = when (innerToken) {
                is Token.Identifier -> {
                    Pair(innerToken.name, false)
                }
                is Token.Function -> {
                    Pair(innerToken.name, true)
                }
                else -> {
                    return Err(location.newError(SelectorParseErrorKind.PseudoNeedsIdentifier))
                }
            }

            return if (doubleColon || context.pseudoElementAllowsSingleColon(name)) {
                val pseudoElementResult = if (functional) {
                    input.parseNestedBlock { parseFunctionalPseudoElement(it, location, name) }
                } else {
                    parsePseudoElement(location, name)
                }

                when (pseudoElementResult) {
                    is Ok -> Ok(Some(SimpleSelectorParseResult.PseudoElement(pseudoElementResult.value)))
                    is Err -> return pseudoElementResult
                }
            } else {
                val pseudoClassResult = if (functional) {
                    input.parseNestedBlock { parseFunctionalPseudoClass(context, it, location, name, negated) }
                } else {
                    parsePseudoClass(location, name)
                }

                when (pseudoClassResult) {
                    is Ok -> Ok(Some(SimpleSelectorParseResult.SimpleSelector(pseudoClassResult.value)))
                    is Err -> return pseudoClassResult
                }
            }
        }
        else -> {
            input.reset(state)
            return Ok(None)
        }
    }
}

private fun parseFunctionalPseudoElement(
    @Suppress("UNUSED_PARAMETER") input: Parser,
    location: SourceLocation,
    name: String
): Result<PseudoElement, ParseError> {
    return Err(location.newUnexpectedTokenError(Token.Function(name)))
}

private fun parsePseudoElement(location: SourceLocation, name: String): Result<PseudoElement, ParseError> {
    return when (name.toLowerCase()) {
        "before" -> Ok(PseudoElement.Before)
        "after" -> Ok(PseudoElement.After)
        "selection" -> Ok(PseudoElement.Selection)
        "first-letter" -> Ok(PseudoElement.FirstLetter)
        "first-line" -> Ok(PseudoElement.FirstLine)
        "placeholder" -> Ok(PseudoElement.Placeholder)
        "icon" -> Ok(PseudoElement.Icon)
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseFunctionalPseudoClass(
    context: SelectorParserContext,
    input: Parser,
    location: SourceLocation,
    name: String,
    negated: Boolean
): Result<Component, ParseError> {
    return when (name.toLowerCase()) {
        "nth-child" -> parseNthPseudoClass(input, Component::NthChild)
        "nth-of-type" -> parseNthPseudoClass(input, Component::NthOfType)
        "nth-last-child" -> parseNthPseudoClass(input, Component::NthLastChild)
        "nth-last-of-type" -> parseNthPseudoClass(input, Component::NthLastOfType)
        "not" -> {
            return if (negated) {
                Err(location.newUnexpectedTokenError(Token.Function(name)))
            } else {
                parseNegation(context, input)
            }
        }
        else -> parseNonTSFunctionalPseudoClass(input, location, name).map(Component::NonTSPseudoClass)
    }
}

private fun parseNthPseudoClass(input: Parser, wrapper: (Nth) -> Component): Result<Component, ParseError> {
    return when (val nthResult = parseNth(input)) {
        is Ok -> Ok(wrapper(nthResult.value))
        is Err -> nthResult
    }
}

private fun parseNonTSFunctionalPseudoClass(
    input: Parser,
    location: SourceLocation,
    name: String
): Result<NonTSPseudoClass, ParseError> {
    return when (name.toLowerCase()) {
        "lang" -> {
            return when (val identifierResult = input.expectIdentifier()) {
                is Ok -> Ok(NonTSPseudoClass.Lang(identifierResult.value))
                is Err -> identifierResult
            }
        }
        else -> Err(location.newUnexpectedTokenError(Token.Function(name)))
    }
}

private fun parsePseudoClass(location: SourceLocation, name: String): Result<Component, ParseError> {
    return when (name.toLowerCase()) {
        "first-child" -> Ok(Component.FirstChild)
        "last-child" -> Ok(Component.LastChild)
        "only-child" -> Ok(Component.OnlyChild)
        "first-of-type" -> Ok(Component.FirstOfType)
        "last-of-type" -> Ok(Component.LastOfType)
        "only-of-type" -> Ok(Component.OnlyOfType)
        "root" -> Ok(Component.Root)
        "empty" -> Ok(Component.Empty)
        "scope" -> Ok(Component.Scope)
        "host" -> Ok(Component.Host)
        else -> parseNonTSPseudoClass(location, name).map(Component::NonTSPseudoClass)
    }
}

private fun parseNonTSPseudoClass(location: SourceLocation, name: String): Result<NonTSPseudoClass, ParseError> {
    return when (name.toLowerCase()) {
        "active" -> Ok(NonTSPseudoClass.Active)
        "checked" -> Ok(NonTSPseudoClass.Checked)
        "disabled" -> Ok(NonTSPseudoClass.Disabled)
        "enabled" -> Ok(NonTSPseudoClass.Enabled)
        "focus" -> Ok(NonTSPseudoClass.Focus)
        "fullscreen" -> Ok(NonTSPseudoClass.Fullscreen)
        "hover" -> Ok(NonTSPseudoClass.Hover)
        "indeterminate" -> Ok(NonTSPseudoClass.Indeterminate)
        "link" -> Ok(NonTSPseudoClass.Link)
        "placeholder-shown" -> Ok(NonTSPseudoClass.PlaceholderShown)
        "read-write" -> Ok(NonTSPseudoClass.ReadWrite)
        "read-only" -> Ok(NonTSPseudoClass.ReadOnly)
        "target" -> Ok(NonTSPseudoClass.Target)
        "visited" -> Ok(NonTSPseudoClass.Visited)
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseNegation(context: SelectorParserContext, input: Parser): Result<Component, ParseError> {
    val simpleSelector = mutableListOf<Component>()

    input.skipWhitespace()

    val parsed = when (val typeSelectorResult = parseTypeSelector(context, input) { simpleSelector.add(it) }) {
        is Err -> {
            return if (typeSelectorResult.value.kind == ParseErrorKind.EndOfFile) {
                Err(input.newError(SelectorParseErrorKind.EmptyNegation))
            } else {
                typeSelectorResult
            }
        }
        is Ok -> typeSelectorResult.value
    }

    if (!parsed) {
        val selector = when (val selector = parseOneSimpleSelector(context, input, true)) {
            is Ok -> {
                when (val option = selector.value) {
                    is Some -> option.value
                    is None -> return Err(input.newError(SelectorParseErrorKind.EmptyNegation))
                }
            }
            is Err -> return selector
        }

        when (selector) {
            is SimpleSelectorParseResult.SimpleSelector -> {
                simpleSelector.add(selector.component)
            }
            is SimpleSelectorParseResult.PseudoElement -> {
                return Err(input.newError(SelectorParseErrorKind.NonSimpleSelectorInNegation))
            }
        }
    }

    return Ok(Component.Negation(simpleSelector))
}


private fun parseAttributeSelector(context: SelectorParserContext, input: Parser): Result<Component, ParseError> {
    val qualifiedName = when (val qualifiedName = parseQualifiedName(context, input, true)) {
        is Ok -> qualifiedName.value
        is Err -> return qualifiedName
    }

    val localName: String
    val namespace: Option<NamespaceConstraint>

    when (qualifiedName) {
        is QualifiedName.Some -> {
            localName = when (qualifiedName.localName) {
                is Some -> qualifiedName.localName.value
                is None -> throw IllegalStateException("unreachable")
            }

            val prefix = qualifiedName.prefix
            namespace = when (prefix) {
                is QualifiedNamePrefix.ImplicitNoNamespace, is QualifiedNamePrefix.ExplicitNoNamespace -> {
                    None
                }
                is QualifiedNamePrefix.ExplicitNamespace -> {
                    Some(NamespaceConstraint.Specific(prefix.prefix, prefix.url))
                }
                is QualifiedNamePrefix.ExplicitAnyNamespace -> Some(NamespaceConstraint.Any)
                is QualifiedNamePrefix.ImplicitAnyNamespace, is QualifiedNamePrefix.ImplicitDefaultNamespace -> {
                    throw IllegalStateException("unreachable")
                }
            }
        }
        is QualifiedName.None -> {
            return Err(input.newError(SelectorParseErrorKind.NoQualifiedNameInAttributeSelector))
        }
    }

    val location = input.sourceLocation()

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> {
            val localNameLower = localName.toLowerCase()
            return if (namespace is Some) {
                Ok(
                    Component.AttributeOther(
                        namespace.value,
                        localName,
                        localNameLower,
                        AttributeSelectorOperation.Exists,
                        false
                    )
                )
            } else {
                Ok(
                    Component.AttributeInNoNamespaceExists(
                        localName,
                        localNameLower
                    )
                )
            }
        }
    }

    val operator = when (token) {
        is Token.Equal -> AttributeSelectorOperator.Equal
        is Token.IncludeMatch -> AttributeSelectorOperator.Includes
        is Token.DashMatch -> AttributeSelectorOperator.DashMatch
        is Token.PrefixMatch -> AttributeSelectorOperator.Prefix
        is Token.SubstringMatch -> AttributeSelectorOperator.Substring
        is Token.SuffixMatch -> AttributeSelectorOperator.Suffix
        else -> return Err(location.newError(SelectorParseErrorKind.UnexpectedTokenInAttributeSelector(token)))
    }

    val value = when (val value = input.expectIdentifierOrString()) {
        is Ok -> value.value
        is Err -> {
            val error = value.value

            return if (error.kind is ParseErrorKind.UnexpectedToken) {
                Err(value.value.location.newError(SelectorParseErrorKind.UnexpectedTokenInAttributeSelector(error.kind.token)))
            } else {
                value
            }
        }
    }

    val neverMatches = when (operator) {
        is AttributeSelectorOperator.Equal, is AttributeSelectorOperator.DashMatch -> false
        is AttributeSelectorOperator.Includes -> value.isEmpty() || value.contains(' ')
        is AttributeSelectorOperator.Prefix,
        is AttributeSelectorOperator.Substring,
        is AttributeSelectorOperator.Suffix -> value.isEmpty()
    }

    val caseSensitive = when (val flagResult = parseAttributeSelectorFlags(input)) {
        is Ok -> flagResult.value
        is Err -> return flagResult
    }

    val localNameLower = localName.toLowerCase()

    return if (namespace is Some) {
        Ok(
            Component.AttributeOther(
                namespace.value,
                localName,
                localNameLower,
                AttributeSelectorOperation.WithValue(
                    operator,
                    caseSensitive,
                    value
                ),
                neverMatches
            )
        )
    } else {
        Ok(
            Component.AttributeInNoNamespace(
                localName,
                localNameLower,
                operator,
                value,
                caseSensitive,
                neverMatches
            )
        )
    }
}

private fun parseAttributeSelectorFlags(input: Parser): Result<Boolean, ParseError> {
    val location = input.sourceLocation()

    val token = when (val token = input.next()) {
        is Ok -> token.value
        is Err -> return Ok(true)
    }

    return when (token) {
        is Token.Identifier -> {
            if (token.name.equals("i", true)) {
                Ok(false)
            } else {
                Err(location.newUnexpectedTokenError(token))
            }
        }
        else -> Err(location.newUnexpectedTokenError(token))
    }
}

private const val UPPER_EIGHT_BIT_MASK = 0xff shl 24

class AncestorHashes(val packedHashes: IntArray) {

    companion object {

        fun fromSelector(selector: Selector, quirksMode: QuirksMode): AncestorHashes {
            // compute the ancestor hashes lazily to prevent overhead
            val hashes = IntArray(4)
            AncestorIterator.fromSelector(selector)
                .asSequence()
                .mapNotNull { component -> component.ancestorHash(quirksMode) }
                .take(4)
                .forEachIndexed { index, ancestorHash -> hashes[index] = ancestorHash and HASH_BLOOM_MASK }

            // pack the fourth hash into the upper bytes of the first three
            val fourth = hashes[3]
            if (fourth != 0) {
                hashes[0] = hashes[0] or ((fourth and 0x000000ff) shl 24)
                hashes[1] = hashes[1] or ((fourth and 0x0000ff00) shl 16)
                hashes[2] = hashes[2] or ((fourth and 0x00ff0000) shl 8)
            }

            return AncestorHashes(
                intArrayOf(hashes[0], hashes[1], hashes[2])
            )
        }
    }

    fun fourthHash(): Int {
        return ((packedHashes[0] and UPPER_EIGHT_BIT_MASK) ushr 24) or
                ((packedHashes[0] and UPPER_EIGHT_BIT_MASK) ushr 16) or
                ((packedHashes[0] and UPPER_EIGHT_BIT_MASK) ushr 8)
    }
}

internal fun hashString(string: String): Int {
    return string.hashCode()
}

class AncestorIterator private constructor(private val iterator: SelectorIterator) : Iterator<Component> {

    companion object {

        fun fromSelector(selector: Selector): AncestorIterator {
            val iterator = selector.iterator()
            skipUntilAncestor(iterator)
            return AncestorIterator(iterator)
        }

        private fun skipUntilAncestor(iterator: SelectorIterator) {
            while (true) {
                // skip all component of this compound selector
                while (iterator.hasNext()) {
                    iterator.next()
                }

                // check whether there are any more compound selectors
                if (!iterator.hasNextSequence()) break

                val combinator = iterator.nextSequence()

                // stop if as soon as we reach an ancestor compound selector
                if (combinator is Combinator.Child || combinator is Combinator.Descendant) break
            }
        }
    }

    override fun hasNext(): Boolean {
        // evaluate if there are any remaining components in compound selector
        if (iterator.hasNext()) return true

        // advance the sequence and skip all sibling compound selectors
        if (!iterator.hasNextSequence()) return false
        val combinator = iterator.nextSequence()
        if (combinator !is Combinator.Child || combinator !is Combinator.Descendant) skipUntilAncestor(iterator)

        // reevaluate if there are any remaining components in compound selector
        return iterator.hasNext()
    }

    override fun next(): Component = iterator.next()
}
