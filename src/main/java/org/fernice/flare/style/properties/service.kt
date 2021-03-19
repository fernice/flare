/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties

import org.fernice.logging.FLogging


interface PropertyContainerContributor {

    fun contribute(container: PropertyContainer)
}

class PropertyContainer {

    private val registeredProperties: MutableMap<String, PropertyId> = mutableMapOf()

    fun registerLonghands(vararg longhand: LonghandId) {
        longhand.forEach { registerLonghand(it) }
    }

    fun registerLonghand(longhand: LonghandId) {
        val property = PropertyId.Longhand(longhand)

        if (registeredProperties.containsKey(longhand.name)) {
            throw IllegalStateException("Property '${longhand.name}' has already been registered")
        }

        LOG.debug { "${longhand.name.padEnd(30)} Longhand ${longhand::class.qualifiedName}" }

        registeredProperties[longhand.name] = property
    }

    fun registerShorthands(vararg shorthand: ShorthandId) {
        shorthand.forEach { registerShorthand(it) }
    }

    fun registerShorthand(shorthand: ShorthandId) {
        val property = PropertyId.Shorthand(shorthand)

        if (registeredProperties.containsKey(shorthand.name)) {
            throw IllegalStateException("Property '${shorthand.name}' has already been registered")
        }

        LOG.debug { "${shorthand.name.padEnd(30)} Shorthand ${shorthand::class.qualifiedName}" }

        registeredProperties[shorthand.name] = property
    }

    fun getRegisteredProperties(): MutableMap<String, PropertyId> {
        return registeredProperties
    }

    companion object {
        private val LOG = FLogging.logger { }
    }
}