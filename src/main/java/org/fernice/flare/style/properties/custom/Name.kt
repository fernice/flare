/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.custom

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result

data class Name(val value: String) : Comparable<Name> {

    override fun compareTo(other: Name): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String = "--$value"

    companion object {

        fun parse(value: String): Result<Name, Unit> {
            return if (value.startsWith("--")) {
                Ok(Name(value.substring(2)))
            } else {
                Err()
            }
        }
    }
}




