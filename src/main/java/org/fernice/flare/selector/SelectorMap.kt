/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.dom.Element
import org.fernice.flare.style.*
import org.fernice.flare.style.ruletree.CascadeLevel

//private val RARE_PSEUDO_CLASS_STATES = EnumSet.of(
//    NonTSPseudoClass.Fullscreen,
//    NonTSPseudoClass.Focus,
//    NonTSPseudoClass.Visited,
//)

/**
 * An optimized implementation of a Map that tries to accelerate the lookup of [Rule] for an [Element]
 * through trying to find the most fitting bucket for each rule and its corresponding selector in order
 * to reduce the rules that need to be matched.
 */
class SelectorMap {

    private val root = mutableListOf<Rule>()
    private val idHash = RuleMap()
    private val classHash = RuleMap()
    private val localNameHash = RuleMap()
    private val namespaceHash = RuleMap()
//    private val rarePseudoClass = mutableListOf<Rule>()
    private val other = mutableListOf<Rule>()
    private var count = 0

    fun getAllMatchingRules(
        element: Element,
        context: MatchingContext,
        cascadeLevel: CascadeLevel,
        cascadeData: CascadeData,
        matchingRules: ApplicableDeclarationList,
    ) {
        if (isEmpty()) return

        val quirksMode = context.quirksMode

        if (element.isRoot()) {
            getMatchingRules(
                element,
                root,
                context,
                cascadeLevel,
                cascadeData,
                matchingRules,
            )
        }

        element.id?.let { id ->
            idHash.get(id, quirksMode)?.let { rules ->
                getMatchingRules(
                    element,
                    rules,
                    context,
                    cascadeLevel,
                    cascadeData,
                    matchingRules,
                )
            }
        }

        element.classes.forEach { styleClass ->
            classHash.get(styleClass, quirksMode)?.let { rules ->
                getMatchingRules(
                    element,
                    rules,
                    context,
                    cascadeLevel,
                    cascadeData,
                    matchingRules,
                )
            }
        }

        localNameHash.get(element.localName, quirksMode)?.let { rules ->
            getMatchingRules(
                element,
                rules,
                context,
                cascadeLevel,
                cascadeData,
                matchingRules,
            )
        }

        element.namespace?.let { namespace ->
            namespaceHash.get(namespace.url, quirksMode)?.let { rules ->
                getMatchingRules(
                    element,
                    rules,
                    context,
                    cascadeLevel,
                    cascadeData,
                    matchingRules,
                )
            }
        }

        getMatchingRules(
            element,
            other,
            context,
            cascadeLevel,
            cascadeData,
            matchingRules,
        )
    }

    private fun getMatchingRules(
        element: Element,
        rules: List<Rule>,
        context: MatchingContext,
        cascadeLevel: CascadeLevel,
        cascadeData: CascadeData,
        matchingRules: ApplicableDeclarationList,
    ) {
        for (rule in rules) {
            if (rule.condition != null && !rule.condition.matches(context.device, context.quirksMode, context.ruleConditionCache).toBoolean(unknown = false)) {
                continue
            }

            if (!matchesSelector(rule.selector, rule.hashes, element, context)) {
                continue
            }

            if (rule.containerConditionId != ContainerConditionId.None) {
                if (!cascadeData.containerConditionMatches(rule.containerConditionId, context.device, element)) {
                    continue
                }
            }

            matchingRules.add(rule.toApplicableDeclarationBlock(cascadeLevel, cascadeData))
        }
    }

    fun insert(rule: Rule) {
        count++

        fun insertIntoBucket(bucket: Bucket) {
            val list = when (bucket) {
                is Bucket.Root -> root
                is Bucket.ID -> idHash.entry(bucket.id)
                is Bucket.Class -> classHash.entry(bucket.name)
                is Bucket.Attribute -> {
                    if (bucket.name != bucket.lowerName) {
                        localNameHash.entry(bucket.lowerName)
                    } else {
                        localNameHash.entry(bucket.name)
                    }
                }

                is Bucket.LocalName -> {
                    if (bucket.name != bucket.lowerName) {
                        localNameHash.entry(bucket.lowerName)
                    } else {
                        localNameHash.entry(bucket.name)
                    }
                }

                is Bucket.Namespace -> namespaceHash.entry(bucket.namespace)
//                is Bucket.RarePseudoClass -> rarePseudoClass
                is Bucket.Universal -> other
            }

            list.add(rule)
        }

        val disjointBuckets = mutableListOf<Bucket>()
        val bucket = findBucket(rule.selector.iterator(), disjointBuckets)

        if (disjointBuckets.isNotEmpty() && disjointBuckets.all { it.moreSpecificThan(bucket) }) {
            for (disjointBucket in disjointBuckets) {
                insertIntoBucket(disjointBucket)
            }
        } else {
            insertIntoBucket(bucket)
        }
    }

    fun clear() {
        idHash.clear()
        classHash.clear()
        localNameHash.clear()
        other.clear()
        count = 0
    }

    fun isEmpty(): Boolean {
        return count == 0
    }

    fun size(): Int {
        return count
    }
}

class RuleMap {

    private val map = mutableMapOf<String, MutableList<Rule>>()

    fun entry(key: String): MutableList<Rule> {
        return map.computeIfAbsent(key) { mutableListOf() }
    }

    fun get(key: String, quirksMode: QuirksMode): List<Rule>? {
        return if (quirksMode == QuirksMode.Quirks) {
            map[key.lowercase()]
        } else {
            map[key]
        }
    }

    fun clear() {
        map.clear()
    }
}

private sealed class Bucket {
    data object Universal : Bucket()
    data class Namespace(val namespace: String) : Bucket()

    //    data object RarePseudoClass : Bucket()
    data class Attribute(val name: String, val lowerName: String) : Bucket()
    data class LocalName(val name: String, val lowerName: String) : Bucket()
    data class Class(val name: String) : Bucket()
    data class ID(val id: String) : Bucket()
    data object Root : Bucket()

    fun specificity(): Int {
        return when (this) {
            is Universal -> 0
            is Namespace -> 1
//            is RarePseudoClass -> 2
            is Attribute -> 3
            is LocalName -> 4
            is Class -> 5
            is ID -> 6
            is Root -> 7
        }
    }

    fun moreOrEquallySpecificThan(bucket: Bucket): Boolean {
        return specificity() >= bucket.specificity()
    }

    fun moreSpecificThan(bucket: Bucket): Boolean {
        return specificity() > bucket.specificity()
    }
}

private fun findBucket(iterator: SelectorIterator, disjointBuckets: MutableList<Bucket>): Bucket {
    var bucket: Bucket = Bucket.Universal
    while (true) {
        for (component in iterator) {
            val componentBucket = specificBucketFor(component, disjointBuckets)

            if (componentBucket.moreOrEquallySpecificThan(bucket)) {
                bucket = componentBucket
            }
        }

        if (!iterator.hasNextSequence()) break
        val combinator = iterator.nextSequence()
        if (combinator != Combinator.PseudoElement) break
    }
    return bucket
}

private fun specificBucketFor(component: Component, disjointBuckets: MutableList<Bucket>): Bucket {
    return when (component) {
        is Component.Root -> Bucket.Root
        is Component.ID -> Bucket.ID(component.id)
        is Component.Class -> Bucket.Class(component.styleClass)
        is Component.AttributeInNoNamespace -> Bucket.Attribute(component.localName, component.localNameLower)
        is Component.AttributeInNoNamespaceExists -> Bucket.Attribute(component.localName, component.localNameLower)
        is Component.AttributeOther -> Bucket.Attribute(component.localName, component.localNameLower)
        is Component.LocalName -> Bucket.LocalName(component.localName, component.localNameLower)
        is Component.Namespace -> Bucket.Namespace(component.namespace.url)
        is Component.DefaultNamespace -> Bucket.Namespace(component.namespace.url)

        is Component.Slotted -> findBucket(component.selector.iterator(), disjointBuckets)
        is Component.Host -> {
            if (component.selector != null) {
                findBucket(component.selector.iterator(), disjointBuckets)
            } else {
                Bucket.Universal
            }
        }

        is Component.Is -> {
            if (component.selectors.size == 1) {
                findBucket(component.selectors[0].iterator(), disjointBuckets)
            } else {
                for (selector in component.selectors) {
                    val bucket = findBucket(selector.iterator(), disjointBuckets)
                    disjointBuckets.add(bucket)
                }
                Bucket.Universal
            }
        }

        is Component.Where -> {
            if (component.selectors.size == 1) {
                findBucket(component.selectors[0].iterator(), disjointBuckets)
            } else {
                for (selector in component.selectors) {
                    val bucket = findBucket(selector.iterator(), disjointBuckets)
                    disjointBuckets.add(bucket)
                }
                Bucket.Universal
            }
        }

//        is Component.NonTSPseudoClass -> {
//            if (component.pseudoClass in RARE_PSEUDO_CLASS_STATES) {
//                Bucket.RarePseudoClass
//            } else {
//                Bucket.Universal
//            }
//        }

        else -> Bucket.Universal
    }
}
