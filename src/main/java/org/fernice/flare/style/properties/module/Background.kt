/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.Experimental
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.background.BackgroundAttachmentId
import org.fernice.flare.style.properties.longhand.background.BackgroundClipId
import org.fernice.flare.style.properties.longhand.background.BackgroundColorId
import org.fernice.flare.style.properties.longhand.background.BackgroundImageId
import org.fernice.flare.style.properties.longhand.background.BackgroundOriginId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionXId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionYId
import org.fernice.flare.style.properties.longhand.background.BackgroundSizeId
import org.fernice.flare.style.properties.shorthand.background.BackgroundId

object BackgroundPropertyModule : PropertyModule {

    override val name: String = "background"

    override val longhands: List<LonghandId> = listOf(
        BackgroundColorId
    )

    override val shorthands: List<ShorthandId> = listOf()
}

@Experimental
object BackgroundImagePropertyModule : PropertyModule {

    override val name: String = "background-image"

    override val longhands: List<LonghandId> = listOf(
        BackgroundImageId,

        BackgroundPositionXId,
        BackgroundPositionYId,
        BackgroundSizeId,

        BackgroundAttachmentId,
        BackgroundClipId,
        BackgroundOriginId
    )

    override val shorthands: List<ShorthandId> = listOf(
        BackgroundId
    )
}