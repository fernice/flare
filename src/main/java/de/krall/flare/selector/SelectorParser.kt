package de.krall.flare.selector

import modern.std.None
import modern.std.Option
import modern.std.Some

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