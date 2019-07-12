/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.service

import org.fernice.flare.style.properties.PropertyContainer
import org.fernice.flare.style.properties.PropertyContainerContributor
import org.fernice.flare.style.properties.longhand.font.FontFamilyId
import org.fernice.flare.style.properties.longhand.font.FontSizeId
import org.fernice.flare.style.properties.longhand.font.FontWeightId

class FontPropertyContainerContributor : PropertyContainerContributor {

    override fun contribute(container: PropertyContainer) {
        container.registerLonghands(
            FontSizeId,
            FontFamilyId,
            FontWeightId
        )
    }
}