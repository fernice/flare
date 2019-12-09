/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.style.properties.stylestruct.Background
import org.fernice.flare.style.properties.stylestruct.Border
import org.fernice.flare.style.properties.stylestruct.Color
import org.fernice.flare.style.properties.stylestruct.Font
import org.fernice.flare.style.properties.stylestruct.Margin
import org.fernice.flare.style.properties.stylestruct.Padding

data class ComputedValues(
    val font: Font,
    val color: Color,
    val background: Background,
    val border: Border,
    val margin: Margin,
    val padding: Padding
) {

    companion object {

        val initial: ComputedValues by lazy {
            ComputedValues(
                Font.initial,
                Color.Initial,
                Background.Initial,
                Border.initial,
                Margin.initial,
                Padding.initial
            )
        }
    }
}
