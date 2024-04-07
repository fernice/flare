/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.cssparser.*
import org.fernice.flare.panic
import org.fernice.flare.style.QuirksMode
import org.fernice.std.*
import java.io.Writer

data class NamespacePrefix(val prefix: String)

data class NamespaceUrl(val prefix: NamespacePrefix, val url: String)

sealed class Component : ToCss {

    data class Combinator(val combinator: org.fernice.flare.selector.Combinator) : Component()

    data class LocalName(val localName: String, val localNameLower: String) : Component()

    data class ID(val id: String) : Component()
    data class Class(val styleClass: String) : Component()

    data class AttributeInNoNamespaceExists(
        val localName: String,
        val localNameLower: String,
    ) : Component()

    data class AttributeInNoNamespace(
        val localName: String,
        val localNameLower: String,
        val operator: AttributeSelectorOperator,
        val value: String,
        val caseSensitive: Boolean,
        val neverMatches: Boolean,
    ) : Component()

    data class AttributeOther(
        val namespace: NamespaceConstraint,
        val localName: String,
        val localNameLower: String,
        val operation: AttributeSelectorOperation,
        val neverMatches: Boolean,
    ) : Component()

    data object ExplicitNoNamespace : Component()
    data object ExplicitAnyNamespace : Component()
    data class DefaultNamespace(val namespace: NamespaceUrl) : Component()
    data class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component()

    data object ExplicitUniversalType : Component()

    data class Negation(val selectors: List<Selector>) : Component()

    data object ParentSelector : Component()

    data object Root : Component()
    data object Empty : Component()
    data object Scope : Component()

    data class Nth(val data: NthData, val selectors: List<Selector> = emptyList()) : Component()

    data class NonTSPseudoClass(val pseudoClass: org.fernice.flare.selector.NonTSPseudoClass) : Component()
    data class NonTSFPseudoClass(val pseudoClass: org.fernice.flare.selector.NonTSFPseudoClass) : Component()

    data class Part(val names: List<String>) : Component()
    data class Slotted(val selector: Selector) : Component()
    data class Host(val selector: Selector?) : Component()

    data class Where(val selectors: List<Selector>) : Component()
    data class Is(val selectors: List<Selector>) : Component()
    data class Has(val selectors: List<RelativeSelector>) : Component()

    data class PseudoElement(val pseudoElement: org.fernice.flare.selector.PseudoElement) : Component()

    data object RelativeSelectorAnchor : Component()

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

        fun Writer.appendNth(a: Int, b: Int) {
            append(
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

        fun Writer.appendSelectors(selectors: List<Selector>) {
            for ((index, selector) in selectors.withIndex()) {
                selector.toCss(this)
                if (index < selectors.lastIndex) append(", ")
            }
        }

        when (this) {
            is Combinator -> this.combinator.toCss(writer)

            is LocalName -> writer.append(localName)

            is ID -> {
                writer.append('#')
                writer.append(this.id)
            }

            is Class -> {
                writer.append(".")
                writer.append(this.styleClass)
            }

            is ExplicitUniversalType -> writer.append('*')

            is ExplicitNoNamespace -> writer.append('|')
            is ExplicitAnyNamespace -> writer.append("*|")
            is DefaultNamespace -> Unit
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
                writer.appendSelectors(selectors)
                writer.append(")")
            }

            is ParentSelector -> writer.append("&")

            is Root -> writer.append(":root")
            is Empty -> writer.append(":empty")
            is Scope -> writer.append(":scope")

            is Nth -> {
                if (!data.isFunction) {
                    when (data.type) {
                        NthType.Child -> writer.append(":first-child")
                        NthType.LastChild -> writer.append(":last-child")
                        NthType.OnlyChild -> writer.append(":only-child")
                        NthType.OfType -> writer.append(":first-of-type")
                        NthType.LastOfType -> writer.append(":last-of-type")
                        NthType.OnlyOfType -> writer.append(":only-of-type")
                    }
                } else {
                    when (data.type) {
                        NthType.Child -> writer.append(":nth-child")
                        NthType.LastChild -> writer.append(":nth-last-child")
                        NthType.OnlyChild -> error("invalid case")
                        NthType.OfType -> writer.append(":nth-of-type")
                        NthType.LastOfType -> writer.append(":nth-last-of-type")
                        NthType.OnlyOfType -> error("invalid case")
                    }
                    writer.append("(")
                    writer.appendNth(data.a, data.b)

                    if (selectors.isNotEmpty()) {
                        writer.append(" of ")
                        writer.appendSelectors(selectors)
                    }

                    writer.append(")")
                }
            }

            is NonTSPseudoClass -> pseudoClass.toCss(writer)
            is NonTSFPseudoClass -> pseudoClass.toCss(writer)

            is Part -> {
                writer.append(":part(")
                writer.append(names.joinToString())
                writer.append(")")
            }

            is Slotted -> {
                writer.append(":slotted(")
                selector.toCss(writer)
                writer.append(")")
            }

            is Host -> {
                writer.append(":host")
                if (selector != null) {
                    writer.append("(")
                    selector.toCss(writer)
                    writer.append(")")
                }
            }

            is Where -> {
                writer.append(":where(")
                writer.appendSelectors(selectors)
                writer.append(")")
            }

            is Is -> {
                writer.append(":is(")
                writer.appendSelectors(selectors)
                writer.append(")")
            }

            is Has -> {
                writer.append(":has(")
                writer.appendSelectors(selectors.map { it.selector })
                writer.append(")")
            }

            is PseudoElement -> this.pseudoElement.toCss(writer)

            is RelativeSelectorAnchor -> {}
        }
    }
}

enum class Combinator : ToCss {

    Child,
    Descendant,
    NextSibling,
    LaterSibling,
    Part,
    SlotAssignment,
    PseudoElement;

    fun isSibling(): Boolean {
        return when (this) {
            Child,
            Descendant,
            -> false

            NextSibling,
            LaterSibling,
            -> true

            Part,
            SlotAssignment,
            PseudoElement,
            -> false
        }
    }

    override fun toCss(writer: Writer) {
        return when (this) {
            Child -> writer.write(" > ")
            Descendant -> writer.write(" ")
            NextSibling -> writer.write(" + ")
            LaterSibling -> writer.write(" ~ ")
            Part,
            SlotAssignment,
            PseudoElement,
            -> {
            }
        }
    }
}

enum class NthType {
    Child,
    LastChild,
    OnlyChild,
    OfType,
    LastOfType,
    OnlyOfType;

    val isOfType: Boolean
        get() = when (this) {
            OfType, LastOfType, OnlyOfType -> true
            Child, LastChild, OnlyChild -> false
        }

    val isOnly: Boolean
        get() = when (this) {
            OnlyChild, OnlyOfType -> true
            Child, LastChild, OfType, LastOfType -> false
        }

    val isFromEnd: Boolean
        get() = when (this) {
            LastChild, LastOfType -> true
            Child, OnlyChild, OnlyOfType, OfType -> false
        }
}

data class NthData(
    val type: NthType,
    val a: Int,
    val b: Int,
    val isFunction: Boolean,
) {

    companion object {
        fun first(ofType: Boolean): NthData {
            val type = when {
                ofType -> NthType.OfType
                else -> NthType.Child
            }
            return NthData(type, a = 0, b = 1, isFunction = false)
        }

        fun last(ofType: Boolean): NthData {
            val type = when {
                ofType -> NthType.LastOfType
                else -> NthType.LastChild
            }
            return NthData(type, a = 0, b = 1, isFunction = false)
        }

        fun only(ofType: Boolean): NthData {
            val type = when {
                ofType -> NthType.OnlyOfType
                else -> NthType.OnlyChild
            }
            return NthData(type, a = 0, b = 1, isFunction = false)
        }
    }
}

enum class PseudoElement : ToCss {

    Before,
    After,
    Selection,
    FirstLetter,
    FirstLine,
    Placeholder,

    Flare_Icon;

    fun acceptsStatePseudoClasses(): Boolean = false

    fun validAfterSlotted(): Boolean = false

    override fun toCss(writer: Writer) {
        val css = when (this) {
            Before -> "::before"
            After -> "::after"
            Selection -> "::selection"
            FirstLetter -> "::first-letter"
            FirstLine -> "::first-line"
            Placeholder -> "::placeholder"

            Flare_Icon -> "::icon"
        }

        writer.write(css)
    }
}

// NonTreeStructural-PseudoClass
enum class NonTSPseudoClass : ToCss {

    Active,
    Checked,
    Autofilled,
    Disabled,
    Enabled,
    Defined, // HTML specific
    Focus,
    FocusVisible,
    FocusWithin,
    Hover,
    Target,
    Indeterminate,
    Fullscreen,
    Modal,
    Optional,
    Required,
    Valid,
    Invalid,
    UserValid,
    UserInvalid,
    InRange,
    OutOfRange,
    ReadWrite,
    ReadOnly,
    Default,
    PlaceholderShown,
    Link,
    AnyLink,
    Visited;

    fun isActiveOrHover(): Boolean = when (this) {
        Hover, Active -> true
        else -> false
    }

    fun isUserActionState(): Boolean = when (this) {
        Focus, Hover, Active -> true
        else -> false
    }

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                Active -> ":active"
                Checked -> ":checked"
                Autofilled -> ":autofilled"
                Disabled -> ":disabled"
                Enabled -> ":enabled"
                Defined -> ":defined"
                Focus -> ":focus"
                FocusVisible -> ":focus-visible"
                FocusWithin -> ":focus-visible"
                Hover -> ":hover"
                Target -> ":target"
                Indeterminate -> ":indeterminate"
                Fullscreen -> ":fullscreen"
                Modal -> ":modal"
                Optional -> ":optional"
                Required -> ":required"
                Valid -> ":valid"
                Invalid -> ":invalid"
                UserValid -> ":user-valid"
                UserInvalid -> ":user-invalid"
                InRange -> ":in-range"
                OutOfRange -> ":out-of-range"
                ReadWrite -> ":read-write"
                ReadOnly -> ":read-only"
                Default -> ":default"
                PlaceholderShown -> ":placeholder-shown"
                Link -> ":link"
                AnyLink -> ":any-link"
                Visited -> ":visited"
            }
        )
    }
}

// NonTreeStructural-Functional-PseudoClass
sealed class NonTSFPseudoClass : ToCss {

    data class Lang(val language: String) : NonTSFPseudoClass()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Lang -> ":lang($language)"
            }
        )
    }
}

sealed class NamespaceConstraint {

    data object Any : NamespaceConstraint()

    data class Specific(val prefix: NamespacePrefix, val url: NamespaceUrl) : NamespaceConstraint()
}

sealed class AttributeSelectorOperation {

    data object Exists : AttributeSelectorOperation()

    data class WithValue(
        val operator: AttributeSelectorOperator,
        val caseSensitive: Boolean,
        val expectedValue: String,
    ) : AttributeSelectorOperation()
}

sealed class AttributeSelectorOperator : ToCss {

    data object Equal : AttributeSelectorOperator()
    data object Includes : AttributeSelectorOperator()
    data object DashMatch : AttributeSelectorOperator()
    data object Prefix : AttributeSelectorOperator()
    data object Substring : AttributeSelectorOperator()
    data object Suffix : AttributeSelectorOperator()

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
    val specificity: Int
        get() = header.specificity

    val hasParent: Boolean
        get() = header.hasParent

    val pseudoElement: PseudoElement?
        get() {
            if (!header.hasPseudoElement) {
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

    fun rawIteratorMatchOrder(): Iterator<Component> {
        return components.iterator()
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

    fun iteratorSkipRelativeSelectorAnchor(): SelectorIterator {
        debug {
            val iterator = rawIteratorParseOrder()
            assert(iterator.next() is Component.RelativeSelectorAnchor)
            assert(iterator.next() is Component.Combinator)
        }
        return SelectorIterator(
            components.dropLast(2),
        )
    }

    fun combinatorOfRelativeSelectorAnchor(): Combinator {
        debug {
            val iterator = rawIteratorParseOrder()
            assert(iterator.next() is Component.RelativeSelectorAnchor)
            assert(iterator.next() is Component.Combinator)
        }
        return when (val component = components[components.lastIndex - 1]) {
            is Component.Combinator -> component.combinator
            else -> error("not a relative selector")
        }
    }

    fun replaceParent(parent: List<Selector>): Selector {
        val flags = SelectorFlags.of(header.flags) - SelectorFlags.HAS_PARENT
        val specificity = Specificity.fromInt(header.specificity)

        val parentSpecificity = Specificity.fromInt(selectorListSpecificityAndFlags(parent).specificity)

        fun replaceParentOnSelectorList(
            selectors: List<Selector>,
            parent: List<Selector>,
            specificity: Specificity,
            withSpecificity: Boolean,
        ): List<Selector> {
            var any = false

            val result = selectors.map { selector ->
                if (!selector.hasParent) return@map selector

                any = true
                selector.replaceParent(parent)
            }

            if (any && withSpecificity) {
                specificity += Specificity.fromInt(
                    selectorListSpecificityAndFlags(result).specificity -
                            selectorListSpecificityAndFlags(selectors).specificity
                )
            }

            return result
        }

        fun replaceParentOnRelativeSelectorList(
            relativeSelectors: List<RelativeSelector>,
            parent: List<Selector>,
            specificity: Specificity,
        ): List<RelativeSelector> {
            var any = false

            val result = relativeSelectors.map { relativeSelector ->
                if (!relativeSelector.selector.hasParent) return@map relativeSelector

                any = true
                RelativeSelector(
                    relativeSelector.selector.replaceParent(parent),
                    relativeSelector.matchHint,
                )
            }

            if (any) {
                specificity += Specificity.fromInt(
                    relativeSelectorListSpecificityAndFlags(result).specificity -
                            relativeSelectorListSpecificityAndFlags(relativeSelectors).specificity
                )
            }

            return result
        }

        fun replaceParentOnSelector(
            selector: Selector,
            parent: List<Selector>,
            specificity: Specificity,
        ): Selector {
            if (!selector.hasParent) return selector

            val result = selector.replaceParent(parent)
            specificity += Specificity.fromInt(result.specificity - selector.specificity)
            return result
        }

        val components = if (!hasParent) {
            specificity += parentSpecificity

            components + listOf(Component.Combinator(Combinator.Descendant), Component.Is(parent))
        } else {
            components.map { component ->
                when (component) {
                    is Component.LocalName,
                    is Component.ID,
                    is Component.Class,
                    is Component.AttributeInNoNamespace,
                    is Component.AttributeInNoNamespaceExists,
                    is Component.AttributeOther,
                    is Component.ExplicitAnyNamespace,
                    is Component.ExplicitNoNamespace,
                    is Component.ExplicitUniversalType,
                    is Component.DefaultNamespace,
                    is Component.Namespace,
                    is Component.Root,
                    is Component.Empty,
                    is Component.Scope,
                    is Component.NonTSFPseudoClass,
                    is Component.NonTSPseudoClass,
                    is Component.PseudoElement,
                    is Component.Combinator,
                    is Component.Part,
                    is Component.RelativeSelectorAnchor,
                    -> component

                    Component.ParentSelector -> {
                        specificity += parentSpecificity
                        Component.Is(parent)
                    }

                    is Component.Negation -> Component.Negation(
                        replaceParentOnSelectorList(component.selectors, parent, specificity, withSpecificity = true)
                    )

                    is Component.Is -> Component.Is(
                        replaceParentOnSelectorList(component.selectors, parent, specificity, withSpecificity = true)
                    )

                    is Component.Where -> Component.Is(
                        replaceParentOnSelectorList(component.selectors, parent, specificity, withSpecificity = false)
                    )

                    is Component.Has -> Component.Has(
                        replaceParentOnRelativeSelectorList(component.selectors, parent, specificity)
                    )

                    is Component.Host -> when {
                        component.selector != null -> Component.Host(
                            replaceParentOnSelector(component.selector, parent, specificity)
                        )

                        else -> component
                    }

                    is Component.Nth -> when {
                        component.selectors.isNotEmpty() -> Component.Nth(
                            component.data,
                            replaceParentOnSelectorList(component.selectors, parent, specificity, withSpecificity = true),
                        )

                        else -> component
                    }

                    is Component.Slotted -> Component.Slotted(
                        replaceParentOnSelector(component.selector, parent, specificity)
                    )
                }
            }
        }

        return Selector(SpecificityAndFlags(specificity.toInt(), flags.bits), components)
    }

    override fun toCss(writer: Writer) {
        rawIteratorTrueParseOrder().toCssJoining(writer)
    }

    override fun toString(): String {
        return "Selector[sr=${toCssString()}, spec=${specificity}]"
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

class CombinatorIterator private constructor(
    private val iterator: SelectorIterator,
) : Iterator<Combinator> {

    override fun hasNext(): Boolean = iterator.hasNextSequence()
    override fun next(): Combinator {
        val combinator = iterator.nextSequence()
        drainNonCombinators()
        return combinator
    }

    private fun drainNonCombinators() {
        while (iterator.hasNext()) {
            iterator.next()
        }
    }

    companion object {
        fun from(iterator: SelectorIterator): CombinatorIterator {
            val combinatorIterator = CombinatorIterator(iterator)
            combinatorIterator.drainNonCombinators()
            return combinatorIterator
        }
    }
}

enum class RelativeSelectorMatchHint {
    InChild,
    InSubtree,
    InSibling,
    InSiblingSubtree,
    InNextSibling,
    InNextSiblingSubtree,
}

private class CombinatorComposition(value: UByte) : U8Bitflags(value) {

    override val all: UByte get() = ALL

    companion object {
        const val DESCENDANTS: UByte = 0b0000_0001u
        const val SIBLINGS: UByte = 0b0000_0010u

        private val ALL = DESCENDANTS and SIBLINGS

        fun empty(): CombinatorComposition = CombinatorComposition(0u)
        fun all(): CombinatorComposition = CombinatorComposition(ALL)
        fun of(value: UByte): CombinatorComposition = CombinatorComposition(value and ALL)
    }
}

private fun CombinatorComposition.Companion.forRelativeSelector(selector: Selector): CombinatorComposition {
    val result = empty()
    for (combinator in CombinatorIterator.from(selector.iteratorSkipRelativeSelectorAnchor())) {
        when (combinator) {
            Combinator.Child, Combinator.Descendant -> result.add(DESCENDANTS)
            Combinator.NextSibling, Combinator.LaterSibling -> result.add(SIBLINGS)
            Combinator.Part, Combinator.SlotAssignment, Combinator.PseudoElement -> continue
        }
        if (result.isAll()) break
    }
    return result
}

data class RelativeSelector(
    val selector: Selector,
    val matchHint: RelativeSelectorMatchHint,
) {

    companion object {
        fun fromSelectorList(selectorList: SelectorList): List<RelativeSelector> {
            return selectorList.selectors.map { selector ->
                val matchHint = when (selector.combinatorOfRelativeSelectorAnchor()) {
                    Combinator.Descendant -> RelativeSelectorMatchHint.InSubtree
                    Combinator.Child -> {
                        val composition = CombinatorComposition.forRelativeSelector(selector)
                        if (composition.isEmpty() || composition.bits == CombinatorComposition.SIBLINGS) {
                            RelativeSelectorMatchHint.InChild
                        } else {
                            RelativeSelectorMatchHint.InSubtree
                        }
                    }

                    Combinator.NextSibling -> {
                        val composition = CombinatorComposition.forRelativeSelector(selector)
                        if (composition.isEmpty()) {
                            RelativeSelectorMatchHint.InNextSibling
                        } else if (composition.bits == CombinatorComposition.SIBLINGS) {
                            RelativeSelectorMatchHint.InSibling
                        } else if (composition.bits == CombinatorComposition.DESCENDANTS) {
                            RelativeSelectorMatchHint.InNextSiblingSubtree
                        } else {
                            RelativeSelectorMatchHint.InSiblingSubtree
                        }
                    }

                    Combinator.LaterSibling -> {
                        val composition = CombinatorComposition.forRelativeSelector(selector)
                        if (composition.isEmpty() || composition.bits == CombinatorComposition.SIBLINGS) {
                            RelativeSelectorMatchHint.InSibling
                        } else {
                            RelativeSelectorMatchHint.InSiblingSubtree
                        }
                    }

                    Combinator.Part, Combinator.SlotAssignment, Combinator.PseudoElement -> {
                        assert(false) { "unexpected combinator in relative selector" }
                        RelativeSelectorMatchHint.InSubtree
                    }
                }

                RelativeSelector(selector, matchHint)
            }
        }
    }
}

class SelectorList(val selectors: List<Selector>) : Iterable<Selector>, ToCss {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    override fun toCss(writer: Writer) {
        selectors.toCssJoining(writer, separator = ", ")
    }

    override fun toString(): String {
        return "SelectorList[${toCssString()}]"
    }

    companion object {

        fun parse(
            context: SelectorParserContext,
            input: Parser,
            parseRelative: ParseRelative,
        ): Result<SelectorList, ParseError> {
            return parseWithState(
                context,
                input,
                SelectorParsingState.empty(),
                ParseForgiving.No,
                parseRelative,
            )
        }

        fun parseWithState(
            context: SelectorParserContext,
            input: Parser,
            state: SelectorParsingState,
            recovery: ParseForgiving,
            parseRelative: ParseRelative,
        ): Result<SelectorList, ParseError> {
            val selectors = mutableListOf<Selector>()

            val forgiving = recovery == ParseForgiving.Yes
            while (true) {
                when (val selector = input.parseUntilBefore(Delimiters.Comma) { i -> parseSelector(context, i, state, parseRelative) }) {
                    is Ok -> selectors.add(selector.value)
                    is Err -> if (!forgiving) return selector
                }

                val token = when (val token = input.next()) {
                    is Ok -> token.value
                    is Err -> break
                }

                when (token) {
                    is Token.Comma -> continue
                    else -> error("unreachable")
                }
            }
            return Ok(SelectorList(selectors.resized()))
        }
    }
}
