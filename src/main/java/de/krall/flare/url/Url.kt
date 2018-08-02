package de.krall.flare.url

import de.krall.flare.std.Ok
import de.krall.flare.std.Result

class Url(val value: String) {

    fun join(suffix: String): Result<Url, ParseError> {
        return Ok(Url(value + suffix))
    }
}

sealed class ParseError {

}