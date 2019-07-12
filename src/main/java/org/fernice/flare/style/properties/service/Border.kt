/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.service

import org.fernice.flare.style.properties.PropertyContainer
import org.fernice.flare.style.properties.PropertyContainerContributor
import org.fernice.flare.style.properties.longhand.border.BorderBottomColorId
import org.fernice.flare.style.properties.longhand.border.BorderBottomLeftRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderBottomRightRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderBottomStyleId
import org.fernice.flare.style.properties.longhand.border.BorderBottomWidthId
import org.fernice.flare.style.properties.longhand.border.BorderLeftColorId
import org.fernice.flare.style.properties.longhand.border.BorderLeftStyleId
import org.fernice.flare.style.properties.longhand.border.BorderLeftWidthId
import org.fernice.flare.style.properties.longhand.border.BorderRightColorId
import org.fernice.flare.style.properties.longhand.border.BorderRightStyleId
import org.fernice.flare.style.properties.longhand.border.BorderRightWidthId
import org.fernice.flare.style.properties.longhand.border.BorderTopColorId
import org.fernice.flare.style.properties.longhand.border.BorderTopLeftRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderTopRightRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderTopStyleId
import org.fernice.flare.style.properties.longhand.border.BorderTopWidthId
import org.fernice.flare.style.properties.shorthand.border.BorderBottomId
import org.fernice.flare.style.properties.shorthand.border.BorderColorId
import org.fernice.flare.style.properties.shorthand.border.BorderId
import org.fernice.flare.style.properties.shorthand.border.BorderLeftId
import org.fernice.flare.style.properties.shorthand.border.BorderRadiusId
import org.fernice.flare.style.properties.shorthand.border.BorderRightId
import org.fernice.flare.style.properties.shorthand.border.BorderStyleId
import org.fernice.flare.style.properties.shorthand.border.BorderTopId
import org.fernice.flare.style.properties.shorthand.border.BorderWidthId

class BorderPropertyContainerContributor : PropertyContainerContributor {

    override fun contribute(container: PropertyContainer) {
        container.registerLonghands(
            BorderTopWidthId,
            BorderTopColorId,
            BorderTopStyleId,

            BorderTopLeftRadiusId,
            BorderTopRightRadiusId,

            BorderRightWidthId,
            BorderRightColorId,
            BorderRightStyleId,

            BorderBottomWidthId,
            BorderBottomColorId,
            BorderBottomStyleId,

            BorderBottomRightRadiusId,
            BorderBottomLeftRadiusId,

            BorderLeftWidthId,
            BorderLeftColorId,
            BorderLeftStyleId
        )

        container.registerShorthands(
            BorderId,

            BorderWidthId,
            BorderColorId,
            BorderStyleId,
            BorderRadiusId,

            BorderTopId,
            BorderRightId,
            BorderBottomId,
            BorderLeftId
        )
    }
}