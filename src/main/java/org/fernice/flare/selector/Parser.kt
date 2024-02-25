/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.cssparser.*
import org.fernice.flare.style.QuirksMode
import org.fernice.std.*

/**
 * The selector specific error kinds
 */
sealed class SelectorParseErrorKind : ParseErrorKind() {

    data object EmptySelector : SelectorParseErrorKind()
    data object DanglingCombinator : SelectorParseErrorKind()
    data object PseudoElementExpectedColon : SelectorParseErrorKind()
    data object NoIdentifierForPseudo : SelectorParseErrorKind()
    data object ExpectedNamespace : SelectorParseErrorKind()
    data object ExpectedBarAttributeSelector : SelectorParseErrorKind()
    data object InvalidQualifiedNameInAttributeSelector : SelectorParseErrorKind()
    data object ExplicitNamespaceUnexpectedToken : SelectorParseErrorKind()
    data object ClassNeedsIdentifier : SelectorParseErrorKind()
    data object PseudoNeedsIdentifier : SelectorParseErrorKind()
    data object EmptyNegation : SelectorParseErrorKind()
    data object NonSimpleSelectorInNegation : SelectorParseErrorKind()
    data object NoQualifiedNameInAttributeSelector : SelectorParseErrorKind()
    data class UnexpectedTokenInAttributeSelector(val token: Token) : SelectorParseErrorKind()
    data object InvalidState : SelectorParseErrorKind()

    override fun toString(): String {
        return "SelectorParseErrorKind::${javaClass.simpleName}"
    }
}

interface SelectorParserContext {
    fun defaultNamespace(): NamespaceUrl?
    fun namespacePrefix(prefix: String): NamespacePrefix
    fun namespaceForPrefix(prefix: NamespacePrefix): NamespaceUrl?
}

class SelectorParsingState(value: UByte) : U8Bitflags(value) {

    override val all: UByte get() = ALL

    operator fun plus(value: UByte): SelectorParsingState = of(this.value or value)
    operator fun minus(value: UByte): SelectorParsingState = of(this.value and value.inv())

    fun allowsPseudos(): Boolean = !intersects(AFTER_PSEUDO_ELEMENT or DISALLOW_PSEUDOS)
    fun allowsSlotted(): Boolean = !intersects(AFTER_PSEUDO or DISALLOW_PSEUDOS)
    fun allowsPart(): Boolean = !intersects(AFTER_PSEUDO or DISALLOW_PSEUDOS)
    fun allowsCustomFunctionalPseudoClasses(): Boolean = !intersects(AFTER_PSEUDO)
    fun allowsNonFunctionalPseudoClasses(): Boolean = !intersects(AFTER_SLOTTED or AFTER_NON_STATEFUL_PSEUDO_ELEMENT)
    fun allowsTreeStructuralPseudoClasses(): Boolean = !intersects(AFTER_PSEUDO)
    fun allowsCombinators(): Boolean = !intersects(DISALLOW_COMBINATORS)

    companion object {
        const val SKIP_DEFAULT_NAMESPACE: UByte = 0b0000_0001u
        const val AFTER_SLOTTED: UByte = 0b0000_0010u
        const val AFTER_PART: UByte = 0b0000_0100u
        const val AFTER_PSEUDO_ELEMENT: UByte = 0b0000_1000u
        const val AFTER_NON_STATEFUL_PSEUDO_ELEMENT: UByte = 0b0001_0000u
        val AFTER_PSEUDO: UByte = AFTER_PART or AFTER_SLOTTED or AFTER_PSEUDO_ELEMENT

        const val DISALLOW_COMBINATORS: UByte = 0b0010_0000u
        const val DISALLOW_PSEUDOS: UByte = 0b0100_0000u
        const val DISALLOW_RELATIVE_SELECTORS: UByte = 0b1000_0000u

        private val ALL: UByte = SKIP_DEFAULT_NAMESPACE or AFTER_SLOTTED or AFTER_PART or AFTER_PSEUDO_ELEMENT or AFTER_NON_STATEFUL_PSEUDO_ELEMENT or
                DISALLOW_COMBINATORS or DISALLOW_PSEUDOS or DISALLOW_RELATIVE_SELECTORS

        fun empty(): SelectorParsingState = SelectorParsingState(0u)
        fun all(): SelectorParsingState = SelectorParsingState(ALL)
        fun of(value: UByte): SelectorParsingState = SelectorParsingState(value and ALL)
    }
}

enum class ParseForgiving {
    No,
    Yes,
}

enum class ParseRelative {
    No,
    ForNesting,
    ForHas,
}

private fun parseInnerCompoundSelector(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
): Result<Selector, ParseError> {
    return parseSelector(
        context,
        input,
        state + SelectorParsingState.DISALLOW_PSEUDOS + SelectorParsingState.DISALLOW_COMBINATORS,
        ParseRelative.No,
    )
}

internal fun parseSelector(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
    parseRelative: ParseRelative,
): Result<Selector, ParseError> {
    val builder = SelectorBuilder()

    input.skipWhitespace()

    if (parseRelative != ParseRelative.No) {
        val combinator = tryParseCombinator(input)
        when (parseRelative) {
            ParseRelative.ForHas -> {
                builder.pushSimpleSelector(Component.RelativeSelectorAnchor)
                builder.pushCombinator(combinator.unwrapOr(Combinator.Descendant))
            }

            ParseRelative.ForNesting -> {
                combinator.ifOk {
                    builder.pushSimpleSelector(Component.ParentSelector)
                    builder.pushCombinator(it)
                }
            }

            else -> error("unreachable")
        }
    }

    while (true) {
        val empty = when (val compoundResult = parseCompoundSelector(context, input, builder, state)) {
            is Ok -> compoundResult.value
            is Err -> return compoundResult
        }

        if (empty) {
            return if (builder.hasDanglingCombinator()) {
                Err(input.newError(SelectorParseErrorKind.DanglingCombinator))
            } else {
                Err(input.newError(SelectorParseErrorKind.EmptySelector))
            }
        }

        if (state.intersects(SelectorParsingState.AFTER_PSEUDO)) {
            break
        }

        val combinator = when (val result = tryParseCombinator(input)) {
            is Ok -> result.value
            is Err -> break
        }

        if (!state.allowsCombinators()) {
            return Err(input.newError(SelectorParseErrorKind.InvalidState))
        }

        builder.pushCombinator(combinator)
    }

    return Ok(builder.build())
}

private fun tryParseCombinator(input: Parser): Result<Combinator, Unit> {
    var seenWhitespace = false
    while (true) {
        val state = input.state()
        val token = when (val token = input.nextIncludingWhitespace()) {
            is Ok -> token.value
            is Err -> return Err()
        }

        when (token) {
            is Token.Whitespace -> {
                seenWhitespace = true
            }

            is Token.Gt -> return Ok(Combinator.Child)
            is Token.Plus -> return Ok(Combinator.NextSibling)
            is Token.Tidle -> return Ok(Combinator.LaterSibling)
            else -> {
                input.reset(state)
                if (seenWhitespace) return Ok(Combinator.Descendant)
                return Err()
            }
        }
    }
}

private fun parseCompoundSelector(
    context: SelectorParserContext,
    input: Parser,
    builder: SelectorBuilder,
    state: SelectorParsingState,
): Result<Boolean, ParseError> {
    input.skipWhitespace()

    var empty = true

    if (parseTypeSelector(context, input, builder::pushSimpleSelector).unwrap { return it }) {
        empty = false
    }

    while (true) {
        val result = parseOneSimpleSelector(context, input, state).unwrap { return it } ?: break

        if (empty) {
            context.defaultNamespace()?.let {
                val ignoreDefaultNamespace = state.intersects(SelectorParsingState.SKIP_DEFAULT_NAMESPACE)
                        || result is SimpleSelectorParseResult.SimpleSelector && result.component is Component.Host

                if (!ignoreDefaultNamespace) {
                    builder.pushSimpleSelector(Component.DefaultNamespace(it))
                }
            }
        }

        empty = false

        when (result) {
            is SimpleSelectorParseResult.SimpleSelector -> {
                builder.pushSimpleSelector(result.component)
            }

            is SimpleSelectorParseResult.PartPseudo -> {
                state.add(SelectorParsingState.AFTER_PART)
                builder.pushCombinator(Combinator.Part)
                builder.pushSimpleSelector(Component.Part(result.names))
            }

            is SimpleSelectorParseResult.SlottedPseudo -> {
                state.add(SelectorParsingState.AFTER_SLOTTED)
                builder.pushCombinator(Combinator.SlotAssignment)
                builder.pushSimpleSelector(Component.Slotted(result.selector))
            }

            is SimpleSelectorParseResult.PseudoElement -> {
                state.add(SelectorParsingState.AFTER_PSEUDO_ELEMENT)
                if (!result.pseudoElement.acceptsStatePseudoClasses()) {
                    state.add(SelectorParsingState.AFTER_NON_STATEFUL_PSEUDO_ELEMENT)
                }
                builder.pushCombinator(Combinator.PseudoElement)
                builder.pushSimpleSelector(Component.PseudoElement(result.pseudoElement))
            }
        }
    }

    return Ok(empty)
}

private sealed class QualifiedNamePrefix {

    data object ImplicitNoNamespace : QualifiedNamePrefix()

    data object ImplicitAnyNamespace : QualifiedNamePrefix()

    data class ImplicitDefaultNamespace(val url: NamespaceUrl) : QualifiedNamePrefix()

    data object ExplicitNoNamespace : QualifiedNamePrefix()

    data object ExplicitAnyNamespace : QualifiedNamePrefix()

    data class ExplicitNamespace(val prefix: NamespacePrefix, val url: NamespaceUrl) : QualifiedNamePrefix()
}

private sealed class QualifiedName {

    data object None : QualifiedName()

    data class Some(val prefix: QualifiedNamePrefix, val localName: String?) : QualifiedName()
}

private fun parseTypeSelector(
    context: SelectorParserContext,
    input: Parser,
    sink: (Component) -> Unit,
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
                is QualifiedNamePrefix.ImplicitDefaultNamespace -> sink(Component.ExplicitNoNamespace)
                is QualifiedNamePrefix.ExplicitNoNamespace -> sink(Component.ExplicitNoNamespace)
                is QualifiedNamePrefix.ExplicitAnyNamespace -> {
                    when (val defaultNamespace = context.defaultNamespace()) {
                        null -> sink(Component.ExplicitAnyNamespace)
                        else -> sink(Component.DefaultNamespace(defaultNamespace))
                    }
                }

                is QualifiedNamePrefix.ExplicitNamespace -> {
                    when (val defaultNamespace = context.defaultNamespace()) {
                        null -> sink(Component.DefaultNamespace(qualifiedName.prefix.url))
                        else -> {
                            if (defaultNamespace == qualifiedName.prefix.url) {
                                sink(Component.DefaultNamespace(qualifiedName.prefix.url))
                            } else {
                                sink(Component.Namespace(qualifiedName.prefix.prefix, qualifiedName.prefix.url))
                            }
                        }
                    }
                }

                else -> {}
            }

            when (qualifiedName.localName) {
                null -> sink(Component.ExplicitUniversalType)
                else -> {
                    val localName = qualifiedName.localName
                    val localNameLower = localName.lowercase()

                    sink(Component.LocalName(localName, localNameLower))
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
    attributeSelector: Boolean,
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
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace, token.name))
                    } else {
                        defaultNamespace(context, token.name)
                    }
                }
            }

            when (innerToken) {
                is Token.Pipe -> {
                    val prefix = context.namespacePrefix(token.name)

                    when (val namespace = context.namespaceForPrefix(prefix)) {
                        null -> {
                            Err(afterIdentState.location().newError(SelectorParseErrorKind.ExpectedNamespace))
                        }

                        else -> {
                            explicitNamespace(
                                input,
                                QualifiedNamePrefix.ExplicitNamespace(prefix, namespace),
                                attributeSelector
                            )
                        }
                    }
                }

                else -> {
                    input.reset(afterIdentState)
                    if (attributeSelector) {
                        Ok(QualifiedName.Some(QualifiedNamePrefix.ImplicitNoNamespace, token.name))
                    } else {
                        defaultNamespace(context, token.name)
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
                        defaultNamespace(context, null)
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
                        defaultNamespace(context, null)
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
    attributeSelector: Boolean,
): Result<QualifiedName, ParseError> {
    val location = input.sourceLocation()

    val token = when (val token = input.nextIncludingWhitespace()) {
        is Ok -> token.value
        is Err -> return token
    }

    return when (token) {
        is Token.Identifier -> {
            Ok(QualifiedName.Some(prefix, token.name))
        }

        is Token.Asterisk -> {
            if (!attributeSelector) {
                Ok(QualifiedName.Some(prefix, null))
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

private fun defaultNamespace(context: SelectorParserContext, name: String?): Result<QualifiedName, ParseError> {
    val namespace = when (val namespace = context.defaultNamespace()) {
        null -> QualifiedNamePrefix.ImplicitAnyNamespace
        else -> QualifiedNamePrefix.ImplicitDefaultNamespace(namespace)
    }

    return Ok(QualifiedName.Some(namespace, name))
}

private sealed class SimpleSelectorParseResult {

    data class SimpleSelector(val component: Component) : SimpleSelectorParseResult()
    data class PartPseudo(val names: List<String>) : SimpleSelectorParseResult()
    data class SlottedPseudo(val selector: Selector) : SimpleSelectorParseResult()
    data class PseudoElement(val pseudoElement: org.fernice.flare.selector.PseudoElement) : SimpleSelectorParseResult()
}

private fun parseOneSimpleSelector(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
): Result<SimpleSelectorParseResult?, ParseError> {
    val parseState = input.state()
    val token = when (val token = input.nextIncludingWhitespace()) {
        is Ok -> token.value
        is Err -> {
            input.reset(parseState)
            return Ok(null)
        }
    }

    when (token) {
        is Token.IdHash -> {
            if (state.intersects(SelectorParsingState.AFTER_PSEUDO)) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }

            val component = Component.ID(token.value)

            return Ok(SimpleSelectorParseResult.SimpleSelector(component))
        }

        is Token.Ampersand -> {
            if (state.intersects(SelectorParsingState.AFTER_PSEUDO)) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }

            return Ok(SimpleSelectorParseResult.SimpleSelector(Component.ParentSelector))
        }

        is Token.Dot -> {
            if (state.intersects(SelectorParsingState.AFTER_PSEUDO)) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }

            val location = input.sourceLocation()

            return when (val nextToken = input.nextIncludingWhitespace().unwrap { return it }) {
                is Token.Identifier -> {
                    val component = Component.Class(nextToken.name)

                    Ok(SimpleSelectorParseResult.SimpleSelector(component))
                }

                else -> {
                    Err(location.newError(SelectorParseErrorKind.ClassNeedsIdentifier))
                }
            }
        }

        is Token.LBracket -> {
            if (state.intersects(SelectorParsingState.AFTER_PSEUDO)) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }
            val attributeSelector = input.parseNestedBlock { parseAttributeSelector(context, it) }.unwrap { return it }
            return Ok(SimpleSelectorParseResult.SimpleSelector(attributeSelector))
        }

        is Token.Colon -> {
            val location = input.sourceLocation()

            val (doubleColon, nextToken) = when (val t = input.nextIncludingWhitespace().unwrap { return it }) {
                is Token.Colon -> true to input.nextIncludingWhitespace().unwrap { return it }
                else -> false to t
            }

            val (name, functional) = when (nextToken) {
                is Token.Identifier -> nextToken.name to false
                is Token.Function -> nextToken.name to true
                else -> return Err(location.newError(SelectorParseErrorKind.PseudoNeedsIdentifier))
            }

            return if (doubleColon || isCSS2PseudoElement(name)) {
                if (!state.allowsPseudos()) {
                    return Err(location.newError(SelectorParseErrorKind.InvalidState))
                }

                val pseudoElement = if (functional) {
                    if (name.equals("part", ignoreCase = true)) {
                        if (!state.allowsPart()) {
                            return Err(location.newError(SelectorParseErrorKind.InvalidState))
                        }

                        val names = input.parseNestedBlock { nestedInput ->
                            val elements = mutableListOf<String>()
                            elements.add(nestedInput.expectIdentifier().unwrap { return@parseNestedBlock it })
                            while (!nestedInput.isExhausted()) {
                                elements.add(nestedInput.expectIdentifier().unwrap { return@parseNestedBlock it })
                            }
                            Ok(elements.resized())
                        }.unwrap { return it }

                        return Ok(SimpleSelectorParseResult.PartPseudo(names))
                    }
                    if (name.equals("slotted", ignoreCase = true)) {
                        if (!state.allowsSlotted()) {
                            return Err(location.newError(SelectorParseErrorKind.InvalidState))
                        }

                        val selector = input.parseNestedBlock { nestedInput ->
                            parseInnerCompoundSelector(context, nestedInput, state)
                        }.unwrap { return it }

                        return Ok(SimpleSelectorParseResult.SlottedPseudo(selector))
                    }

                    input.parseNestedBlock { parseFunctionalPseudoElement(it, location, name) }.unwrap { return it }
                } else {
                    parsePseudoElement(location, name).unwrap { return it }
                }

                if (state.intersects(SelectorParsingState.AFTER_SLOTTED) && !pseudoElement.validAfterSlotted()) {
                    return Err(location.newError(SelectorParseErrorKind.InvalidState))
                }

                Ok(SimpleSelectorParseResult.PseudoElement(pseudoElement))
            } else {
                val pseudoClass = if (functional) {
                    input.parseNestedBlock { parseFunctionalPseudoClass(context, it, location, name, state) }.unwrap { return it }
                } else {
                    parsePseudoClass(location, name, state).unwrap { return it }
                }

                Ok(SimpleSelectorParseResult.SimpleSelector(pseudoClass))
            }
        }

        else -> {
            input.reset(parseState)
            return Ok(null)
        }
    }
}

private fun isCSS2PseudoElement(name: String): Boolean {
    return when (name) {
        "before",
        "after",
        "first-letter",
        "first-line",
        -> true

        else -> false
    }
}

private fun parseFunctionalPseudoElement(
    @Suppress("UNUSED_PARAMETER") input: Parser,
    location: SourceLocation,
    name: String,
): Result<PseudoElement, ParseError> {
    return Err(location.newUnexpectedTokenError(Token.Function(name)))
}

private fun parsePseudoElement(location: SourceLocation, name: String): Result<PseudoElement, ParseError> {
    return when (name.lowercase()) {
        "before" -> Ok(PseudoElement.Before)
        "after" -> Ok(PseudoElement.After)
        "selection" -> Ok(PseudoElement.Selection)
        "first-letter" -> Ok(PseudoElement.FirstLetter)
        "first-line" -> Ok(PseudoElement.FirstLine)
        "placeholder" -> Ok(PseudoElement.Placeholder)
        "icon" -> Ok(PseudoElement.Flare_Icon)
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseFunctionalPseudoClass(
    context: SelectorParserContext,
    input: Parser,
    location: SourceLocation,
    name: String,
    state: SelectorParsingState,
): Result<Component, ParseError> {
    return when (name.lowercase()) {
        "nth-child" -> parseNthPseudoClass(context, input, state, NthType.Child)
        "nth-of-type" -> parseNthPseudoClass(context, input, state, NthType.OfType)
        "nth-last-child" -> parseNthPseudoClass(context, input, state, NthType.LastChild)
        "nth-last-of-type" -> parseNthPseudoClass(context, input, state, NthType.LastOfType)
        "is" -> parseIsOrWhere(context, input, state, Component::Is)
        "where" -> parseIsOrWhere(context, input, state, Component::Where)
        "has" -> parseHas(context, input, state)
        "host" -> {
            if (!state.allowsTreeStructuralPseudoClasses()) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }

            val selector = parseInnerCompoundSelector(context, input, state).unwrap { return it }
            return Ok(Component.Host(selector))
        }

        "not" -> parseNegation(context, input, state)

        else -> {
            if (!state.allowsCustomFunctionalPseudoClasses()) {
                return Err(input.newError(SelectorParseErrorKind.InvalidState))
            }

            parseNonTSFunctionalPseudoClass(input, location, name).map(Component::NonTSFPseudoClass)
        }
    }
}

private fun parseNthPseudoClass(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
    type: NthType,
): Result<Component, ParseError> {
    if (!state.allowsTreeStructuralPseudoClasses()) {
        return Err(input.newError(SelectorParseErrorKind.InvalidState))
    }

    val (a, b) = parseNth(input).unwrap { return it }
    val nthData = NthData(type, a, b, isFunction = true)

    if (type.isOfType) {
        return Ok(Component.Nth(nthData))
    }

    if (input.tryParse { it.expectIdentifierMatching("of") }.isErr()) {
        return Ok(Component.Nth(nthData))
    }

    val selectors = SelectorList.parseWithState(
        context,
        input,
        state + SelectorParsingState.SKIP_DEFAULT_NAMESPACE + SelectorParsingState.DISALLOW_PSEUDOS,
        ParseForgiving.No,
        ParseRelative.No,
    ).unwrap { return it }

    return Ok(Component.Nth(nthData, selectors.selectors))
}

private fun parseIsOrWhere(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
    wrapper: (List<Selector>) -> Component,
): Result<Component, ParseError> {
    val selectors = SelectorList.parseWithState(
        context,
        input,
        state + SelectorParsingState.SKIP_DEFAULT_NAMESPACE + SelectorParsingState.DISALLOW_PSEUDOS,
        ParseForgiving.Yes,
        ParseRelative.No,
    ).unwrap { return it }

    return Ok(wrapper(selectors.selectors))
}

private fun parseHas(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
): Result<Component, ParseError> {
    if (state.intersects(SelectorParsingState.DISALLOW_RELATIVE_SELECTORS)) {
        return Err(input.newError(SelectorParseErrorKind.InvalidState))
    }

    val selectors = SelectorList.parseWithState(
        context,
        input,
        state + SelectorParsingState.SKIP_DEFAULT_NAMESPACE + SelectorParsingState.DISALLOW_PSEUDOS + SelectorParsingState.DISALLOW_RELATIVE_SELECTORS,
        ParseForgiving.No,
        ParseRelative.ForHas,
    ).unwrap { return it }

    return Ok(Component.Has(RelativeSelector.fromSelectorList(selectors)))
}

private fun parseNonTSFunctionalPseudoClass(
    input: Parser,
    location: SourceLocation,
    name: String,
): Result<NonTSFPseudoClass, ParseError> {
    return when (name.lowercase()) {
        "lang" -> {
            return when (val identifierResult = input.expectIdentifier()) {
                is Ok -> Ok(NonTSFPseudoClass.Lang(identifierResult.value))
                is Err -> identifierResult
            }
        }

        else -> Err(location.newUnexpectedTokenError(Token.Function(name)))
    }
}

private fun parsePseudoClass(
    location: SourceLocation,
    name: String,
    state: SelectorParsingState,
): Result<Component, ParseError> {
    if (!state.allowsNonFunctionalPseudoClasses()) {
        return Err(location.newError(SelectorParseErrorKind.InvalidState))
    }

    if (state.allowsTreeStructuralPseudoClasses()) {
        when (name.lowercase()) {
            "first-child" -> return Ok(Component.Nth(NthData.first(ofType = false)))
            "last-child" -> return Ok(Component.Nth(NthData.last(ofType = false)))
            "only-child" -> return Ok(Component.Nth(NthData.only(ofType = false)))
            "first-of-type" -> return Ok(Component.Nth(NthData.first(ofType = true)))
            "last-of-type" -> return Ok(Component.Nth(NthData.last(ofType = true)))
            "only-of-type" -> return Ok(Component.Nth(NthData.only(ofType = true)))
            "root" -> return Ok(Component.Root)
            "empty" -> return Ok(Component.Empty)
            "scope" -> return Ok(Component.Scope)
            "host" -> return Ok(Component.Host(selector = null))
            else -> {}
        }
    }

    val pseudoClass = parseNonTSPseudoClass(location, name).unwrap { return it }
    if (state.intersects(SelectorParsingState.AFTER_PSEUDO_ELEMENT) && !pseudoClass.isUserActionState()) {
        return Err(location.newError(SelectorParseErrorKind.InvalidState))
    }
    return Ok(Component.NonTSPseudoClass(pseudoClass))
}

private fun parseNonTSPseudoClass(location: SourceLocation, name: String): Result<NonTSPseudoClass, ParseError> {
    return when (name.lowercase()) {
        "active" -> Ok(NonTSPseudoClass.Active)
        "checked" -> Ok(NonTSPseudoClass.Checked)
        "autofill" -> Ok(NonTSPseudoClass.Autofill)
        "disabled" -> Ok(NonTSPseudoClass.Disabled)
        "enabled" -> Ok(NonTSPseudoClass.Enabled)
        "defined" -> Ok(NonTSPseudoClass.Defined)
        "focus" -> Ok(NonTSPseudoClass.Focus)
        "focus-visible" -> Ok(NonTSPseudoClass.FocusVisible)
        "focus-within" -> Ok(NonTSPseudoClass.FocusWithin)
        "hover" -> Ok(NonTSPseudoClass.Hover)
        "target" -> Ok(NonTSPseudoClass.Target)
        "indeterminate" -> Ok(NonTSPseudoClass.Indeterminate)
        "fullscreen" -> Ok(NonTSPseudoClass.Fullscreen)
        "modal" -> Ok(NonTSPseudoClass.Modal)
        "optional" -> Ok(NonTSPseudoClass.Optional)
        "required" -> Ok(NonTSPseudoClass.Required)
        "valid" -> Ok(NonTSPseudoClass.Valid)
        "invalid" -> Ok(NonTSPseudoClass.Invalid)
        "user-valid" -> Ok(NonTSPseudoClass.UserValid)
        "user-invalid" -> Ok(NonTSPseudoClass.UserInvalid)
        "in-range" -> Ok(NonTSPseudoClass.InRange)
        "out-of-range" -> Ok(NonTSPseudoClass.OutOfRange)
        "read-write" -> Ok(NonTSPseudoClass.ReadWrite)
        "read-only" -> Ok(NonTSPseudoClass.ReadOnly)
        "default" -> Ok(NonTSPseudoClass.Default)
        "placeholder-shown" -> Ok(NonTSPseudoClass.PlaceholderShown)
        "link" -> Ok(NonTSPseudoClass.Link)
        "any-link" -> Ok(NonTSPseudoClass.AnyLink)
        "visited" -> Ok(NonTSPseudoClass.Visited)
        else -> Err(location.newUnexpectedTokenError(Token.Identifier(name)))
    }
}

private fun parseNegation(
    context: SelectorParserContext,
    input: Parser,
    state: SelectorParsingState,
): Result<Component, ParseError> {
    val selectorList = SelectorList.parseWithState(
        context,
        input,
        state + SelectorParsingState.SKIP_DEFAULT_NAMESPACE + SelectorParsingState.DISALLOW_PSEUDOS,
        ParseForgiving.No,
        ParseRelative.No,
    ).unwrap { return it }

    return Ok(Component.Negation(selectorList.selectors))
}


private fun parseAttributeSelector(context: SelectorParserContext, input: Parser): Result<Component, ParseError> {
    val qualifiedName = when (val qualifiedName = parseQualifiedName(context, input, true)) {
        is Ok -> qualifiedName.value
        is Err -> return qualifiedName
    }

    val localName: String
    val namespace: NamespaceConstraint?

    when (qualifiedName) {
        is QualifiedName.Some -> {
            localName = qualifiedName.localName ?: error("unreachable")

            val prefix = qualifiedName.prefix
            namespace = when (prefix) {
                is QualifiedNamePrefix.ImplicitNoNamespace, is QualifiedNamePrefix.ExplicitNoNamespace -> null
                is QualifiedNamePrefix.ExplicitNamespace -> NamespaceConstraint.Specific(prefix.prefix, prefix.url)
                is QualifiedNamePrefix.ExplicitAnyNamespace -> NamespaceConstraint.Any
                is QualifiedNamePrefix.ImplicitAnyNamespace, is QualifiedNamePrefix.ImplicitDefaultNamespace -> error("unreachable")
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
            val localNameLower = localName.lowercase()
            return if (namespace != null) {
                Ok(
                    Component.AttributeOther(
                        namespace,
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
        is AttributeSelectorOperator.Suffix,
        -> value.isEmpty()
    }

    val caseSensitive = when (val flagResult = parseAttributeSelectorFlags(input)) {
        is Ok -> flagResult.value
        is Err -> return flagResult
    }

    val localNameLower = localName.lowercase()

    return if (namespace != null) {
        Ok(
            Component.AttributeOther(
                namespace,
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
                if (combinator == Combinator.Child || combinator == Combinator.Descendant) break
            }
        }
    }

    override fun hasNext(): Boolean {
        // evaluate if there are any remaining components in compound selector
        if (iterator.hasNext()) return true

        // advance the sequence and skip all sibling compound selectors
        if (!iterator.hasNextSequence()) return false
        val combinator = iterator.nextSequence()
        if (combinator != Combinator.Child && combinator != Combinator.Descendant) skipUntilAncestor(iterator)

        // reevaluate if there are any remaining components in compound selector
        return iterator.hasNext()
    }

    override fun next(): Component = iterator.next()
}
