/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.std

sealed class Either<out A, out B>

data class First<A>(val value: A) : Either<A, Nothing>()

data class Second<B>(val value: B) : Either<Nothing, B>()
