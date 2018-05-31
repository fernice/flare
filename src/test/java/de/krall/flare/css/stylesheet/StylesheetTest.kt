package de.krall.flare.css.stylesheet

import org.junit.Test

class StylesheetParseTest {

    @Test
    fun block() {
        val stylesheet = Stylesheet.from("element > .p { background-attachment: fixed !important }", Origin.AUTHOR)

        println(stylesheet)
    }
}