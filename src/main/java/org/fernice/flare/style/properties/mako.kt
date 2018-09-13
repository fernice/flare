/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import kotlin.reflect.KClass

@Repeatable
annotation class Longhand(
        val name: String,
        val value: KClass<*>,
        val initialValue: String,
        val initialValueSpecified: String,
        val allowQuirks: Boolean = false,
        val vector: Boolean = false)
