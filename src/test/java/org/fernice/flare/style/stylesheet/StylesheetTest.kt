/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.fernice.flare.style.Origin
import org.junit.BeforeClass
import org.junit.Test
import java.net.URI

class StylesheetParseTest {

    companion object {

        @JvmStatic
        @BeforeClass
        fun init() {
            //register(BackgroundPropertyModule, BackgroundImagePropertyModule)
        }
    }

    @Test
    fun block() {
        val stylesheet = Stylesheet.from(Origin.Author, "element > .p {\n background-attachment: fixed !important; background-color: #FFE }", URI("test"))

        println(stylesheet)
    }
}
