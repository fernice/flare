package de.krall.flare.selector

import de.krall.flare.style.parser.QuirksMode
import modern.std.Option

class MatchingContext(val bloomFilter: Option<BloomFilter>,
                      val quirksMode: QuirksMode) {

    fun quirksMode(): QuirksMode {
        return quirksMode
    }
}