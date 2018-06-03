package de.krall.flare.selector

import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some

class SelectorParser : SelectorParserContext {

    override fun defaultNamespace(): Option<NamespaceUrl> {
        return None()
    }

    override fun namespacePrefix(prefix: String): NamespacePrefix {
        return NamespacePrefixImpl(prefix)
    }

    override fun namespaceForPrefix(prefix: NamespacePrefix): Option<NamespaceUrl> {
        return Some(NamespaceUrlImpl(prefix))
    }

    override fun pseudoElementAllowsSingleColon(name: String): Boolean {
        return false
    }

    class NamespacePrefixImpl(private val prefix: String) : NamespacePrefix {

        override fun getPrefix(): String {
            return prefix
        }
    }

    class NamespaceUrlImpl(private val prefix: NamespacePrefix) : NamespaceUrl {

        override fun getUrl(): String {
            return prefix.getPrefix()
        }
    }
}