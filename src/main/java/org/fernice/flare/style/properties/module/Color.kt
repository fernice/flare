/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.ColorId

object ColorPropertyModule : PropertyModule {

    override val name: String = "color"

    override val longhands: List<LonghandId> = listOf(
        ColorId
    )

    override val shorthands: List<ShorthandId> = listOf()
}