/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import fernice.std.None
import fernice.std.Option
import fernice.std.Some

class SelectorParser : SelectorParserContext {

    override fun defaultNamespace(): Option<NamespaceUrl> {
        return None
    }

    override fun namespacePrefix(prefix: String): NamespacePrefix {
        return NamespacePrefix(prefix)
    }

    override fun namespaceForPrefix(prefix: NamespacePrefix): Option<NamespaceUrl> {
        return Some(NamespaceUrl(prefix, "unknown"))
    }

    override fun pseudoElementAllowsSingleColon(name: String): Boolean {
        return false
    }
}
