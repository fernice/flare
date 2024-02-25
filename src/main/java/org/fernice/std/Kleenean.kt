/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

enum class Kleenean {
    True,
    False,
    Unknown;

    fun toBoolean(unknown: Boolean): Boolean {
        if (this === Unknown) return unknown
        return this === True
    }

    operator fun not(): Kleenean = when (this) {
        True -> False
        False -> True
        Unknown -> Unknown
    }

    override fun toString(): String = name.lowercase()
}

fun Boolean.toKleenean() = when (this) {
    true -> Kleenean.True
    false -> Kleenean.False
}

fun Boolean?.toKleenean() = when (this) {
    true -> Kleenean.True
    false -> Kleenean.False
    null -> Kleenean.Unknown
}
