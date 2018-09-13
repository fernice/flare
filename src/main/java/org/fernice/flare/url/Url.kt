/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.url

import fernice.std.Ok
import fernice.std.Result


data class Url(val value: String) {

    fun join(suffix: String): Result<Url, ParseError> {
        return Ok(Url(value + suffix))
    }
}

sealed class ParseError {

}
