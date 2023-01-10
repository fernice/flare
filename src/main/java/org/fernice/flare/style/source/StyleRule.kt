/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.source

import org.fernice.flare.cssparser.SourceLocation
import org.fernice.flare.cssparser.toCssString
import org.fernice.flare.selector.SelectorList
import org.fernice.flare.style.ApplicableDeclarationBlock
import org.fernice.flare.style.Importance
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.Origin
import java.net.URI

class StyleRule(
    val selectors: SelectorList,
    override val declarations: PropertyDeclarationBlock,
    override val origin: Origin,
    val location: SourceLocation,
    val source: URI?,
) : StyleSource {

    private val importantDeclarations = ApplicableDeclarationBlock(this, Importance.Important)
    private val normalDeclarations = ApplicableDeclarationBlock(this, Importance.Normal)

    override fun getApplicableDeclarations(importance: Importance): ApplicableDeclarationBlock {
        return when (importance) {
            Importance.Important -> importantDeclarations
            Importance.Normal -> normalDeclarations
        }
    }

    override fun toString(): String {
        return "StyleRule(${selectors.toCssString()}, ${declarations.count} declarations, location: $location source: $source)"
    }
}
