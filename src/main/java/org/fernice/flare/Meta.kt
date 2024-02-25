/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare

import java.lang.Error

internal annotation class Experimental

fun panic(message: String): Nothing {
    throw Panic(message)
}

class Panic(message: String) : Error(message)
