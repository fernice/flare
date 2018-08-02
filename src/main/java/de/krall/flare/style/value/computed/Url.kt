package de.krall.flare.style.value.computed

sealed class ComputedUrl {

    class Valid(text: String) : ComputedUrl()

    class Invalid()
}