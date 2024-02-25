/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.std.PerEnumLazy

enum class Importance {

    Normal,

    Important;

}

typealias PerImportance<E> = PerEnumLazy<Importance, E>

fun <E : Any> PerImportance(initializer: (Importance) -> E): PerImportance<E> = PerEnumLazy(initializer)
