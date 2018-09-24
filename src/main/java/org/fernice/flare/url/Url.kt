/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.url

import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ToCss
import java.io.Writer


data class Url(val value: String) : ToCss {

    fun join(suffix: String): Result<Url, ParseError> {
        return Ok(Url(value + suffix))
    }

    override fun toCss(writer: Writer) {
        writer.append(value)
    }
}

sealed class ParseError {

}
