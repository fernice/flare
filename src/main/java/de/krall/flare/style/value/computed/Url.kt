package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.url.Url

sealed class ComputedUrl : ComputedValue {

    data class Valid(val url: Url) : ComputedUrl()

    data class Invalid(val text: String) : ComputedUrl()
}