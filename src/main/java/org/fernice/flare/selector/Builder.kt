/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.std.U8Bitflags
import org.fernice.std.resized
import kotlin.math.max

class SelectorBuilder {

    private val simpleSelectors = mutableListOf<Component>()
    private val combinators = mutableListOf<Pair<Combinator, Int>>()
    private var currentLength = 0

    fun pushSimpleSelector(component: Component) {
        assert(component !is Component.Combinator) { "What is he doing here?" }

        simpleSelectors.add(component)
        currentLength += 1
    }

    fun pushCombinator(combinator: Combinator) {
        combinators.add(Pair(combinator, currentLength))
        currentLength = 0
    }

    fun isEmpty(): Boolean {
        return simpleSelectors.isEmpty() && combinators.isEmpty()
    }

    fun hasDanglingCombinator(): Boolean {
        return combinators.isNotEmpty() && currentLength == 0
    }

    fun build(): Selector {
        val specificity = specificityAndFlags(simpleSelectors)

        return buildWithSpecificityAndFlags(specificity)
    }

    fun buildWithSpecificityAndFlags(specificityAndFlags: SpecificityAndFlags): Selector {
        val selector = mutableListOf<Component>()

        val combinatorIter = combinators.reversed().iterator()

        var upper = simpleSelectors.size
        var lower = upper - currentLength

        do {
            selector.addAll(simpleSelectors.subList(lower, upper))

            if (!combinatorIter.hasNext()) break

            val (combinator, length) = combinatorIter.next()

            upper = lower
            lower -= length

            selector.add(Component.Combinator(combinator))
        } while (true)

        return Selector(specificityAndFlags, selector.resized())
    }
}

private const val MAX_10_BIT = (1 shl 10) - 1

class Specificity(
    var idSelectors: Int,
    var classLikeSelectors: Int,
    var elementSelectors: Int,
) {

    operator fun plusAssign(specificity: Specificity) {
        idSelectors += specificity.idSelectors
        classLikeSelectors += specificity.classLikeSelectors
        elementSelectors += specificity.elementSelectors
    }

    fun toInt(): Int {
        return (this.idSelectors.coerceAtMost(MAX_10_BIT) shl 20) or
                (this.classLikeSelectors.coerceAtMost(MAX_10_BIT) shl 10) or
                (this.elementSelectors.coerceAtMost(MAX_10_BIT))
    }

    companion object {
        fun default(): Specificity {
            return Specificity(
                idSelectors = 0,
                classLikeSelectors = 0,
                elementSelectors = 0,
            )
        }

        fun fromInt(value: Int): Specificity {
            return Specificity(
                idSelectors = (value shr 20) and MAX_10_BIT,
                classLikeSelectors = (value shr 10) and MAX_10_BIT,
                elementSelectors = value and MAX_10_BIT,
            )
        }
    }
}

class SelectorFlags(value: UByte) : U8Bitflags(value) {

    override val all: UByte get() = ALL

    operator fun plus(value: UByte): SelectorFlags = of(this.value or value)
    operator fun minus(value: UByte): SelectorFlags = of(this.value and value.inv())

    companion object {
        const val HAS_PSEUDO_ELEMENT: UByte = 0b0000_0001u
        const val HAS_SLOTTED: UByte = 0b0000_0010u
        const val HAS_PART: UByte = 0b0000_0100u
        const val HAS_PARENT: UByte = 0b0000_1000u

        private val ALL = HAS_PSEUDO_ELEMENT or HAS_SLOTTED or HAS_PART or HAS_PARENT

        fun empty(): SelectorFlags = SelectorFlags(0u)
        fun all(): SelectorFlags = SelectorFlags(ALL)
        fun of(value: UByte): SelectorFlags = SelectorFlags(value and ALL)
    }
}

internal fun selectorListSpecificityAndFlags(iterable: Iterable<Selector>): SpecificityAndFlags {
    var specificity = 0
    val flags = SelectorFlags.empty()
    for (selector in iterable) {
        specificity = max(selector.specificity, specificity)
        if (selector.hasParent) {
            flags.add(SelectorFlags.HAS_PARENT)
        }
    }
    return SpecificityAndFlags(specificity, flags.bits)
}

internal fun relativeSelectorListSpecificityAndFlags(iterable: Iterable<RelativeSelector>): SpecificityAndFlags {
    return selectorListSpecificityAndFlags(iterable.map { it.selector })
}

private fun specificityAndFlags(iterable: Iterable<Component>): SpecificityAndFlags {
    return complexSelectorSpecificityAndFlags(iterable)
}

private fun complexSelectorSpecificityAndFlags(iterable: Iterable<Component>): SpecificityAndFlags {
    fun simpleSelectorSpecificity(simpleSelector: Component, specificity: Specificity, flags: SelectorFlags) {
        when (simpleSelector) {
            is Component.Combinator -> error("unreachable")
            is Component.ParentSelector -> flags.add(SelectorFlags.HAS_PARENT)
            is Component.Part -> {
                flags.add(SelectorFlags.HAS_PART)
                specificity.elementSelectors += 1
            }

            is Component.PseudoElement -> {
                flags.add(SelectorFlags.HAS_PSEUDO_ELEMENT)
                specificity.elementSelectors += 1
            }

            is Component.LocalName -> specificity.elementSelectors += 1
            is Component.Slotted -> {
                flags.add(SelectorFlags.HAS_SLOTTED)
                specificity.elementSelectors += 1

                specificity += Specificity.fromInt(simpleSelector.selector.specificity)
                if (simpleSelector.selector.hasParent) {
                    flags.add(SelectorFlags.HAS_PARENT)
                }
            }

            is Component.Host -> {
                specificity.classLikeSelectors += 1

                if (simpleSelector.selector != null) {
                    specificity += Specificity.fromInt(simpleSelector.selector.specificity)
                    if (simpleSelector.selector.hasParent) {
                        flags.add(SelectorFlags.HAS_PARENT)
                    }
                }
            }

            is Component.ID -> specificity.idSelectors += 1

            is Component.Class,
            is Component.AttributeInNoNamespace,
            is Component.AttributeInNoNamespaceExists,
            is Component.AttributeOther,
            is Component.Root,
            is Component.Empty,
            is Component.Scope,
            is Component.NonTSPseudoClass,
            is Component.NonTSFPseudoClass,
            -> {
                specificity.classLikeSelectors += 1
            }

            is Component.Nth -> {
                specificity.classLikeSelectors += 1

                if (simpleSelector.selectors.isNotEmpty()) {
                    val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                    specificity += Specificity.fromInt(specificityAndFlags.specificity)
                    flags.add(specificityAndFlags.flags)
                }
            }

            is Component.Is -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificityAndFlags.specificity)
                flags.add(specificityAndFlags.flags)
            }

            is Component.Where -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                // where does not contribute the specificity of its selectors
                flags.add(specificityAndFlags.flags)
            }

            is Component.Negation -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificityAndFlags.specificity)
                flags.add(specificityAndFlags.flags)
            }

            is Component.Has -> {
                val specificityAndFlags = relativeSelectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificityAndFlags.specificity)
                flags.add(specificityAndFlags.flags)
            }

            is Component.ExplicitAnyNamespace,
            is Component.ExplicitNoNamespace,
            is Component.ExplicitUniversalType,
            is Component.DefaultNamespace,
            is Component.Namespace,
            is Component.RelativeSelectorAnchor,
            -> {
                // do not contribute to specificity
            }
        }
    }

    val specificity = Specificity.default()
    val flags = SelectorFlags.empty()
    for (simpleSelector in iterable) {
        simpleSelectorSpecificity(simpleSelector, specificity, flags)
    }
    return SpecificityAndFlags(specificity.toInt(), flags.bits)
}

class SpecificityAndFlags(
    val specificity: Int,
    val flags: UByte,
) {

    private fun hasFlags(value: UByte): Boolean {
        return (flags and value) != 0u.toUByte()
    }

    val hasPseudoElement: Boolean
        get() = hasFlags(SelectorFlags.HAS_PSEUDO_ELEMENT)

    val hasSlotted: Boolean
        get() = hasFlags(SelectorFlags.HAS_SLOTTED)

    val hasPart: Boolean
        get() = hasFlags(SelectorFlags.HAS_PART)

    val hasParent: Boolean
        get() = hasFlags(SelectorFlags.HAS_PARENT)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpecificityAndFlags) return false

        if (specificity != other.specificity) return false
        if (flags != other.flags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = specificity
        result = 31 * result + flags.hashCode()
        return result
    }

    override fun toString(): String {
        val flags = buildSet {
            if (hasPseudoElement) add("pseudo-element")
            if (hasSlotted) add("slotted")
            if (hasPart) add("part")
            if (hasParent) add("parent")
        }
        return "SpecificityAndFlags[specificity: $specificity, flags: $flags]"
    }
}
