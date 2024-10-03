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

class RuleCondition(
    val condition: EffectiveCondition,
    val parent: RuleCondition? = null,
) {
    fun matches(
        device: Device,
        quirksMode: QuirksMode,
        cache: RuleConditionCache?,
    ): Kleenean {
        if (cache != null) {
            val matches = cache[this]
            if (matches != null) return matches
        }

        val matches = matchesInternally(device, quirksMode, cache)

        if (cache != null) {
            cache[this] = matches
        }

        return matches
    }

    private fun matchesInternally(
        device: Device,
        quirksMode: QuirksMode,
        cache: RuleConditionCache?,
    ): Kleenean {
        if (parent != null) {
            val parentMatches = parent.matches(device, quirksMode, cache)
            if (parentMatches != Kleenean.True) return parentMatches
        }

        return condition.matches(device, quirksMode)
    }

    fun derive(condition: EffectiveCondition): RuleCondition {
        return RuleCondition(condition, this)
    }
}

interface RuleConditionCache {
    operator fun get(condition: RuleCondition): Kleenean?
    operator fun set(condition: RuleCondition, result: Kleenean)
}

class SimpleRuleConditionCache : RuleConditionCache {
    private val rules = IdentityHashMap<RuleCondition, Kleenean>()

    override fun get(condition: RuleCondition): Kleenean? {
        return rules[condition]
    }

    override fun set(condition: RuleCondition, result: Kleenean) {
        rules[condition] = result
    }
}
