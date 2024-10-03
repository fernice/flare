/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.context

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.style.StyleBloom
import org.fernice.flare.style.Stylist
import org.fernice.flare.style.stylesheet.RuleConditionCache
import org.fernice.flare.style.stylesheet.SimpleRuleConditionCache

class StyleContext(
    val device: Device,
    val stylist: Stylist,
    val fontMetricsProvider: FontMetricsProvider,
) {

    val styleRoots = StyleRootStack(stylist.styleRoot)
    val bloomFilter = StyleBloom()

    fun prepare(element: Element) {
        styleRoots.insert(element)
        bloomFilter.insertParent(element)
    }

    val ruleConditionCache: RuleConditionCache = SimpleRuleConditionCache()
}
