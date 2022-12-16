/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.source

import org.fernice.flare.style.ApplicableDeclarationBlock
import org.fernice.flare.style.Importance
import org.fernice.flare.style.Origin
import org.fernice.flare.style.properties.PropertyDeclarationBlock

sealed interface StyleSource {

    val origin: Origin

    val declarations: PropertyDeclarationBlock

    fun getApplicableDeclarations(importance: Importance): ApplicableDeclarationBlock
}
