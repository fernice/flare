/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.service

import org.fernice.flare.style.properties.PropertyContainer
import org.fernice.flare.style.properties.PropertyContainerContributor
import org.fernice.flare.style.properties.longhand.margin.MarginBottomId
import org.fernice.flare.style.properties.longhand.margin.MarginLeftId
import org.fernice.flare.style.properties.longhand.margin.MarginRightId
import org.fernice.flare.style.properties.longhand.margin.MarginTopId
import org.fernice.flare.style.properties.shorthand.margin.MarginId

class MarginPropertyContainerContributor : PropertyContainerContributor {

    override fun contribute(container: PropertyContainer) {
        container.registerLonghands(
            MarginTopId,
            MarginRightId,
            MarginBottomId,
            MarginLeftId
        )

        container.registerShorthands(
            MarginId
        )
    }
}