/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.MarginBottomId
import org.fernice.flare.style.properties.longhand.MarginLeftId
import org.fernice.flare.style.properties.longhand.MarginRightId
import org.fernice.flare.style.properties.longhand.MarginTopId
import org.fernice.flare.style.properties.shorthand.MarginId

object MarginPropertyModule : PropertyModule {

    override val name: String = "margin"

    override val longhands: List<LonghandId> = listOf(
        MarginTopId,
        MarginRightId,
        MarginBottomId,
        MarginLeftId
    )

    override val shorthands: List<ShorthandId> = listOf(
        MarginId
    )
}