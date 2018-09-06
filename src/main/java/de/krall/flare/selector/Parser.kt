package de.krall.flare.selector

import de.krall.flare.cssparser.Nth
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.SourceLocation
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newError
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.cssparser.parseNth
import de.krall.flare.std.iter.Iter
import de.krall.flare.style.parser.QuirksMode
import modern.std.Err
import modern.std.None
import modern.std.Ok
import modern.std.Option
import modern.std.Result
import modern.std.Some
import modern.std.let
import modern.std.mapOr

sealed class SelectorParseErrorKind : ParseErrorKind() {

    override fun toString(): String {
        return "SelectorParseErrorKind::${javaClass.simpleName}"
    }

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
    class UnexpectedTokenInAttributeSelector(val token: Token) : SelectorParseErrorKind()
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
        val compoundResult = parseCompoundSelector(context, input, builder)

        val compoundOption = when (compoundResult) {
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
            val tokenResult = input.nextIncludingWhitespace()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
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

private fun parseCompoundSelector(context: SelectorParserContext, input: Parser, builder: SelectorBuilder): Result<Option<ParseResult>, ParseError> {
    input.skipWhitespace()

    var empty = true
    var pseudo = false

    val typeSelectorResult = parseTypeSelector(context, input, builder::pushSimpleSelector)

    when (typeSelectorResult) {
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
        val selectorResult = parseOneSimpleSelector(context, input, false)

        val selector = when (selectorResult) {
            is Ok -> {
                val option = selectorResult.value

                if (option is Some) {
                    option.value
                } else {
                    break@loop
                }
            }
            is Err -> return selectorResult
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
                    var tokenResult = input.nextIncludingWhitespace()

                    var token = when (tokenResult) {
                        is Ok -> tokenResult.value
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
                    tokenResult = input.nextIncludingWhitespace()

                    token = when (tokenResult) {
                        is Ok -> tokenResult.value
                        is Err -> return tokenResult
                    }

                    when (token) {
                        is Token.Identifier -> {
                            val pseudoClassResult = parseNonTSPseudoClass(location, token.name)

                            when (pseudoClassResult) {
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

private fun parseTypeSelector(context: SelectorParserContext, input: Parser, sink: (Component) -> Unit): Result<Boolean, ParseError> {
    val qualifiedNameResult = parseQualifiedName(context, input, false)

    val qualifiedName = when (qualifiedNameResult) {
        is Ok -> qualifiedNameResult.value
        is Err -> {
            return if (input.isExhausted()) {
                Ok(false)
            } else {
                qualifiedNameResult
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
                    val defaultNamespace = context.defaultNamespace()
                    when (defaultNamespace) {
                        is Some -> {
                            sink(Component.DefaultNamespace(defaultNamespace.value))
                        }
                        is None -> {
                            sink(Component.ExplicitAnyNamespace)
                        }
                    }
                }
                is QualifiedNamePrefix.ExplicitNamespace -> {
                    val defaultNamespace = context.defaultNamespace()
                    when (defaultNamespace) {
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

private fun parseQualifiedName(context: SelectorParserContext, input: Parser, attributeSelector: Boolean): Result<QualifiedName, ParseError> {
    val state = input.state()

    val tokenResult = input.nextIncludingWhitespace()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> {
            input.reset(state)
            return tokenResult
        }
    }

    return when (token) {
        is Token.Identifier -> {
            val afterIdentState = input.state()

            val innerTokenResult = input.nextIncludingWhitespace()

            val innerToken = when (innerTokenResult) {
                is Ok -> tokenResult.value
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
                    val namespace = context.namespaceForPrefix(prefix)

                    when (namespace) {
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

            val innerTokenResult = input.nextIncludingWhitespace()

            val innerToken = when (innerTokenResult) {
                is Ok -> tokenResult.value
                is Err -> {
                    input.reset(afterAsteriskState)

                    return if (attributeSelector) {
                        innerTokenResult
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

private fun explicitNamespace(input: Parser, prefix: QualifiedNamePrefix, attributeSelector: Boolean): Result<QualifiedName, ParseError> {
    val location = input.sourceLocation()
    val tokenResult = input.nextIncludingWhitespace()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> return tokenResult
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
    val defaultNamespace = context.defaultNamespace()
    val namespace = when (defaultNamespace) {
        is Some -> QualifiedNamePrefix.ImplicitDefaultNamespace(defaultNamespace.value)
        is None -> QualifiedNamePrefix.ImplicitAnyNamespace
    }

    return Ok(QualifiedName.Some(namespace, name))
}

private sealed class SimpleSelectorParseResult {

    class SimpleSelector(val component: Component) : SimpleSelectorParseResult()

    class PseudoElement(val pseudoElement: de.krall.flare.selector.PseudoElement) : SimpleSelectorParseResult()
}

private fun parseOneSimpleSelector(context: SelectorParserContext, input: Parser, negated: Boolean): Result<Option<SimpleSelectorParseResult>, ParseError> {
    val state = input.state()
    val tokenResult = input.nextIncludingWhitespace()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
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
            val innerTokenResult = input.nextIncludingWhitespace()

            val innerToken = when (innerTokenResult) {
                is Ok -> innerTokenResult.value
                is Err -> return innerTokenResult
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
            val attributeSelector = input.parseNestedBlock { parseAttributeSelector(context, it) }

            return when (attributeSelector) {
                is Ok -> Ok(Some(SimpleSelectorParseResult.SimpleSelector(attributeSelector.value)))
                is Err -> attributeSelector
            }
        }
        is Token.Colon -> {
            val location = input.sourceLocation()
            var innerTokenResult = input.nextIncludingWhitespace()

            var innerToken = when (innerTokenResult) {
                is Ok -> innerTokenResult.value
                is Err -> return innerTokenResult
            }

            val doubleColon = when (innerToken) {
                is Token.Colon -> {
                    innerTokenResult = input.nextIncludingWhitespace()

                    innerToken = when (innerTokenResult) {
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
        "-flr-tab-area" -> Ok(PseudoElement.FlareTabArea)
        "-flr-tab" -> Ok(PseudoElement.FlareTab)
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
    val nthResult = parseNth(input)

    return when (nthResult) {
        is Ok -> Ok(wrapper(nthResult.value))
        is Err -> nthResult
    }
}

private fun parseNonTSFunctionalPseudoClass(input: Parser, location: SourceLocation, name: String): Result<NonTSPseudoClass, ParseError> {
    return when (name.toLowerCase()) {
        "lang" -> {
            val identifierResult = input.expectIdentifier()

            return when (identifierResult) {
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
        "only-type" -> Ok(Component.OnlyType)
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

    val typeSelectorResult = parseTypeSelector(context, input) { simpleSelector.add(it) }

    val parsed = when (typeSelectorResult) {
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
        val selectorResult = parseOneSimpleSelector(context, input, true)

        val selector = when (selectorResult) {
            is Ok -> {
                val option = selectorResult.value

                when (option) {
                    is Some -> option.value
                    is None -> return Err(input.newError(SelectorParseErrorKind.EmptyNegation))
                }
            }
            is Err -> return selectorResult
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
    val qualifiedNameResult = parseQualifiedName(context, input, true)

    val qualifiedName = when (qualifiedNameResult) {
        is Ok -> qualifiedNameResult.value
        is Err -> return qualifiedNameResult
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
    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> {
            val localNameLower = localName.toLowerCase()
            return if (namespace is Some) {
                Ok(Component.AttributeOther(
                        namespace.value,
                        localName,
                        localNameLower,
                        AttributeSelectorOperation.Exists,
                        false
                ))
            } else {
                Ok(Component.AttributeInNoNamespaceExists(
                        localName,
                        localNameLower
                ))
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

    val valueResult = input.expectIdentifierOrString()

    val value = when (valueResult) {
        is Ok -> valueResult.value
        is Err -> {
            val error = valueResult.value

            return if (error.kind is ParseErrorKind.UnexpectedToken) {
                Err(valueResult.value.location.newError(SelectorParseErrorKind.UnexpectedTokenInAttributeSelector(error.kind.token)))
            } else {
                valueResult
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

    val flagResult = parseAttributeSelectorFlags(input)

    val caseSensitive = when (flagResult) {
        is Ok -> flagResult.value
        is Err -> return flagResult
    }

    val localNameLower = localName.toLowerCase()

    return if (namespace is Some) {
        Ok(Component.AttributeOther(
                namespace.value,
                localName,
                localNameLower,
                AttributeSelectorOperation.WithValue(
                        operator,
                        caseSensitive,
                        value
                ),
                neverMatches
        ))
    } else {
        Ok(Component.AttributeInNoNamespace(
                localName,
                localNameLower,
                operator,
                value,
                caseSensitive,
                neverMatches
        ))
    }
}

private fun parseAttributeSelectorFlags(input: Parser): Result<Boolean, ParseError> {
    val location = input.sourceLocation()
    val tokenResult = input.next()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
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

fun main(args: Array<String>) {
    val fourth = 10239454 and HASH_BLOOM_MASK

    val o = 23403243 and HASH_BLOOM_MASK
    val a1 = o or ((fourth and 0x000000ff) shl 24)
    val a2 = o or ((fourth and 0x0000ff00) shl 16)
    val a3 = o or ((fourth and 0x00ff0000) shl 8)

    println(fourth)

    val b = ((a1 and UPPER_EIGHT_BIT_MASK) ushr 24) or
            ((a2 and UPPER_EIGHT_BIT_MASK) ushr 16) or
            ((a3 and UPPER_EIGHT_BIT_MASK) ushr 8)

    println(b)
}

private const val UPPER_EIGHT_BIT_MASK = 0xff shl 24

class AncestorHashes(val packedHashes: IntArray) {

    companion object {

        fun new(selector: Selector, quirksMode: QuirksMode): AncestorHashes {
            return fromIter(selector.iter(), quirksMode)
        }

        private fun fromIter(selector: SelectorIter, quirksMode: QuirksMode): AncestorHashes {
            val iter = AncestorIter.new(selector).filterMap { component -> component.ancestorHash(quirksMode) }

            val hashes = IntArray(4)

            loop@
            for (i in 0..4) {
                val hash = iter.next()
                when (hash) {
                    is Some -> hashes[i] = hash.value and HASH_BLOOM_MASK
                    is None -> break@loop
                }
            }

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

class AncestorIter private constructor(private val iter: SelectorIter) : Iter<Component> {

    companion object {
        fun new(selector: SelectorIter): AncestorIter {
            val iter = AncestorIter(selector)
            iter.skipUntilAncestor()
            return iter
        }
    }

    private fun skipUntilAncestor() {
        while (true) {
            while (iter.next().isSome()) {
            }

            val ancestor = iter.nextSequence().mapOr({ combinator ->
                combinator is Combinator.Child || combinator is Combinator.Descendant
            }, true)

            if (ancestor) {
                break
            }
        }
    }

    override fun next(): Option<Component> {
        val next = iter.next()
        if (next is Some) {
            return next
        }

        val combinator = iter.nextSequence()
        if (combinator is Some) {
            when (combinator.value) {
                is Combinator.Child,
                is Combinator.Descendant -> skipUntilAncestor()
                else -> {
                }
            }
        }

        return iter.next()
    }

    override fun clone(): Iter<Component> {
        return AncestorIter(iter.clone())
    }
}
