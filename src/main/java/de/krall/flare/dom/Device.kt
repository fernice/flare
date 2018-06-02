package de.krall.flare.dom

import de.krall.flare.css.ComputedValues
import de.krall.flare.css.value.computed.Au
import de.krall.flare.css.value.generic.Size2D

interface Device {

    fun defaultComputedValues(): ComputedValues {
        return ComputedValues.initial
    }

    fun viewportSize(): Size2D<Au>

    fun rootFontSize(): Au
}