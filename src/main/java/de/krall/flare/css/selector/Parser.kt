package de.krall.flare.css.selector

import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import de.krall.flare.css.selector.PseudoElement as SelectorPseudoElement

sealed class SelectorParseErrorKind : ParseErrorKind() {

    override fun toString(): String {
        return "SelectorParseErrorKind::${javaClass.simpleName}"
    }

    class UnknownSelector : SelectorParseErrorKind()
    class DanglingCombinator : SelectorParseErrorKind()
    class PseudoElementExpectedColon : SelectorParseErrorKind()
    class NoIdentifierForPseudo : SelectorParseErrorKind()
    class ExpectedNamespace : SelectorParseErrorKind()
    class ExpectedBarAttributeSelector : SelectorParseErrorKind()
    class InvalidQualifiedNameInAttributeSelector : SelectorParseErrorKind()
    class ExplicitNamespaceUnexpectedToken : SelectorParseErrorKind()
    class ClassNeedsIdentifier : SelectorParseErrorKind()
    class PseudoNeedsIdentifier : SelectorParseErrorKind()
    class EmptyNegation : SelectorParseErrorKind()
    class NonSimpleSelectorInNegation : SelectorParseErrorKind()
}

interface SelectorParserContext {

    fun pseudoElementAllowsSingleColon(name: String): Boolean

    fun defaultNamespace(): Option<NamespaceUrl>

    fun namespacePrefix(prefix: String): NamespacePrefix

    fun namespaceForPrefix(prefix: NamespacePrefix): Option<NamespaceUrl>
}

interface NamespacePrefix {

    fun getPrefix(): String
}

interface NamespaceUrl {

    fun getUrl(): String
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
                Err(input.newError(SelectorParseErrorKind.DanglingCombinator()))
            } else {
                Err(input.newError(SelectorParseErrorKind.UnknownSelector()))
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
                    combinator = Combinator.Child()
                    break@inner
                }
                is Token.Plus -> {
                    combinator = Combinator.NextSibling()
                    break@inner
                }
                is Token.Tidle -> {
                    combinator = Combinator.LaterSibling()
                    break@inner
                }
                else -> {
                    input.reset(state)
                    if (seenWhitespace) {
                        combinator = Combinator.Descendant()
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
                if (selectorResult.value is Some) {
                    selectorResult.value.value
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
                            return Err(location.newError(SelectorParseErrorKind.PseudoElementExpectedColon()))
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
                            return Err(location.newError(SelectorParseErrorKind.NoIdentifierForPseudo()))
                        }
                    }
                }

                if (!builder.isEmpty()) {
                    builder.pushCombinator(Combinator.PseudoElement())
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
        Ok(None())
    } else {
        Ok(Some(ParseResult(pseudo)))
    }
}

private sealed class QualifiedNamePrefix {

    class ImplicitNoNamespace : QualifiedNamePrefix()

    class ImplicitAnyNamespace : QualifiedNamePrefix()

    class ImplicitDefaultNamespace(val url: NamespaceUrl) : QualifiedNamePrefix()

    class ExplicitNoNamespace : QualifiedNamePrefix()

    class ExplicitAnyNamespace : QualifiedNamePrefix()

    class ExplicitNamespace(val prefix: NamespacePrefix, val url: NamespaceUrl) : QualifiedNamePrefix()
}

private sealed class QualifiedName {

    class None : QualifiedName()

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
                    sink(Component.ExplicitNoNamespace())
                }
                is QualifiedNamePrefix.ExplicitNoNamespace -> {
                    sink(Component.ExplicitNoNamespace())
                }
                is QualifiedNamePrefix.ExplicitAnyNamespace -> {
                    val defaultNamespace = context.defaultNamespace()
                    when (defaultNamespace) {
                        is Some -> {
                            sink(Component.DefaultNamespace(defaultNamespace.value))
                        }
                        is None -> {
                            sink(Component.ExplicitAnyNamespace())
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
            }

            when (qualifiedName.localName) {
                is Some -> {
                    sink(Component.LocalName(qualifiedName.localName.value))
                }
                is None -> {
                    sink(Component.ExplicitUniversalType())
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
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace(), Some(token.name)))
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
                            Err(afterIdentState.sourceLocation().newError(SelectorParseErrorKind.ExpectedNamespace()))
                        }
                    }
                }
                else -> {
                    input.reset(afterIdentState)
                    if (attributeSelector) {
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace(), Some(token.name)))
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
                        defaultNamespace(context, None())
                    }
                }
            }

            when (innerToken) {
                is Token.Pipe -> {
                    explicitNamespace(input, QualifiedNamePrefix.ExplicitAnyNamespace(), attributeSelector)
                }
                else -> {
                    input.reset(afterAsteriskState)

                    if (attributeSelector) {
                        Err(afterAsteriskState.sourceLocation().newError(SelectorParseErrorKind.ExpectedBarAttributeSelector()))
                    } else {
                        defaultNamespace(context, None())
                    }
                }
            }
        }
        is Token.Pipe -> {
            explicitNamespace(input, QualifiedNamePrefix.ExplicitNoNamespace(), attributeSelector)
        }
        else -> {
            input.reset(state)
            Ok(QualifiedName.None())
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
                Ok(QualifiedName.Some(prefix, None()))
            } else {
                Err(location.newError(SelectorParseErrorKind.InvalidQualifiedNameInAttributeSelector()))
            }
        }
        else -> {
            if (attributeSelector) {
                Err(location.newError(SelectorParseErrorKind.InvalidQualifiedNameInAttributeSelector()))
            } else {
                Err(location.newError(SelectorParseErrorKind.ExplicitNamespaceUnexpectedToken()))
            }
        }
    }
}

private fun defaultNamespace(context: SelectorParserContext, name: Option<String>): Result<QualifiedName, ParseError> {
    val defaultNamespace = context.defaultNamespace()
    val namespace = when (defaultNamespace) {
        is Some -> QualifiedNamePrefix.ImplicitDefaultNamespace(defaultNamespace.value)
        is None -> QualifiedNamePrefix.ImplicitAnyNamespace()
    }

    return Ok(QualifiedName.Some(namespace, name))
}

private sealed class SimpleSelectorParseResult {

    class SimpleSelector(val component: Component) : SimpleSelectorParseResult()

    class PseudoElement(val pseudoElement: SelectorPseudoElement) : SimpleSelectorParseResult()
}

private fun parseOneSimpleSelector(context: SelectorParserContext, input: Parser, negated: Boolean): Result<Option<SimpleSelectorParseResult>, ParseError> {
    val state = input.state()
    val tokenResult = input.nextIncludingWhitespace()

    val token = when (tokenResult) {
        is Ok -> tokenResult.value
        is Err -> {
            input.reset(state)
            return Ok(None())
        }
    }

    when (token) {
        is Token.IdHash -> {
            val component = Component.Id(token.value)

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
                    Err(location.newError(SelectorParseErrorKind.ClassNeedsIdentifier()))
                }
            }
        }
        is Token.LBracket -> {
            return Err(input.newUnexpectedTokenError(token))
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
                    return Err(location.newError(SelectorParseErrorKind.PseudoNeedsIdentifier()))
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
            return Ok(None())
        }
    }
}

private fun parseFunctionalPseudoElement(input: Parser, location: SourceLocation, name: String): Result<SelectorPseudoElement, ParseError> {
    return Err(location.newUnexpectedTokenError(Token.Function(name)))
}

private fun parsePseudoElement(location: SourceLocation, name: String): Result<SelectorPseudoElement, ParseError> {
    return when (name.toLowerCase()) {
        "before" -> Ok(SelectorPseudoElement.Before())
        "after" -> Ok(SelectorPseudoElement.After())
        "selection" -> Ok(SelectorPseudoElement.Selection())
        "first-letter" -> Ok(SelectorPseudoElement.FirstLetter())
        "first-line" -> Ok(SelectorPseudoElement.FirstLine())
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseFunctionalPseudoClass(context: SelectorParserContext, input: Parser, location: SourceLocation, name: String, negated: Boolean): Result<Component, ParseError> {
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
        "first-child" -> Ok(Component.FirstChild())
        "last-child" -> Ok(Component.LastChild())
        "only-child" -> Ok(Component.OnlyChild())
        "first-of-type" -> Ok(Component.FirstOfType())
        "last-of-type" -> Ok(Component.LastOfType())
        "only-type" -> Ok(Component.OnlyType())
        "root" -> Ok(Component.Root())
        "empty" -> Ok(Component.Empty())
        "scope" -> Ok(Component.Scope())
        "host" -> Ok(Component.Host())
        else -> parseNonTSPseudoClass(location, name).map(Component::NonTSPseudoClass)
    }
}

private fun parseNonTSPseudoClass(location: SourceLocation, name: String): Result<NonTSPseudoClass, ParseError> {
    return when (name.toLowerCase()) {
        "active" -> Ok(NonTSPseudoClass.Active())
        "checked" -> Ok(NonTSPseudoClass.Checked())
        "disabled" -> Ok(NonTSPseudoClass.Disabled())
        "enabled" -> Ok(NonTSPseudoClass.Enabled())
        "focus" -> Ok(NonTSPseudoClass.Focus())
        "fullscreen" -> Ok(NonTSPseudoClass.Fullscreen())
        "hover" -> Ok(NonTSPseudoClass.Hover())
        "indeterminate" -> Ok(NonTSPseudoClass.Indeterminate())
        "link" -> Ok(NonTSPseudoClass.Link())
        "placeholder-shown" -> Ok(NonTSPseudoClass.PlaceholderShown())
        "read-write" -> Ok(NonTSPseudoClass.ReadWrite())
        "read-only" -> Ok(NonTSPseudoClass.ReadOnly())
        "target" -> Ok(NonTSPseudoClass.Target())
        "visited" -> Ok(NonTSPseudoClass.Visited())
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseNegation(context: SelectorParserContext, input: Parser): Result<Component, ParseError> {
    val simpleSelector = mutableListOf<Component>()

    input.skipWhitespace()

    val typeSelectorResult = parseTypeSelector(context, input, { simpleSelector.add(it) })

    val parsed = when (typeSelectorResult) {
        is Err -> {
            return if (typeSelectorResult.value.kind == ParseErrorKind.EndOfFile()) {
                Err(input.newError(SelectorParseErrorKind.EmptyNegation()))
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
                when (selectorResult.value) {
                    is Some -> selectorResult.value.value
                    is None -> return Err(input.newError(SelectorParseErrorKind.EmptyNegation()))
                }
            }
            is Err -> return selectorResult
        }

        when (selector) {
            is SimpleSelectorParseResult.SimpleSelector -> {
                simpleSelector.add(selector.component)
            }
            is SimpleSelectorParseResult.PseudoElement -> {
                return Err(input.newError(SelectorParseErrorKind.NonSimpleSelectorInNegation()))
            }
        }
    }

    return Ok(Component.Negation(simpleSelector))
}

/*
fun test(): String? {
    val result = parseQualifiedName()

    val localName: String
    val namespace: Option<NamespaceConstraint>

    when (result) {
        is QualifiedName.Some -> {
            localName = when (result.localName) {
                is Option.None -> {
                    throw IllegalStateException("unreachable")
                }
                is Option.Some -> {
                    result.localName.value
                }
            }

            val prefix = result.prefix
            namespace = when (prefix) {
                is QualifiedNamePrefix.ImplicitNoNamespace, is QualifiedNamePrefix.ExplicitNoNamespace -> {
                    Option.None()
                }
                is QualifiedNamePrefix.ExplicitNamespace -> {
                    Some(NamespaceConstraint.Specific(prefix.prefix, prefix.url))
                }
                is QualifiedNamePrefix.ExplicitAnyNamespace -> Option.Some(NamespaceConstraint.Any())
                is QualifiedNamePrefix.ImplicitAnyNamespace, is QualifiedNamePrefix.ImplicitDefaultNamespace -> {
                    throw IllegalStateException("unreachable")
                }
            }
        }
    }

    return null
}

private fun parseQualifiedName(): QualifiedName {
    return QualifiedName.None()
}
        */