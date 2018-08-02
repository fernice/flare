package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.url.Url

sealed class ComputedUrl : ComputedValue {

    class Valid(url: Url) : ComputedUrl()

    class Invalid(text: String) : ComputedUrl()
}