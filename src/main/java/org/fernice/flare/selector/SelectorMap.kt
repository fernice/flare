/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.ApplicableDeclarationBlock
import org.fernice.flare.dom.Element
import org.fernice.flare.style.Rule
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.ruletree.CascadeLevel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An optimized implementation of a Map that tries to accelerate the lookup of [Rule] for an [Element]
 * through trying to find the most fitting bucket for each rule and its corresponding selector in order
 * to reduce the rules that need to be matched.
 */
class SelectorMap {

    private val idHash = RuleMap.new()
    private val classHash = RuleMap.new()
    private val localNameHash = RuleMap.new()
    private val other = CopyOnWriteArrayList<Rule>()
    private var count = 0

    fun getAllMatchingRules(
        element: Element,
        matchingRules: MutableList<ApplicableDeclarationBlock>,
        context: MatchingContext,
        cascadeLevel: CascadeLevel
    ) {

        if (isEmpty()) {
            return
        }

        val originalLength = matchingRules.size

        val quirksMode = context.quirksMode()

        element.id?.let { id ->
            idHash.get(id, quirksMode)?.let { rules ->
                getMatchingRules(
                    element,
                    rules,
                    matchingRules,
                    context,
                    cascadeLevel
                )
            }
        }

        element.classes.forEach { styleClass ->
            classHash.get(styleClass, quirksMode)?.let { rules ->
                getMatchingRules(
                    element,
                    rules,
                    matchingRules,
                    context,
                    cascadeLevel
                )
            }
        }

        localNameHash.get(element.localName, quirksMode)?.let { rules ->
            getMatchingRules(
                element,
                rules,
                matchingRules,
                context,
                cascadeLevel
            )
        }

        getMatchingRules(
            element,
            other,
            matchingRules,
            context,
            cascadeLevel
        )

        matchingRules.subList(originalLength, matchingRules.size)
            .sortWith(RuleComparator.instance)
    }

    private fun getMatchingRules(
        element: Element,
        rules: List<Rule>,
        matchingRules: MutableList<ApplicableDeclarationBlock>,
        context: MatchingContext,
        cascadeLevel: CascadeLevel
    ) {
        for (rule in rules) {
            if (matchesSelector(rule.selector, rule.hashes, element, context)) {
                matchingRules.add(rule.toApplicableDeclaration(cascadeLevel))
            }
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
                    is Component.Class -> bucket = component
                    is Component.LocalName -> if (bucket == null) bucket = component
                    is Component.Negation -> if (bucket == null) bucket = findBucket(component.iterator())
                    else -> {
                    }
                }
            }

            if (!iterator.hasNextSequence()) break
            val combinator = iterator.nextSequence()
            if (combinator !is Combinator.PseudoElement) break
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

private class RuleComparator : Comparator<ApplicableDeclarationBlock> {

    override fun compare(o1: ApplicableDeclarationBlock, o2: ApplicableDeclarationBlock): Int {
        val c = o1.specificity.compareTo(o2.specificity)

        if (c != 0) {
            return c
        }

        return o1.sourceOrder().compareTo(o2.sourceOrder())
    }

    companion object {
        val instance: RuleComparator by lazy { RuleComparator() }
    }
}

class RuleMap {

    companion object {

        fun new(): RuleMap {
            return RuleMap()
        }
    }

    private val map = mutableMapOf<String, MutableList<Rule>>()

    fun entry(key: String): MutableList<Rule> {
        return map.computeIfAbsent(key) { CopyOnWriteArrayList() }
    }

    fun get(key: String, quirksMode: QuirksMode): List<Rule>? {
        return if (quirksMode == QuirksMode.QUIRKS) {
            map[key.lowercase()]
        } else {
            map[key]
        }
    }

    fun clear() {
        map.clear()
    }
}
