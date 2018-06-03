package de.krall.flare.style.stylesheet

import de.krall.flare.style.properties.PropertyId
import org.junit.Test

class StylesheetParseTest {

    @Test
    fun init(){
        PropertyId.ids
    }

    @Test
    fun block() {
        val stylesheet = Stylesheet.from("element > .p {\n background-attachment: fixed !important;\nbackground-color: #FFE }", Origin.AUTHOR)

        println(stylesheet)
    }
}