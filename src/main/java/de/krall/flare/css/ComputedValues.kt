package de.krall.flare.css

import de.krall.flare.css.properties.stylestruct.Background

data class ComputedValues(private val background: Background) {

    fun getBackground(): Background {
        return background
    }
}