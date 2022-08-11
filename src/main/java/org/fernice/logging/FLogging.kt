/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.logging

import org.slf4j.LoggerFactory

object FLogging {
    fun logger(block: () -> Unit): FLogger {
        val name = block.javaClass.name
        val slicedName = when {
            name.contains("Kt$") -> name.substringBefore("Kt$")
            name.contains("$") -> name.substringBefore("$")
            else -> name
        }
        return FLogger(LoggerFactory.getLogger(slicedName))
    }
}