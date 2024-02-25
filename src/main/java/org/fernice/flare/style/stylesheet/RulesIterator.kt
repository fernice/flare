/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.stylesheet

import org.fernice.flare.dom.Device
import org.fernice.flare.style.QuirksMode
import org.fernice.std.Kleenean
import java.util.*

class RulesIterator<C : NestedRuleIterationCondition>(
    private val device: Device,
    private val quirksMode: QuirksMode,
    private val condition: C,
    private val stack: LinkedList<Iterator<CssRule>>,
) : Iterator<CssRule> {

    override fun hasNext(): Boolean {
        return stack.isNotEmpty() && stack.any { it.hasNext() }
    }

    override fun next(): CssRule {
        while (stack.isNotEmpty()) {
            val iterator = stack.peek()
            if (!iterator.hasNext()) {
                stack.pop()
                continue
            }
            val rule = iterator.next()

            val (children, effective) = children(rule, device, quirksMode, condition)
            if (effective != Effective.True) continue

            if (children != null) {
                stack.push(children)
            }

            return rule
        }
        throw NoSuchElementException()
    }

    fun skipChildren() {
        stack.pop()
    }

    companion object {

        fun <C : NestedRuleIterationCondition> of(
            device: Device,
            quirksMode: QuirksMode,
            condition: C,
            rules: Iterator<CssRule>,
        ): RulesIterator<C> {
            val stack = LinkedList<Iterator<CssRule>>()
            stack.push(rules)
            return RulesIterator(
                device,
                quirksMode,
                condition,
                stack,
            )
        }

        fun children(
            rule: CssRule,
            device: Device,
            quirksMode: QuirksMode,
            condition: NestedRuleIterationCondition,
        ): Pair<Iterator<CssRule>?, Effective> {
            return when (rule) {
                is CssRule.Style -> {
                    rule.styleRule.rules?.iterator() to Effective.True
                }

                is CssRule.Media -> {
                    val effective = condition.isMediaEffective(rule.mediaRule, device, quirksMode)
                    if (effective == Effective.False) {
                        return null to Effective.False
                    }
                    rule.mediaRule.rules.iterator() to effective
                }
            }
        }
    }
}

interface NestedRuleIterationCondition {
    fun isMediaEffective(
        rule: MediaRule,
        device: Device,
        quirksMode: QuirksMode,
    ): Effective
}

object EffectiveRules : NestedRuleIterationCondition {
    override fun isMediaEffective(
        rule: MediaRule,
        device: Device,
        quirksMode: QuirksMode,
    ): Effective {
        return when (rule.condition.matches(device, quirksMode)) {
            Kleenean.True -> Effective.True
            else -> Effective.False
        }
    }
}

object PotentiallyEffectiveRules : NestedRuleIterationCondition {
    override fun isMediaEffective(
        rule: MediaRule,
        device: Device,
        quirksMode: QuirksMode,
    ): Effective {
        return when (rule.condition.matches(device, quirksMode)) {
            Kleenean.True -> Effective.True
            Kleenean.False -> Effective.False
            Kleenean.Unknown -> Effective.Indeterminable(rule.condition)
        }
    }
}


sealed class Effective {
    data object True : Effective()
    data object False : Effective()
    data class Indeterminable(val condition: EffectiveCondition) : Effective()
}

interface EffectiveCondition {

    /**
     * Returns whether the condition is effective. [Kleenean.Unknown] is returned, if the
     * condition is indeterminable for the given parameters.
     */
    fun matches(
        device: Device,
        quirksMode: QuirksMode,
    ): Kleenean
}