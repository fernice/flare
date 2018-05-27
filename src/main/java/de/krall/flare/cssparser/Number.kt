package de.krall.flare.cssparser

class Number(val type: String,
             val text: String,
             val value: Double,
             val negative: Boolean) {

    fun int(): Int {
        return value.toInt()
    }
}