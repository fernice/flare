/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

class SelectorParser : SelectorParserContext {
    override fun defaultNamespace(): NamespaceUrl? = null
    override fun namespacePrefix(prefix: String): NamespacePrefix = NamespacePrefix(prefix)
    override fun namespaceForPrefix(prefix: NamespacePrefix): NamespaceUrl = NamespaceUrl(prefix, "unknown")
}
