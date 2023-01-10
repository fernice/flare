/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

import java.util.concurrent.atomic.AtomicReference

class Recycler<T : Any>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
) {

    private val reference = AtomicReference<T>()

    fun acquire(): T {
        return reference.getAndSet(null) ?: factory()
    }

    fun release(value: T) {
        reset(value)
        reference.compareAndSet(null, value)
    }
}
