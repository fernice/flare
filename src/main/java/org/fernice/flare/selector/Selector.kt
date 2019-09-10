/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.Nth
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.debugAssert
import org.fernice.flare.panic
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.drain
import org.fernice.flare.std.iter.iter
import org.fernice.flare.style.parser.QuirksMode
import java.io.Writer

data class NamespacePrefix(val prefix: String)

data class NamespaceUrl(val prefix: NamespacePrefix, val url: String)

sealed class Component : ToCss {

    data class Combinator(val combinator: org.fernice.flare.selector.Combinator) : Component()

    data class DefaultNamespace(val namespace: NamespaceUrl) : Component()

    object ExplicitNoNamespace : Component()
    object ExplicitAnyNamespace : Component()
    data class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component()

    data class LocalName(val localName: String, val localNameLower: String) : Component()

    object ExplicitUniversalType : Component()

    data class ID(val id: String) : Component()

    data class Class(val styleClass: String) : Component()

    data class PseudoElement(val pseudoElement: org.fernice.flare.selector.PseudoElement) : Component()
    data class NonTSPseudoClass(val pseudoClass: org.fernice.flare.selector.NonTSPseudoClass) : Component()

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
    object OnlyOfType : Component()

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
        val operation: AttributeSelectorOperation,
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

    override fun toCss(writer: Writer) {
        fun Int.toStringWithSign(): String {
            return if (this >= 0) "+$this" else this.toString()
        }

        fun writeNth(prefix: String, nth: Nth, writer: Writer) {
            writer.append(prefix)
            writer.append('(')

            with(nth) {
                writer.append(
                    when {
                        a == 0 && b == 0 -> "0"

                        a == 1 && b == 0 -> "n"
                        a == -1 && b == 0 -> "-n"
                        b == 0 -> "${a}n"

                        a == 0 -> b.toString()
                        a == 1 -> "n${b.toStringWithSign()}"
                        a == -1 -> "-n${b.toStringWithSign()}"
                        else -> "${a}n${b.toStringWithSign()}"
                    }
                )
            }

            writer.append(')')
        }

        when (this) {
            is Component.Combinator -> this.combinator.toCss(writer)
            is Component.PseudoElement -> this.pseudoElement.toCss(writer)
            is Component.ID -> {
                writer.append('#')
                writer.append(this.id)
            }
            is Component.Class -> {
                writer.append(".")
                writer.append(this.styleClass)
            }
            is Component.LocalName -> writer.append(localName)
            is Component.ExplicitUniversalType -> writer.append('*')

            is Component.DefaultNamespace -> Unit
            is Component.ExplicitNoNamespace -> writer.append('|')
            is Component.ExplicitAnyNamespace -> writer.append("*|")
            is Component.Namespace -> {
                writer.append(this.prefix.prefix)
                writer.append('|')
            }

            is Component.AttributeInNoNamespaceExists -> {
                writer.append('[')
                writer.append(this.localName)
                writer.append(']')
            }
            is Component.AttributeInNoNamespace -> {
                writer.append('[')
                writer.append(this.localName)
                this.operator.toCss(writer)
                writer.append('"')
                writer.append(this.value)
                writer.append('"')
                if (this.caseSensitive) {
                    writer.append(" i")
                }
                writer.append(']')
            }
            is Component.AttributeOther -> TODO("Implement toCss(Writer)")

            is Component.Negation -> {
                writer.append(":not(")
                for (component in this.simpleSelector) {
                    component.toCss(writer)
                }
                writer.append(")")
            }

            is Component.FirstChild -> writer.append(":first-child")
            is Component.LastChild -> writer.append(":last-child")
            is Component.OnlyChild -> writer.append(":only-child")
            is Component.Root -> writer.append(":root")
            is Component.Empty -> writer.append(":empty")
            is Component.Scope -> writer.append(":scope")
            is Component.Host -> TODO("Implement host selector")
            is Component.FirstOfType -> writer.append(":first-of-type")
            is Component.LastOfType -> writer.append(":last-of-type")
            is Component.OnlyOfType -> writer.append(":only-of-type")
            is Component.NthChild -> writeNth("nth-child", this.nth, writer)
            is Component.NthLastChild -> writeNth("nth-last-child", this.nth, writer)
            is Component.NthOfType -> writeNth("nth-of-type", this.nth, writer)
            is Component.NthLastOfType -> writeNth("nth-last-child", this.nth, writer)

            is Component.NonTSPseudoClass -> this.pseudoClass.toCss(writer)
        }
    }
}

sealed class Combinator : ToCss {

    object Child : Combinator()

    object Descendant : Combinator()

    object NextSibling : Combinator()

    object LaterSibling : Combinator()

    object PseudoElement : Combinator()

    override fun toCss(writer: Writer) {
        return when (this) {
            is Combinator.Child -> writer.write(" > ")
            is Combinator.Descendant -> writer.write(" ")
            is Combinator.NextSibling -> writer.write(" + ")
            is Combinator.LaterSibling -> writer.write(" ~ ")
            is Combinator.PseudoElement -> Unit
        }
    }
}

const val PSEUDO_COUNT = 8

sealed class PseudoElement : ToCss {

    object Before : PseudoElement()
    object After : PseudoElement()
    object Selection : PseudoElement()
    object FirstLetter : PseudoElement()
    object FirstLine : PseudoElement()
    object Placeholder : PseudoElement()

    object Icon : PseudoElement()

    fun ordinal(): Int {
        return when (this) {
            is PseudoElement.Before -> 0
            is PseudoElement.After -> 1
            is PseudoElement.Selection -> 2
            is PseudoElement.FirstLetter -> 3
            is PseudoElement.FirstLine -> 4
            is PseudoElement.Placeholder -> 5

            is PseudoElement.Icon -> 6
        }
    }

    override fun toCss(writer: Writer) {
        val css = when (this) {
            is PseudoElement.Before -> "::before"
            is PseudoElement.After -> "::after"
            is PseudoElement.Selection -> "::selection"
            is PseudoElement.FirstLetter -> "::first-letter"
            is PseudoElement.FirstLine -> "::first-line"
            is PseudoElement.Placeholder -> "::placeholder"

            is PseudoElement.Icon -> "::icon"
        }

        writer.write(css)
    }

    companion object {

        fun forEachEagerCascadedPseudoElement(function: (PseudoElement) -> Unit) {
            for (pseudoElement in values) {
                function(pseudoElement)
            }
        }

        fun fromEagerOrdinal(ordinal: Int): PseudoElement {
            return when (ordinal) {
                0 -> PseudoElement.Before
                1 -> PseudoElement.After
                2 -> PseudoElement.Selection
                3 -> PseudoElement.FirstLetter
                4 -> PseudoElement.FirstLine
                5 -> PseudoElement.Placeholder

                6 -> PseudoElement.Icon
                else -> throw IndexOutOfBoundsException()
            }
        }

        val values: Array<PseudoElement> by lazy {
            arrayOf(
                PseudoElement.Before,
                PseudoElement.After,
                PseudoElement.Selection,
                PseudoElement.FirstLetter,
                PseudoElement.FirstLine,
                PseudoElement.Placeholder,

                PseudoElement.Icon
            )
        }
    }
}

sealed class NonTSPseudoClass : ToCss {

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

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is NonTSPseudoClass.Active -> ":active"
                is NonTSPseudoClass.Checked -> ":checked"
                is NonTSPseudoClass.Disabled -> ":disabled"
                is NonTSPseudoClass.Enabled -> ":enabled"
                is NonTSPseudoClass.Focus -> ":focus"
                is NonTSPseudoClass.Fullscreen -> ":fullscreen"
                is NonTSPseudoClass.Hover -> ":hover"
                is NonTSPseudoClass.Indeterminate -> ":indeterminate"
                is NonTSPseudoClass.Lang -> ":lang($language)"
                is NonTSPseudoClass.Link -> ":link"
                is NonTSPseudoClass.PlaceholderShown -> ":placeholder-shown"
                is NonTSPseudoClass.ReadWrite -> ":read-write"
                is NonTSPseudoClass.ReadOnly -> ":read-only"
                is NonTSPseudoClass.Target -> ":target"
                is NonTSPseudoClass.Visited -> ":visited"
            }
        )
    }
}

sealed class NamespaceConstraint {

    object Any : NamespaceConstraint()

    data class Specific(val prefix: NamespacePrefix, val url: NamespaceUrl) : NamespaceConstraint()
}

sealed class AttributeSelectorOperation {

    object Exists : AttributeSelectorOperation()

    data class WithValue(val operator: AttributeSelectorOperator, val caseSensitive: Boolean, val expectedValue: String) : AttributeSelectorOperation()
}

sealed class AttributeSelectorOperator : ToCss {

    object Equal : AttributeSelectorOperator()
    object Includes : AttributeSelectorOperator()
    object DashMatch : AttributeSelectorOperator()
    object Prefix : AttributeSelectorOperator()
    object Substring : AttributeSelectorOperator()
    object Suffix : AttributeSelectorOperator()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Equal -> "="
                is Includes -> "~="
                is DashMatch -> "|="
                is Prefix -> "^="
                is Substring -> "*="
                is Suffix -> "$="
            }
        )
    }
}

/**
 * A Selector represents a sequence of compound selectors where each simple selector is separated by a [Combinator].
 * A compound selector consists out of a sequence of simple selectors, represented by [Component]. The Selector is
 * stored in matching order (right-to-left) for the combinators whereas for the compound selectors in parse order
 * (left-to-right).
 */
class Selector(private val header: SpecificityAndFlags, private val components: List<Component>) : ToCss {

    /**
     * Returns the specificity of this selector in a 4 Byte compressed format. For further information of the format
     * see [Specificity].
     */
    fun specificity(): Int {
        return header.specificity()
    }

    fun pseudoElement(): PseudoElement? {
        if (!header.hasPseudoElement()) {
            return null
        }

        for (component in components) {
            if (component is Component.PseudoElement) {
                return component.pseudoElement
            }
        }

        panic("header.hasPseudoElement() resulted in true, but pseudoElement was not found")
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
                when (val next = iter.next()) {
                    is Some -> compoundSelector.add(next.value)
                    is None -> break@inner
                }
            }

            selector.addAll(compoundSelector.drain().reversed())

            when (val next = iter.nextSequence()) {
                is Some -> selector.add(Component.Combinator(next.value))
                is None -> break@outer
            }
        }

        return selector.iter()
    }

    override fun toCss(writer: Writer) {
        rawIterTrueParseOrder().toCssJoining(writer)
    }

    override fun toString(): String {
        return "Selector[sr=${toCssString()}, spec=${specificity()}]"
    }
}

data class SelectorIter(private val iter: Iter<Component>, private var nextCombinator: Option<Combinator> = None) : Iter<Component> {

    override fun next(): Option<Component> {
        if (nextCombinator.isSome()) {
            throw IllegalStateException("next in sequence")
        }

        return when (val next = iter.next()) {
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

data class SelectorList(private val selectors: List<Selector>) : Iterable<Selector>, ToCss {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    override fun toCss(writer: Writer) {
        selectors.toCssJoining(writer, separator = ", ")
    }

    override fun toString(): String {
        return "SelectorList[sr=${toCssString()}]"
    }

    companion object {

        fun parse(context: SelectorParserContext, input: Parser): Result<SelectorList, ParseError> {
            val selectors = mutableListOf<Selector>()

            loop@
            while (true) {
                when (val selector = input.parseUntilBefore(Delimiters.Comma) { i -> parseSelector(context, i) }) {
                    is Ok -> selectors.add(selector.value)
                    is Err -> return selector
                }

                val token = when (val token = input.next()) {
                    is Ok -> token.value
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
