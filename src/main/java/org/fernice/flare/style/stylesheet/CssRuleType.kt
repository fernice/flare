/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.stylesheet

import java.util.EnumSet

enum class CssRuleType {
    Style,
}

typealias CssRuleTypes = EnumSet<CssRuleType>

fun CssRuleTypes(): CssRuleTypes = EnumSet.noneOf(CssRuleType::class.java)

