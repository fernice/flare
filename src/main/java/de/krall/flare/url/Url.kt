package de.krall.flare.url

import modern.std.Ok
import modern.std.Result


data class Url(val value: String) {

    fun join(suffix: String): Result<Url, ParseError> {
        return Ok(Url(value + suffix))
    }
}

sealed class ParseError {

}