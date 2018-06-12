package de.krall.flare.dom

import de.krall.flare.style.ComputedValues
import de.krall.flare.style.value.computed.Au
import de.krall.flare.style.value.generic.Size2D

interface Device {

    fun defaultComputedValues(): ComputedValues {
        return ComputedValues.initial
    }

    fun viewportSize(): Size2D<Au>

    fun rootFontSize(): Au

    fun setRootFontSize(size: Au)
}