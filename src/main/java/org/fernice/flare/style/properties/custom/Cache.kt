/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.custom

import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import java.util.IdentityHashMap

class SubstitutionCache {
    private val cache: MutableMap<Pair<ShorthandId, LonghandId>, PropertyDeclaration> = mutableMapOf()

    fun find(key: Pair<ShorthandId, LonghandId>): PropertyDeclaration? {
        return cache[key]
    }

    fun put(key: Pair<ShorthandId, LonghandId>, declaration: PropertyDeclaration) {
        cache[key] = declaration
    }

    fun remove(key: Pair<ShorthandId, LonghandId>) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }
}
