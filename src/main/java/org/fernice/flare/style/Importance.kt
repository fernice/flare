/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.std.Ordinal
import org.fernice.std.OrdinalUniverse
import org.fernice.std.PerOrdinal

enum class Importance : Ordinal {

    Normal,

    Important;

    companion object {
        val values = OrdinalUniverse(*values())
    }
}

typealias PerImportance<E> = PerOrdinal<Importance, E>

fun <E : Any> PerImportance(): PerImportance<E> = PerOrdinal(Importance.values)
fun <E : Any> PerImportance(initializer: (Importance) -> E): PerImportance<E> = PerOrdinal(Importance.values, initializer)
