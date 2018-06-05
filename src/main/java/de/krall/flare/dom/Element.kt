package de.krall.flare.dom

import de.krall.flare.std.Option

interface Element {

    fun localName(): String

    fun id(): Option<String>

    fun classes(): List<String>
}