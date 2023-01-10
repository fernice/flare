/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.source.StyleSource

data class ApplicableDeclarationBlock(
    val source: StyleSource,
    val importance: Importance,
) {

    val origin: Origin
        get() = source.origin

    val cascadeLevel: CascadeLevel
        get() = CascadeLevel.of(origin, importance)

    fun asSequence(reversed: Boolean = false): Sequence<PropertyDeclaration> {
        return source.declarations.asSequence(importance, reversed)
    }
}
