/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.style.parser.QuirksMode
import fernice.std.Option

class MatchingContext(
    val bloomFilter: Option<BloomFilter>,
    val quirksMode: QuirksMode
) {

    fun quirksMode(): QuirksMode {
        return quirksMode
    }
}
