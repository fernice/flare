package de.krall.flare.dom

import de.krall.flare.css.ComputedValues

interface Device {

    fun defaultComputedValues(): ComputedValues
}