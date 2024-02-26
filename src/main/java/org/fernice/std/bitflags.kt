/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std


interface Bitflags

abstract class U8Bitflags(
    protected var value: UByte,
) : Bitflags {

    val bits: UByte
        get() = value

    protected abstract val all: UByte

    fun isEmpty(): Boolean = value == 0u.toUByte()
    fun isAll(): Boolean = value == all

    fun intersects(value: UByte): Boolean = (this.value and value) != 0.toUByte()
    fun contains(value: UByte): Boolean = (this.value and value) == value

    fun add(value: UByte) {
        this.value = this.value or value
    }

    fun remove(value: UByte) {
        this.value = this.value and value.inv()
    }
}

