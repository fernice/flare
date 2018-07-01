package de.krall.flare.std

import java.security.AccessController
import java.security.PrivilegedAction

fun getBooleanProperty(key: String): Boolean {
    return getBooleanProperty(key, false)
}

fun getBooleanProperty(key: String, default: Boolean): Boolean {
    return AccessController.doPrivileged(PrivilegedAction<Boolean> {
        val value = System.getProperty(key)

        if (value != null) {
            value == "true"
        } else {
            default
        }
    })
}
