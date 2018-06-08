package de.krall.flare.selector

import de.krall.flare.ApplicableDeclarationBlock
import de.krall.flare.dom.Element
import de.krall.flare.std.*
import de.krall.flare.style.Rule
import de.krall.flare.style.parser.QuirksMode
import de.krall.flare.style.ruletree.CascadeLevel

class SelectorMap {

    private val idHash = RuleMap.new()
    private val classHash = RuleMap.new()
    private val localNameHash = RuleMap.new()
    private val other = mutableListOf<Rule>()
    private var count = 0

    fun getAllMatchingRules(element: Element,
                            matchingRules: MutableList<ApplicableDeclarationBlock>,
                            context: MatchingContext,
                            cascadeLevel: CascadeLevel) {

        if (isEmpty()) {
            return
        }

        val quirksMode = context.quirksMode()

        element.id().ifLet { id ->
            idHash.get(id, quirksMode).ifLet { rules ->
                getMatchingRules(
                        element,
                        rules,
                        matchingRules,
                        context,
                        cascadeLevel
                )
            }
        }

        element.classes().forEach { styleClass ->
            classHash.get(styleClass, quirksMode).ifLet { rules ->
                getMatchingRules(
                        element,
                        rules,
                        matchingRules,
                        context,
                        cascadeLevel
                )
            }
        }

        localNameHash.get(element.localName(), quirksMode).ifLet { rules ->
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
    }

    private fun getMatchingRules(element: Element,
                                 rules: List<Rule>,
                                 matchingRules: MutableList<ApplicableDeclarationBlock>,
                                 context: MatchingContext,
                                 cascadeLevel: CascadeLevel) {
        for (rule in rules) {
            if (matchesSelector(rule.selector, Some(rule.hashes), element, context)) {
                matchingRules.add(rule.toApplicableDeclaration(cascadeLevel))
            }
        }
    }

    fun insert(rule: Rule) {
        val bucket = findBucket(rule.selector.iter())

        val list = when (bucket) {
            is Some -> {
                when (bucket.value) {
                    is Component.ID -> idHash.entry(bucket.value.id)
                    is Component.Class -> classHash.entry(bucket.value.styleClass)
                    is Component.LocalName -> {
                        if (bucket.value.localName != bucket.value.localNameLower) {
                            localNameHash.entry(bucket.value.localNameLower)
                        } else {
                            localNameHash.entry(bucket.value.localName)
                        }
                    }
                    else -> throw IllegalStateException("unreachable")
                }
            }
            is None -> {
                other
            }
        }

        list.add(rule)
        count++
    }

    private fun findBucket(iter: SelectorIter): Option<Component> {
        var bucket: Option<Component> = None()
        while (true) {
            for (component in iter) {
                when (component) {
                    is Component.ID -> return Some(component)
                    is Component.Class -> bucket = Some(component)
                    is Component.LocalName -> {
                        if (bucket.isNone()) {
                            bucket = Some(component)
                        }
                    }
                    is Component.Negation -> findBucket(component.iter())
                    else -> {
                    }
                }
            }

            val next = iter.nextSequence()
            if (next.isNone() || next is Some && next.value !is Combinator.PseudoElement) {
                break
            }
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

    companion object {

        fun new(): RuleMap {
            return RuleMap()
        }
    }

    private val map = mutableMapOf<String, MutableList<Rule>>()

    fun entry(key: String): MutableList<Rule> {
        return map.computeIfAbsent(key, { mutableListOf() })
    }

    fun get(key: String, quirksMode: QuirksMode): Option<List<Rule>> {
        return if (quirksMode == QuirksMode.QUIRKS) {
            map[key.toLowerCase()].into()
        } else {
            map[key].into()
        }
    }

    fun clear() {
        map.clear()
    }
}