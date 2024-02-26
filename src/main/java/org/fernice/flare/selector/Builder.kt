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

//class Flags(
//    private var value: Byte,
//) {
//
//    fun add(flags: Byte) {
//        value = value or flags
//    }
//
//    fun remove(flags: Byte) {
//        value = value and flags.inv()
//    }
//
//    fun intersects(flags: Byte): Boolean {
//        return (value and flags) != 0.toByte()
//    }
//
//    fun toByte(): Byte = value
//
//    companion object {
//        const val HAS_PSEUDO_ELEMENT = (1 shl 0).toByte()
//        const val HAS_SLOTTED = (1 shl 1).toByte()
//        const val HAS_PART = (1 shl 2).toByte()
//        const val HAS_PARENT = (1 shl 3).toByte()
//
//        fun default(): Flags = Flags(0)
//
//        fun fromByte(value: Byte): Flags = Flags(value)
//    }
//}

class Flags(value: UByte) : U8Bitflags(value) {

    override val all: UByte get() = ALL

    companion object {
        const val HAS_PSEUDO_ELEMENT: UByte = 0b0000_0001u
        const val HAS_SLOTTED: UByte = 0b0000_0010u
        const val HAS_PART: UByte = 0b0000_0100u
        const val HAS_PARENT: UByte = 0b0000_1000u

        private val ALL = HAS_PSEUDO_ELEMENT or HAS_SLOTTED or HAS_PART or HAS_PARENT

        fun empty(): Flags = Flags(0u)
        fun all(): Flags = Flags(ALL)
        fun of(value: UByte): Flags = Flags(value and ALL)
    }
}

private fun specificityAndFlags(iterable: Iterable<Component>): SpecificityAndFlags {
    return complexSelectorSpecificityAndFlags(iterable)
}

private fun selectorListSpecificityAndFlags(iterable: Iterable<Selector>): SpecificityAndFlags {
    var specificity = 0
    val flags = Flags.empty()
    for (selector in iterable) {
        specificity = max(selector.specificity, specificity)
        if (selector.hasParent) {
            flags.add(Flags.HAS_PARENT)
        }
    }
    return SpecificityAndFlags(specificity, flags.bits)
}

private fun relativeSelectorListSpecificityAndFlags(iterable: Iterable<RelativeSelector>): SpecificityAndFlags {
    return selectorListSpecificityAndFlags(iterable.map { it.selector })
}

private fun complexSelectorSpecificityAndFlags(iterable: Iterable<Component>): SpecificityAndFlags {
    fun simpleSelectorSpecificity(simpleSelector: Component, specificity: Specificity, flags: Flags) {
        when (simpleSelector) {
            is Component.Combinator -> error("unreachable")
            is Component.ParentSelector -> flags.add(Flags.HAS_PARENT)
            is Component.Part -> {
                flags.add(Flags.HAS_PART)
                specificity.elementSelectors += 1
            }

            is Component.PseudoElement -> {
                flags.add(Flags.HAS_PSEUDO_ELEMENT)
                specificity.elementSelectors += 1
            }

            is Component.LocalName -> specificity.elementSelectors += 1
            is Component.Slotted -> {
                flags.add(Flags.HAS_SLOTTED)
                specificity.elementSelectors += 1

                specificity += Specificity.fromInt(simpleSelector.selector.specificity)
                if (simpleSelector.selector.hasParent) {
                    flags.add(Flags.HAS_PARENT)
                }
            }

            is Component.Host -> {
                specificity.classLikeSelectors += 1

                if (simpleSelector.selector != null) {
                    specificity += Specificity.fromInt(simpleSelector.selector.specificity)
                    if (simpleSelector.selector.hasParent) {
                        flags.add(Flags.HAS_PARENT)
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
            is Component.Nth,
            is Component.NonTSPseudoClass,
            is Component.NonTSFPseudoClass,
            -> {
                specificity.classLikeSelectors += 1
            }

            is Component.NthOfType -> {
                specificity.classLikeSelectors += 1

                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificity.toInt())
                flags.add(specificityAndFlags.flags)
            }

            is Component.Is -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificity.toInt())
                flags.add(specificityAndFlags.flags)
            }

            is Component.Where -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                // where does not contribute the specificity of its selectors
                flags.add(specificityAndFlags.flags)
            }

            is Component.Negation -> {
                val specificityAndFlags = selectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificity.toInt())
                flags.add(specificityAndFlags.flags)
            }

            is Component.Has -> {
                val specificityAndFlags = relativeSelectorListSpecificityAndFlags(simpleSelector.selectors)
                specificity += Specificity.fromInt(specificity.toInt())
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
    val flags = Flags.empty()
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
        get() = hasFlags(Flags.HAS_PSEUDO_ELEMENT)

    val hasSlotted: Boolean
        get() = hasFlags(Flags.HAS_SLOTTED)

    val hasPart: Boolean
        get() = hasFlags(Flags.HAS_PART)

    val hasParent: Boolean
        get() = hasFlags(Flags.HAS_PARENT)
}
