/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.selector.SelectorList

sealed class ContextualError {

    data class InvalidRule(val slice: String, val error: ParseError) : ContextualError()

    data class UnsupportedPropertyDeclaration(val slice: String, val error: ParseError, val selectors: List<SelectorList>) : ContextualError()
}

sealed class StyleParseErrorKind : ParseErrorKind() {
    data object UnspecifiedError : StyleParseErrorKind()
    data object UnexpectedImportRule : StyleParseErrorKind()
    data object UnexpectedNamespaceRule : StyleParseErrorKind()
    data object UnexpectedCharsetRule : StyleParseErrorKind()
    data object UnexpectedLayerRule : StyleParseErrorKind()
    data class UnknownProperty(val name: String) : StyleParseErrorKind()
    data class InvalidPropertyValue(val name: String, val error: ParseError) : StyleParseErrorKind()
}
