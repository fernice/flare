package de.krall.flare.style

import de.krall.flare.selector.BloomFilter
import de.krall.flare.selector.CountingBloomFilter

class StyleBloom(private val bloomFilter: BloomFilter) {

    companion object {
        fun new(): StyleBloom {
            return StyleBloom(
                    CountingBloomFilter()
            )
        }
    }

    fun filter(): BloomFilter {
        return bloomFilter
    }
}