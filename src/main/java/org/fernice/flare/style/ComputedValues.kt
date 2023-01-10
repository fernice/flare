/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.style.properties.CustomPropertiesList
import org.fernice.flare.style.properties.PropertiesList
import org.fernice.flare.style.properties.stylestruct.Background
import org.fernice.flare.style.properties.stylestruct.Border
import org.fernice.flare.style.properties.stylestruct.Color
import org.fernice.flare.style.properties.stylestruct.Font
import org.fernice.flare.style.properties.stylestruct.Margin
import org.fernice.flare.style.properties.stylestruct.Padding
import org.fernice.flare.style.ruletree.RuleNode

data class ComputedValues(
    val font: Font,
    val color: Color,
    val background: Background,
    val border: Border,
    val margin: Margin,
    val padding: Padding,
    val customProperties: CustomPropertiesList?,
    val properties: PropertiesList?,
    internal val ruleNode: RuleNode?,
) {

    companion object {

        val Initial: ComputedValues by lazy {
            ComputedValues(
                font = Font.Initial,
                color = Color.Initial,
                background = Background.Initial,
                border = Border.initial,
                margin = Margin.initial,
                padding = Padding.initial,
                customProperties = null,
                properties = null,
                ruleNode = null,
            )
        }
    }
}
