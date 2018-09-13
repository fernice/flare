/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.value.ComputedValue
import org.fernice.flare.url.Url

sealed class ComputedUrl : ComputedValue {

    data class Valid(val url: Url) : ComputedUrl()

    data class Invalid(val text: String) : ComputedUrl()
}
