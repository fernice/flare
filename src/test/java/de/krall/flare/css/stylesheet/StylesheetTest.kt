package de.krall.flare.css.stylesheet

import de.krall.flare.css.properties.PropertyId
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