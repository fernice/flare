/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.PaddingBottomId
import org.fernice.flare.style.properties.longhand.PaddingLeftId
import org.fernice.flare.style.properties.longhand.PaddingRightId
import org.fernice.flare.style.properties.longhand.PaddingTopId
import org.fernice.flare.style.properties.shorthand.PaddingId

object PaddingPropertyModule : PropertyModule {

    override val name: String = "padding"

    override val longhands: List<LonghandId> = listOf(
        PaddingTopId,
        PaddingRightId,
        PaddingBottomId,
        PaddingLeftId
    )

    override val shorthands: List<ShorthandId> = listOf(
        PaddingId
    )
}