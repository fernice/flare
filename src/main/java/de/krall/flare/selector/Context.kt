package de.krall.flare.selector

import de.krall.flare.style.parser.QuirksMode

class MatchingContext(val quirksMode: QuirksMode) {

    fun quirksMode(): QuirksMode {
        return quirksMode
    }
}