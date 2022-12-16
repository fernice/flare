/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.std.mapErr
import kotlin.math.PI
import org.fernice.flare.style.value.computed.Angle as ComputedAngle

sealed class AngleDimension {

    data class Deg(val value: Float) : AngleDimension()

    data class Grad(val value: Float) : AngleDimension()

    data class Rad(val value: Float) : AngleDimension()

    data class Turn(val value: Float) : AngleDimension()
}

private const val DEG_PER_RAD = 180.0f / PI.toFloat()
private const val DEG_PER_GRAD = 180.0f / 200.0f
private const val DEG_PER_TURN = 360.0f

fun AngleDimension.degrees(): Float {
    return when (this) {
        is AngleDimension.Deg -> this.value
        is AngleDimension.Rad -> this.value * DEG_PER_RAD
        is AngleDimension.Grad -> this.value * DEG_PER_GRAD
        is AngleDimension.Turn -> this.value * DEG_PER_TURN
    }
}

data class Angle(
    val value: AngleDimension,
    val wasCalc: Boolean
) : SpecifiedValue<ComputedAngle> {

    override fun toComputedValue(context: Context): ComputedAngle {
        return ComputedAngle(value.degrees())
    }

    fun degrees(): Float {
        return value.degrees()
    }

    companion object {

        fun fromDegree(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                AngleDimension.Deg(angle),
                wasCalc
            )
        }

        fun fromGradians(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                AngleDimension.Grad(angle),
                wasCalc
            )
        }

        fun fromTurns(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                AngleDimension.Turn(angle),
                wasCalc
            )
        }

        fun fromRadians(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                AngleDimension.Rad(angle),
                wasCalc
            )
        }

        fun fromCalc(degrees: Float): Angle {
            return Angle(
                AngleDimension.Deg(degrees),
                true
            )
        }

        private val zero: Angle by lazy { Angle.fromDegree(0f, false) }

        fun zero(): Angle {
            return zero
        }

        fun parseDimension(value: Float, unit: String, wasCalc: Boolean): Result<Angle, Unit> {
            val angle = when (unit.lowercase()) {
                "deg" -> fromDegree(value, wasCalc)
                "grad" -> fromGradians(value, wasCalc)
                "turn" -> fromTurns(value, wasCalc)
                "rad" -> fromRadians(value, wasCalc)
                else -> return Err()
            }
            return Ok(angle)
        }

        fun parseAllowingUnitless(context: ParserContext, input: Parser): Result<Angle, ParseError> {
            return parseInternal(context, input, true)
        }

        fun parseInternal(
            context: ParserContext,
            input: Parser,
            allowUnitless: Boolean
        ): Result<Angle, ParseError> {
            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Dimension -> parseDimension(token.number.float(), token.unit, false)
                is Token.Number -> {
                    if (token.number.float() == 0f) {
                        if (allowUnitless) {
                            Ok(zero())
                        } else {
                            Err()
                        }
                    } else {
                        Err()
                    }
                }
                is Token.Function -> {
                    if (token.name.equals("calc", true)) {
                        input.parseNestedBlock { parser -> CalcNode.parseAngle(context, parser) }
                    } else {
                        Err()
                    }
                }
                else -> Err()
            }.mapErr { input.newUnexpectedTokenError(token) }
        }
    }
}
