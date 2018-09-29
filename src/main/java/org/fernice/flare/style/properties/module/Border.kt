/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.Experimental
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.BorderBottomColorId
import org.fernice.flare.style.properties.longhand.BorderBottomLeftRadiusId
import org.fernice.flare.style.properties.longhand.BorderBottomRightRadiusId
import org.fernice.flare.style.properties.longhand.BorderBottomStyleId
import org.fernice.flare.style.properties.longhand.BorderBottomWidthId
import org.fernice.flare.style.properties.longhand.BorderLeftColorId
import org.fernice.flare.style.properties.longhand.BorderLeftStyleId
import org.fernice.flare.style.properties.longhand.BorderLeftWidthId
import org.fernice.flare.style.properties.longhand.BorderRightColorId
import org.fernice.flare.style.properties.longhand.BorderRightStyleId
import org.fernice.flare.style.properties.longhand.BorderRightWidthId
import org.fernice.flare.style.properties.longhand.BorderTopColorId
import org.fernice.flare.style.properties.longhand.BorderTopLeftRadiusId
import org.fernice.flare.style.properties.longhand.BorderTopRightRadiusId
import org.fernice.flare.style.properties.longhand.BorderTopStyleId
import org.fernice.flare.style.properties.longhand.BorderTopWidthId
import org.fernice.flare.style.properties.shorthand.BorderBottomId
import org.fernice.flare.style.properties.shorthand.BorderColorId
import org.fernice.flare.style.properties.shorthand.BorderId
import org.fernice.flare.style.properties.shorthand.BorderLeftId
import org.fernice.flare.style.properties.shorthand.BorderRadiusId
import org.fernice.flare.style.properties.shorthand.BorderRightId
import org.fernice.flare.style.properties.shorthand.BorderStyleId
import org.fernice.flare.style.properties.shorthand.BorderTopId
import org.fernice.flare.style.properties.shorthand.BorderWidthId

object BorderPropertyModule : PropertyModule {

    override val name: String = "border"

    override val longhands: List<LonghandId> = listOf(
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

    override val shorthands: List<ShorthandId> = listOf(
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

@Experimental
object BorderImagePropertyModule : PropertyModule {

    override val name: String = "border-image"

    override val longhands: List<LonghandId> = listOf()

    override val shorthands: List<ShorthandId> = listOf()
}