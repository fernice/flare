/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.dom.Element
import org.fernice.flare.style.*
import org.fernice.flare.style.ruletree.CascadeLevel

/**
 * An optimized implementation of a Map that tries to accelerate the lookup of [Rule] for an [Element]
 * through trying to find the most fitting bucket for each rule and its corresponding selector in order
 * to reduce the rules that need to be matched.
 */
class SelectorMap {

    private val idHash = RuleMap()
    private val classHash = RuleMap()
    private val localNameHash = RuleMap()
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
        val list = when (val bucket = findBucket(rule.selector.iterator())) {
            is Component.ID -> idHash.entry(bucket.id)
            is Component.Class -> classHash.entry(bucket.styleClass)
            is Component.LocalName -> {
                if (bucket.localName != bucket.localNameLower) {
                    localNameHash.entry(bucket.localNameLower)
                } else {
                    localNameHash.entry(bucket.localName)
                }
            }

            null -> other
            else -> error("unsupported bucket type: ${bucket::class.java.name}")
        }

        list.add(rule)
        count++
    }

    private fun findBucket(iterator: SelectorIterator): Component? {
        var bucket: Component? = null
        while (true) {
            for (component in iterator) {
                when (component) {
                    is Component.ID -> return component
                    is Component.Class -> if (bucket == null || bucket !is Component.Class) bucket = component
                    is Component.LocalName -> if (bucket == null) bucket = component
                    is Component.Is -> {
                        for (selector in component.selectors) {
                            if (bucket == null) bucket = findBucket(selector.iterator())
                        }
                    }

                    is Component.Where -> {
                        for (selector in component.selectors) {
                            if (bucket == null) bucket = findBucket(selector.iterator())
                        }
                    }

                    is Component.Negation -> {
                        for (selector in component.selectors) {
                            if (bucket == null) bucket = findBucket(selector.iterator())
                        }
                    }

                    else -> {}
                }
            }

            if (!iterator.hasNextSequence()) break
            val combinator = iterator.nextSequence()
            if (combinator != Combinator.PseudoElement) break
        }
        return bucket
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
