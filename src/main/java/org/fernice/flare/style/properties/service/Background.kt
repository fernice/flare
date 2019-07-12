/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.service

import org.fernice.flare.style.properties.PropertyContainer
import org.fernice.flare.style.properties.PropertyContainerContributor
import org.fernice.flare.style.properties.longhand.background.BackgroundAttachmentId
import org.fernice.flare.style.properties.longhand.background.BackgroundClipId
import org.fernice.flare.style.properties.longhand.background.BackgroundColorId
import org.fernice.flare.style.properties.longhand.background.BackgroundImageId
import org.fernice.flare.style.properties.longhand.background.BackgroundOriginId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionXId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionYId
import org.fernice.flare.style.properties.longhand.background.BackgroundSizeId
import org.fernice.flare.style.properties.shorthand.background.BackgroundId

class BackgroundPropertyContainerContributor : PropertyContainerContributor {

    override fun contribute(container: PropertyContainer) {
        container.registerLonghands(
            BackgroundColorId,

            BackgroundImageId,

            BackgroundPositionXId,
            BackgroundPositionYId,
            BackgroundSizeId,

            BackgroundAttachmentId,
            BackgroundClipId,
            BackgroundOriginId
        )

        container.registerShorthand(BackgroundId)
    }
}