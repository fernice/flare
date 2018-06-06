package de.krall.flare.selector

import de.krall.flare.std.Option
import de.krall.flare.style.parser.QuirksMode

class MatchingContext(val bloomFilter: Option<BloomFilter>,
                      val quirksMode: QuirksMode) {

    fun quirksMode(): QuirksMode {
        return quirksMode
    }
}