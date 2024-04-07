/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.stylesheet

import org.fernice.flare.dom.Device
import org.fernice.flare.style.QuirksMode
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
            if (!effective) continue

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
        ): Pair<Iterator<CssRule>?, Boolean> {
            return when (rule) {
                is CssRule.Style -> {
                    rule.styleRule.rules?.iterator() to true
                }
            }
        }
    }
}

interface NestedRuleIterationCondition

object EffectiveRules : NestedRuleIterationCondition
