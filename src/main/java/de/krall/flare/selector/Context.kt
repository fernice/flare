package de.krall.flare.selector

import de.krall.flare.std.Option
import de.krall.flare.style.parser.QuirksMode

class MatchingContext(val quirksMode: QuirksMode,
                      val bloomFilter: Option<BloomFilter>) {

    fun quirksMode(): QuirksMode {
        return quirksMode
    }
}