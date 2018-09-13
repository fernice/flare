/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.std

import java.security.AccessController
import java.security.PrivilegedAction

fun getBooleanProperty(key: String): Boolean {
    return getBooleanProperty(key, false)
}

fun getBooleanProperty(key: String, default: Boolean): Boolean {
    return AccessController.doPrivileged(PrivilegedAction<Boolean> {
        val value = System.getProperty(key)

        if (value != null) {
            value == "true"
        } else {
            default
        }
    })
}
