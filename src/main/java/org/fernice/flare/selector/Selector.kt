/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.Nth
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.cssparser.toCssString
import org.fernice.flare.panic
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

        fun iterator(): SelectorIterator {
            return SelectorIterator(simpleSelector)
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
        val neverMatches: Boolean,
    ) : Component()

    data class AttributeInNoNamespaceExists(val localName: String, val localNameLower: String) : Component()

    data class AttributeInNoNamespace(
        val localName: String,
        val localNameLower: String,
        val operator: AttributeSelectorOperator,
        val value: String,
        val caseSensitive: Boolean,
        val neverMatches: Boolean,
    ) : Component()

    fun ancestorHash(quirksMode: QuirksMode): Int? {
        return when (this) {
            is DefaultNamespace -> hashString(namespace.url)
            is Namespace -> hashString(namespace.url)
            is LocalName -> if (localName == localNameLower) hashString(localName) else null
            is ID -> if (quirksMode != QuirksMode.Quirks) hashString(id) else null
            is Class -> if (quirksMode != QuirksMode.Quirks) hashString(styleClass) else null
            else -> null
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
            is Combinator -> this.combinator.toCss(writer)
            is PseudoElement -> this.pseudoElement.toCss(writer)
            is ID -> {
                writer.append('#')
                writer.append(this.id)
            }

            is Class -> {
                writer.append(".")
                writer.append(this.styleClass)
            }

            is LocalName -> writer.append(localName)
            is ExplicitUniversalType -> writer.append('*')

            is DefaultNamespace -> Unit
            is ExplicitNoNamespace -> writer.append('|')
            is ExplicitAnyNamespace -> writer.append("*|")
            is Namespace -> {
                writer.append(this.prefix.prefix)
                writer.append('|')
            }

            is AttributeInNoNamespaceExists -> {
                writer.append('[')
                writer.append(this.localName)
                writer.append(']')
            }

            is AttributeInNoNamespace -> {
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

            is AttributeOther -> TODO("Implement toCss(Writer)")

            is Negation -> {
                writer.append(":not(")
                for (component in this.simpleSelector) {
                    component.toCss(writer)
                }
                writer.append(")")
            }

            is FirstChild -> writer.append(":first-child")
            is LastChild -> writer.append(":last-child")
            is OnlyChild -> writer.append(":only-child")
            is Root -> writer.append(":root")
            is Empty -> writer.append(":empty")
            is Scope -> writer.append(":scope")
            is Host -> TODO("Implement host selector")
            is FirstOfType -> writer.append(":first-of-type")
            is LastOfType -> writer.append(":last-of-type")
            is OnlyOfType -> writer.append(":only-of-type")
            is NthChild -> writeNth("nth-child", this.nth, writer)
            is NthLastChild -> writeNth("nth-last-child", this.nth, writer)
            is NthOfType -> writeNth("nth-of-type", this.nth, writer)
            is NthLastOfType -> writeNth("nth-last-child", this.nth, writer)

            is NonTSPseudoClass -> this.pseudoClass.toCss(writer)
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
            is Child -> writer.write(" > ")
            is Descendant -> writer.write(" ")
            is NextSibling -> writer.write(" + ")
            is LaterSibling -> writer.write(" ~ ")
            is PseudoElement -> Unit
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
            is Before -> 0
            is After -> 1
            is Selection -> 2
            is FirstLetter -> 3
            is FirstLine -> 4
            is Placeholder -> 5

            is Icon -> 6
        }
    }

    override fun toCss(writer: Writer) {
        val css = when (this) {
            is Before -> "::before"
            is After -> "::after"
            is Selection -> "::selection"
            is FirstLetter -> "::first-letter"
            is FirstLine -> "::first-line"
            is Placeholder -> "::placeholder"

            is Icon -> "::icon"
        }

        writer.write(css)
    }

    companion object {

        inline fun forEachEagerCascadedPseudoElement(function: (PseudoElement) -> Unit) {
            for (pseudoElement in values) {
                function(pseudoElement)
            }
        }

        fun fromEagerOrdinal(ordinal: Int): PseudoElement {
            return when (ordinal) {
                0 -> Before
                1 -> After
                2 -> Selection
                3 -> FirstLetter
                4 -> FirstLine
                5 -> Placeholder

                6 -> Icon
                else -> throw IndexOutOfBoundsException()
            }
        }

        val values: Array<PseudoElement> by lazy {
            arrayOf(
                Before,
                After,
                Selection,
                FirstLetter,
                FirstLine,
                Placeholder,

                Icon
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
    data class Lang(val language: String) : NonTSPseudoClass()
    object Link : NonTSPseudoClass()
    object PlaceholderShown : NonTSPseudoClass()
    object ReadWrite : NonTSPseudoClass()
    object ReadOnly : NonTSPseudoClass()
    object Target : NonTSPseudoClass()
    object Visited : NonTSPseudoClass()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Active -> ":active"
                is Checked -> ":checked"
                is Disabled -> ":disabled"
                is Enabled -> ":enabled"
                is Focus -> ":focus"
                is Fullscreen -> ":fullscreen"
                is Hover -> ":hover"
                is Indeterminate -> ":indeterminate"
                is Lang -> ":lang($language)"
                is Link -> ":link"
                is PlaceholderShown -> ":placeholder-shown"
                is ReadWrite -> ":read-write"
                is ReadOnly -> ":read-only"
                is Target -> ":target"
                is Visited -> ":visited"
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
     * Returns a high-level [SelectorIterator]. Iterates over a single compound selector until it returns [None]. After that
     * [SelectorIter.nextSequence] might return [Some] if the is another compound selector.
     *
     * Selector:
     * ```css
     * div.class > #id:visited > *.class3
     * ```
     * Match Order:
     * ```css
     * *.class3 > #id:visited > div.class
     * ```
     */
    fun iterator(): SelectorIterator {
        return SelectorIterator(components)
    }

    /**
     * Returns a raw [Iterator] in parse order. The Iter is not in true parse order meaning the compound selectors are reversed.
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
    fun rawIteratorParseOrder(): Iterator<Component> {
        return components.reversed().iterator()
    }

    /**
     * Constructs a raw [Iterator] that represents true parse order. Due to the nature of how the selector is stored internally,
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
     * @see rawIteratorParseOrder for semi parse order
     * @see iterator for high-level iter
     */
    fun rawIteratorTrueParseOrder(): Iterator<Component> {
        val iterator = SelectorIterator(components.reversed())

        val selector = mutableListOf<Component>()
        val compoundSelector = mutableListOf<Component>()

        outer@
        while (true) {
            while (iterator.hasNext()) {
                val next = iterator.next()
                compoundSelector.add(next)
            }

            selector.addAll(compoundSelector.drain().reversed())

            if (!iterator.hasNextSequence()) break

            selector.add(Component.Combinator(iterator.nextSequence()))
        }

        return selector.iterator()
    }

    private fun <E> MutableCollection<E>.drain(): List<E> {
        val list = toList()
        clear()
        return list
    }

    override fun toCss(writer: Writer) {
        rawIteratorTrueParseOrder().toCssJoining(writer)
    }

    override fun toString(): String {
        return "Selector[sr=${toCssString()}, spec=${specificity()}]"
    }
}

class SelectorIterator(
    private val components: List<Component>,
    private var index: Int = 0,
    private var combinator: Combinator? = null,
) : Iterator<Component> {

    override fun hasNext(): Boolean = combinator == null && index < components.size

    override fun next(): Component {
        if (combinator != null) throw NoSuchElementException("end of sequence")

        val next = components[index++]

        if (index < components.size) {
            val component = components[index]
            if (component is Component.Combinator) {
                combinator = component.combinator
            }
        }

        return next
    }

    fun hasNextSequence(): Boolean = combinator != null || hasNext()

    fun nextSequence(): Combinator {
        val next = combinator ?: error("not at end of sequence")
        combinator = null
        index++
        return next
    }

    fun clone(): SelectorIterator {
        return SelectorIterator(
            components,
            index,
            combinator
        )
    }
}

class SelectorList(private val selectors: List<Selector>) : Iterable<Selector>, ToCss {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    override fun toCss(writer: Writer) {
        selectors.toCssJoining(writer, separator = ", ")
    }

    override fun toString(): String {
        return "SelectorList[${toCssString()}]"
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
