/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.source

import org.fernice.flare.cssparser.SourceLocation
import org.fernice.flare.cssparser.toCssString
import org.fernice.flare.selector.SelectorList
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.stylesheet.CssRule

class StyleRule(
    val selectors: SelectorList,
    override val declarations: PropertyDeclarationBlock,
    val rules: List<CssRule>?,
    val location: SourceLocation,
) : StyleSource {

    override fun toString(): String {
        return "StyleRule(${selectors.toCssString()}, ${declarations.size} declarations, location: $location)"
    }
}
