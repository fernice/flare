package de.krall.flare.selector

import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import de.krall.flare.std.iter.Iter
import de.krall.flare.std.iter.drain
import de.krall.flare.std.iter.iter
import de.krall.flare.style.parser.QuirksMode

interface NamespacePrefix {

    fun getPrefix(): String
}

interface NamespaceUrl {

    fun getUrl(): String
}

sealed class Component {

    open fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
        return None()
    }

    class Combinator(val combinator: de.krall.flare.selector.Combinator) : Component()

    class DefaultNamespace(val namespace: NamespaceUrl) : Component() {
        override fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
            return Some(hashString(namespace.getUrl()))
        }
    }

    class ExplicitNoNamespace : Component()
    class ExplicitAnyNamespace : Component()
    class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component() {
        override fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
            return Some(hashString(namespace.getUrl()))
        }
    }

    class LocalName(val localName: String, val localNameLower: String) : Component() {
        override fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
            return if (localName == localNameLower) {
                Some(hashString(localName))
            } else {
                None()
            }
        }
    }

    class ExplicitUniversalType : Component()

    class ID(val id: String) : Component() {
        override fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
            return if (quirksMode != QuirksMode.QUIRKS) {
                Some(hashString(id))
            } else {
                None()
            }
        }
    }

    class Class(val styleClass: String) : Component() {
        override fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
            return if (quirksMode != QuirksMode.QUIRKS) {
                Some(hashString(styleClass))
            } else {
                None()
            }
        }
    }

    class PseudoElement(val pseudoElement: de.krall.flare.selector.PseudoElement) : Component()
    class NonTSPseudoClass(val pseudoClass: de.krall.flare.selector.NonTSPseudoClass) : Component()

    class Negation(val simpleSelector: List<Component>) : Component() {

        fun iter(): SelectorIter {
            return SelectorIter(simpleSelector.iter())
        }
    }

    class FirstChild : Component()
    class LastChild : Component()
    class OnlyChild : Component()
    class FirstOfType : Component()
    class LastOfType : Component()
    class OnlyType : Component()

    class Root : Component()
    class Empty : Component()
    class Scope : Component()
    class Host : Component()

    class NthChild(val nth: Nth) : Component()
    class NthOfType(val nth: Nth) : Component()
    class NthLastChild(val nth: Nth) : Component()
    class NthLastOfType(val nth: Nth) : Component()

    class AttributeOther(val namespace: NamespaceConstraint,
                         val localName: String,
                         val localNameLower: String,
                         val opertation: AttributeSelectorOperation,
                         val neverMatches: Boolean) : Component()

    class AttributeInNoNamespaceExists(val localName: String,
                                       val localNameLower: String) : Component()

    class AttributeInNoNamespace(val localName: String,
                                 val localNameLower: String,
                                 val operator: AttributeSelectorOperator,
                                 val value: String,
                                 val caseSensitive: Boolean,
                                 val neverMatches: Boolean) : Component()
}

sealed class Combinator {

    class Child : Combinator()

    class NextSibling : Combinator()

    class LaterSibling : Combinator()

    class Descendant : Combinator()

    class PseudoElement : Combinator()
}

sealed class PseudoElement {

    class Before : PseudoElement()
    class After : PseudoElement()
    class Selection : PseudoElement()
    class FirstLetter : PseudoElement()
    class FirstLine : PseudoElement()
    class Placeholder : PseudoElement()
}

sealed class NonTSPseudoClass {

    class Active : NonTSPseudoClass()
    class Checked : NonTSPseudoClass()
    class Disabled : NonTSPseudoClass()
    class Enabled : NonTSPseudoClass()
    class Focus : NonTSPseudoClass()
    class Fullscreen : NonTSPseudoClass()
    class Hover : NonTSPseudoClass()
    class Indeterminate : NonTSPseudoClass()
    class Lang(val language: String) : NonTSPseudoClass()
    class Link : NonTSPseudoClass()
    class PlaceholderShown : NonTSPseudoClass()
    class ReadWrite : NonTSPseudoClass()
    class ReadOnly : NonTSPseudoClass()
    class Target : NonTSPseudoClass()
    class Visited : NonTSPseudoClass()
}

sealed class NamespaceConstraint {

    class Any : NamespaceConstraint()

    class Specific(val prefix: NamespacePrefix, val url: NamespaceUrl) : NamespaceConstraint()
}

sealed class AttributeSelectorOperation {

    class Exists : AttributeSelectorOperation()

    class WithValue(val operator: AttributeSelectorOperator, val caseSensitive: Boolean, val expectedValue: String) : AttributeSelectorOperation()
}

sealed class AttributeSelectorOperator {

    class Equal : AttributeSelectorOperator()
    class Includes : AttributeSelectorOperator()
    class DashMatch : AttributeSelectorOperator()
    class Prefix : AttributeSelectorOperator()
    class Substring : AttributeSelectorOperator()
    class Suffix : AttributeSelectorOperator()
}

/**
 * A Selector represents a sequence of compound selectors where each simple selector is separated by a [Combinator].
 * A compound selector consists out of a sequence of simple selectors, represented by [Component]. The Selector is
 * stored in matching order (right-to-left) for the combinators whereas for the compound selectors in parse order
 * (left-to-right).
 */
class Selector(private val header: SpecificityAndFlags, private val components: List<Component>) {

    /**
     * Returns the specificity of this selector in a 4 Byte compressed format. For further information of the format
     * see [Specificity].
     */
    fun specificity(): Int {
        return header.specificity()
    }

    /**
     * Returns a high-level [SelectorIter]. Iterates over a single compound selector until it returns [None]. After that
     * [SelectorIter.nextSequence] might return [Some] if the is another compound selector.
     *
     * Selector:
     * ```
     * div.class > #id:visited > *.class3
     * ```
     * Match Order:
     * ```
     * *.class3 > #id:visited > div.class
     * ```
     */
    fun iter(): SelectorIter {
        return SelectorIter(components.iter())
    }

    /**
     * Returns a raw [Iter] in parse order. The Iter is not in true parse order meaning the compound selectors are reversed.
     * The sequence of the compound selectors in relation to the combinators are in parse order.
     *
     * Selector:
     * ```
     * div.class > #id:visited > *.class3
     * ```
     * Semi Parse Order:
     * ```
     * [.class] [div] > [:visited] [#id] > [.class3] [*]
     * ```
     */
    fun rawIterParseOrder(): Iter<Component> {
        return components.reversed().iter()
    }

    /**
     * Constructs a raw [Iter] that represents true parse order. Due to the nature of how the selector is stored internally,
     * this is a very expensive operation compared to the iters.
     *
     * Selector:
     * ```
     * div.class > #id:visited > *.class3
     * ```
     * Parse Order:
     * ```
     * [div] [.class] > [#id] [:visited] > [*] [.class3]
     * ```
     *
     * @see rawIterParseOrder for semi parse order
     * @see iter for high-level iter
     */
    fun rawIterTrueParseOrder(): Iter<Component> {
        val iter = SelectorIter(components.reversed().iter())

        val selector = mutableListOf<Component>()
        val compoundSelector = mutableListOf<Component>()

        outer@
        while (true) {

            inner@
            while (true) {
                val next = iter.next()

                when (next) {
                    is Some -> compoundSelector.add(next.value)
                    is None -> break@inner
                }
            }

            selector.addAll(compoundSelector.drain().reversed())

            val next = iter.nextSequence()

            when (next) {
                is Some -> selector.add(Component.Combinator(next.value))
                is None -> break@outer
            }
        }

        return selector.iter()
    }
}

class SelectorIter(private val iter: Iter<Component>, private var nextCombinator: Option<Combinator> = None()) : Iter<Component> {

    override fun next(): Option<Component> {
        if (nextCombinator.isSome()) {
            throw IllegalStateException("next in sequence")
        }

        val next = iter.next()

        return when (next) {
            is Some -> {
                if (next.value is Component.Combinator) {
                    nextCombinator = Some(next.value.combinator)
                    None()
                } else {
                    next
                }
            }
            is None -> {
                next
            }
        }
    }

    fun nextSequence(): Option<Combinator> {
        val current = nextCombinator
        nextCombinator = None()
        return current
    }

    override fun clone(): SelectorIter {
        return SelectorIter(iter.clone(), nextCombinator)
    }
}

class SelectorList(private val selectors: List<Selector>) : Iterable<Selector> {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    companion object {

        fun parse(context: SelectorParserContext, input: Parser): Result<SelectorList, ParseError> {
            val selectors = mutableListOf<Selector>()

            loop@
            while (true) {
                val selectorResult = input.parseUntilBefore(Delimiters.Comma, { input -> parseSelector(context, input) })

                when (selectorResult) {
                    is Ok -> selectors.add(selectorResult.value)
                    is Err -> return selectorResult
                }

                val tokenResult = input.next()

                val token = when (tokenResult) {
                    is Ok -> tokenResult.value
                    is Err -> return Ok(SelectorList(selectors))
                }

                when (token) {
                    is Token.Comma -> continue@loop
                    else -> throw IllegalStateException("unreachable")
                }
            }
        }
    }
}