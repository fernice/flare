package de.krall.flare.selector

import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some

class SelectorParser : SelectorParserContext {

    override fun defaultNamespace(): Option<NamespaceUrl> {
        return None()
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