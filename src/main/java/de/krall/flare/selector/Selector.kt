package de.krall.flare.selector

import de.krall.flare.cssparser.Delimiters
import de.krall.flare.cssparser.Nth
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.debugAssert
import de.krall.flare.std.iter.Iter
import de.krall.flare.std.iter.drain
import de.krall.flare.std.iter.iter
import de.krall.flare.style.parser.QuirksMode
import modern.std.Err
import modern.std.None
import modern.std.Ok
import modern.std.Option
import modern.std.Result
import modern.std.Some

data class NamespacePrefix(val prefix: String)

data class NamespaceUrl(val prefix: NamespacePrefix, val url: String)

sealed class Component {

    data class Combinator(val combinator: de.krall.flare.selector.Combinator) : Component()

    data class DefaultNamespace(val namespace: NamespaceUrl) : Component()

    object ExplicitNoNamespace : Component()
    object ExplicitAnyNamespace : Component()
    data class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component()

    data class LocalName(val localName: String, val localNameLower: String) : Component()

    object ExplicitUniversalType : Component()

    data class ID(val id: String) : Component()

    data class Class(val styleClass: String) : Component()

    data class PseudoElement(val pseudoElement: de.krall.flare.selector.PseudoElement) : Component()
    data class NonTSPseudoClass(val pseudoClass: de.krall.flare.selector.NonTSPseudoClass) : Component()

    data class Negation(val simpleSelector: List<Component>) : Component() {

        fun iter(): SelectorIter {
            return SelectorIter(simpleSelector.iter())
        }
    }

    object FirstChild : Component()
    object LastChild : Component()
    object OnlyChild : Component()
    object FirstOfType : Component()
    object LastOfType : Component()
    object OnlyType : Component()

    object Root : Component()
    object Empty : Component()
    object Scope : Component()
    object Host : Component()

    data class NthChild(val nth: Nth) : Component()
    data class NthOfType(val nth: Nth) : Component()
    data class NthLastChild(val nth: Nth) : Component()
    data class NthLastOfType(val nth: Nth) : Component()

    data class AttributeOther(
            val namespace: NamespaceConstraint,
            val localName: String,
            val localNameLower: String,
            val opertation: AttributeSelectorOperation,
            val neverMatches: Boolean
    ) : Component()

    data class AttributeInNoNamespaceExists(val localName: String, val localNameLower: String) : Component()

    data class AttributeInNoNamespace(
            val localName: String,
            val localNameLower: String,
            val operator: AttributeSelectorOperator,
            val value: String,
            val caseSensitive: Boolean,
            val neverMatches: Boolean
    ) : Component()

    fun ancestorHash(quirksMode: QuirksMode): Option<Int> {
        return when (this) {
            is Component.DefaultNamespace -> Some(hashString(namespace.url))
            is Component.Namespace -> Some(hashString(namespace.url))
            is Component.LocalName -> {
                if (localName == localNameLower) {
                    Some(hashString(localName))
                } else {
                    None
                }
            }
            is Component.ID -> {
                if (quirksMode != QuirksMode.QUIRKS) {
                    Some(hashString(id))
                } else {
                    None
                }
            }
            is Component.Class -> {
                if (quirksMode != QuirksMode.QUIRKS) {
                    Some(hashString(styleClass))
                } else {
                    None
                }
            }
            else -> None
        }
    }
}

sealed class Combinator {

    object Child : Combinator()

    object NextSibling : Combinator()

    object LaterSibling : Combinator()

    object Descendant : Combinator()

    object PseudoElement : Combinator()
}

const val PSEUDO_COUNT = 8

sealed class PseudoElement {

    abstract fun ordinal(): Int

    object Before : PseudoElement() {
        override fun ordinal(): Int {
            return 0
        }
    }

    object After : PseudoElement() {
        override fun ordinal(): Int {
            return 1
        }
    }

    object Selection : PseudoElement() {
        override fun ordinal(): Int {
            return 2
        }
    }

    object FirstLetter : PseudoElement() {
        override fun ordinal(): Int {
            return 3
        }
    }

    object FirstLine : PseudoElement() {
        override fun ordinal(): Int {
            return 4
        }
    }

    object Placeholder : PseudoElement() {
        override fun ordinal(): Int {
            return 5
        }
    }

    object FlareTabArea : PseudoElement() {
        override fun ordinal(): Int {
            return 6
        }
    }

    object FlareTab : PseudoElement() {
        override fun ordinal(): Int {
            return 7
        }
    }

    companion object {

        fun forEachEagerCascadedPseudoElement(function: (PseudoElement) -> Unit) {
            for (pseudoElement in values) {
                function(pseudoElement)
            }
        }

        fun fromEagerOrdinal(ordinal: Int): PseudoElement {
            return values[ordinal]
        }

        val values: Array<PseudoElement> by lazy {
            arrayOf(
                    PseudoElement.Before,
                    PseudoElement.After,
                    PseudoElement.Selection,
                    PseudoElement.FirstLetter,
                    PseudoElement.FirstLine,
                    PseudoElement.Placeholder,

                    PseudoElement.FlareTabArea,
                    PseudoElement.FlareTab
            )
        }
    }
}

sealed class NonTSPseudoClass {

    object Active : NonTSPseudoClass()
    object Checked : NonTSPseudoClass()
    object Disabled : NonTSPseudoClass()
    object Enabled : NonTSPseudoClass()
    object Focus : NonTSPseudoClass()
    object Fullscreen : NonTSPseudoClass()
    object Hover : NonTSPseudoClass()
    object Indeterminate : NonTSPseudoClass()
    class Lang(val language: String) : NonTSPseudoClass()
    object Link : NonTSPseudoClass()
    object PlaceholderShown : NonTSPseudoClass()
    object ReadWrite : NonTSPseudoClass()
    object ReadOnly : NonTSPseudoClass()
    object Target : NonTSPseudoClass()
    object Visited : NonTSPseudoClass()
}

sealed class NamespaceConstraint {

    object Any : NamespaceConstraint()

    data class Specific(val prefix: NamespacePrefix, val url: NamespaceUrl) : NamespaceConstraint()
}

sealed class AttributeSelectorOperation {

    object Exists : AttributeSelectorOperation()

    data class WithValue(val operator: AttributeSelectorOperator, val caseSensitive: Boolean, val expectedValue: String) : AttributeSelectorOperation()
}

sealed class AttributeSelectorOperator {

    object Equal : AttributeSelectorOperator()
    object Includes : AttributeSelectorOperator()
    object DashMatch : AttributeSelectorOperator()
    object Prefix : AttributeSelectorOperator()
    object Substring : AttributeSelectorOperator()
    object Suffix : AttributeSelectorOperator()
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

    fun pseudoElement(): Option<PseudoElement> {
        if (!header.hasPseudoElement()) {
            return None
        }

        for (component in components) {
            if (component is Component.PseudoElement) {
                return Some(component.pseudoElement)
            }
        }

        debugAssert(false, "something went terribly wrong")
        return None
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

    override fun toString(): String {
        return "Selector[components=${components.size}, specificity=${specificity()}]"
    }
}

data class SelectorIter(private val iter: Iter<Component>, private var nextCombinator: Option<Combinator> = None) : Iter<Component> {

    override fun next(): Option<Component> {
        if (nextCombinator.isSome()) {
            throw IllegalStateException("next in sequence")
        }

        val next = iter.next()

        return when (next) {
            is Some -> {
                val component = next.value

                if (component is Component.Combinator) {
                    nextCombinator = Some(component.combinator)
                    None
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
        nextCombinator = None
        return current
    }

    override fun clone(): SelectorIter {
        return SelectorIter(iter.clone(), nextCombinator)
    }
}

data class SelectorList(private val selectors: List<Selector>) : Iterable<Selector> {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    companion object {

        fun parse(context: SelectorParserContext, input: Parser): Result<SelectorList, ParseError> {
            val selectors = mutableListOf<Selector>()

            loop@
            while (true) {
                val selectorResult = input.parseUntilBefore(Delimiters.Comma) { i -> parseSelector(context, i) }

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