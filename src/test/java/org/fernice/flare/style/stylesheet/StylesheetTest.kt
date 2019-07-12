/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.stylesheet

import org.junit.BeforeClass
import org.junit.Test

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
        val stylesheet = Stylesheet.from("element > .p {\n background-attachment: fixed !important; background-color: #FFE }", Origin.AUTHOR)

        println(stylesheet)
    }
}
