package de.krall.flare.style.properties

import kotlin.reflect.KClass

@Repeatable
annotation class Longhand(
        val name: String,
        val value: KClass<*>,
        val initialValue: String,
        val initialValueSpecified: String,
        val allowQuirks: Boolean = false,
        val vector: Boolean = false)