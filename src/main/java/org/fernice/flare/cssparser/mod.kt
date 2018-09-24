/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import fernice.std.Option
import fernice.std.Some
import java.io.StringWriter
import java.io.Writer

interface ToCss {

    fun toCss(writer: Writer)

    fun toCssString(): String {
        val writer = StringWriter()
        toCss(writer)
        return writer.toString()
    }
}

fun <T : ToCss> Iterable<T>.toCssJoining(writer: Writer, separator: String = "") {
    var later = false
    for (element in this) {
        if (later) {
            writer.append(separator)
        }
        later = true
        element.toCss(writer)
    }
}

fun <T : ToCss> Option<T>.toCss(writer: Writer) {
    if (this is Some) {
        value.toCss(writer)
    }
}