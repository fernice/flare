/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.module

import org.fernice.flare.Experimental
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.font.FontFamilyId
import org.fernice.flare.style.properties.longhand.font.FontSizeId
import org.fernice.flare.style.properties.longhand.font.FontWeightId

object FontPropertyModule : PropertyModule {

    override val name: String = "font"

    override val longhands: List<LonghandId> = listOf(
        FontSizeId,
        FontFamilyId,
        FontWeightId
    )

    override val shorthands: List<ShorthandId> = listOf()
}

@Experimental
object FontExtensionPropertyModule : PropertyModule {

    override val name: String = "font-extension"

    override val longhands: List<LonghandId> = listOf()

    override val shorthands: List<ShorthandId> = listOf()
}