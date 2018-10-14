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

/**
 * Marks parsed objects that can be turned back into CSS syntax.
 */
interface ToCss {

    /**
     * Writes a representation of the object in a valid CSS syntax as true to original parsed text as
     * possible into the [writer].
     */
    fun toCss(writer: Writer)

    /**
     * Returns a representation of the object in a valid CSS syntax as true to original parsed text as
     * possible.
     *
     * This is a convenience method and should not be overridden. See [toCss] for the actual
     * implementation.
     */
    fun toCssString(): String {
        val writer = StringWriter()
        toCss(writer)
        return writer.toString()
    }
}

/**
 * Assistance method to write a list of [ToCss] marked objects into a [writer] by invoking the
 * [ToCss.toCss] method for each element while separating the output using the specified [separator].
 */
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

/**
 *  Assistance method to write an optional containing a [ToCss] marked object into a [writer] by
 *  invoking its [ToCss.toCss] method.
 */
fun <T : ToCss> Option<T>.toCss(writer: Writer) {
    if (this is Some) {
        value.toCss(writer)
    }
}