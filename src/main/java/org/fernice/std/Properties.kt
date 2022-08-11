/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.std

import java.security.AccessController
import java.security.PrivilegedAction


fun systemFlag(key: String, default: Boolean = false): Boolean {
    return AccessController.doPrivileged(PrivilegedAction<Boolean> {
        val value = System.getProperty(key)

        if (value != null) {
            value == "true"
        } else {
            default
        }
    })
}
