/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.stylesheet

class LayerOrder(val value: Int) : Comparable<LayerOrder> {

    override fun compareTo(other: LayerOrder): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerOrder) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String = "LayerOrder[$value]"

    companion object {
        val Root = LayerOrder(Int.MAX_VALUE - 1)
        val StyleAttribute = LayerOrder(Int.MAX_VALUE)
    }
}

class LayerName(
    val qualifiedName: List<String>,
)
