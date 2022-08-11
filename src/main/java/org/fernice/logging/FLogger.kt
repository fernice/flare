/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.logging

import org.slf4j.Logger

class FLogger(private val logger: Logger) : Logger by logger {

    fun trace(message: () -> Any?) {
        if (logger.isTraceEnabled) logger.trace(message().toString())
    }

    fun debug(message: () -> Any?) {
        if (logger.isDebugEnabled) logger.trace(message().toString())
    }

    fun info(message: () -> Any?) {
        if (logger.isInfoEnabled) logger.trace(message().toString())
    }

    fun warn(message: () -> Any?) {
        if (logger.isWarnEnabled) logger.trace(message().toString())
    }

    fun error(message: () -> Any?) {
        if (logger.isErrorEnabled) logger.trace(message().toString())
    }

    fun trace(throwable: Throwable?, message: () -> Any?) {
        if (logger.isTraceEnabled) logger.trace(message().toString(), throwable)
    }

    fun debug(throwable: Throwable?, message: () -> Any?) {
        if (logger.isDebugEnabled) logger.trace(message().toString(), throwable)
    }

    fun info(throwable: Throwable?, message: () -> Any?) {
        if (logger.isInfoEnabled) logger.trace(message().toString(), throwable)
    }

    fun warn(throwable: Throwable?, message: () -> Any?) {
        if (logger.isWarnEnabled) logger.trace(message().toString(), throwable)
    }

    fun error(throwable: Throwable?, message: () -> Any?) {
        if (logger.isErrorEnabled) logger.trace(message().toString(), throwable)
    }
}