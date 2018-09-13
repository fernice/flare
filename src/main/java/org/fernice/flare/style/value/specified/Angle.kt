/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import fernice.std.Empty
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.style.value.computed.Angle as ComputedAngle

data class Angle(
        val value: ComputedAngle,
        val wasCalc: Boolean
) : SpecifiedValue<ComputedAngle> {

    override fun toComputedValue(context: Context): ComputedAngle {
        return value
    }

    fun radians():Float {
        return value.radians()
    }

    fun degrees():Float {
        return value.degrees()
    }

    companion object {

        fun fromDegree(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                    ComputedAngle.Deg(angle),
                    wasCalc
            )
        }

        fun fromGradians(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                    ComputedAngle.Grad(angle),
                    wasCalc
            )
        }

        fun fromTurns(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                    ComputedAngle.Turn(angle),
                    wasCalc
            )
        }

        fun fromRadians(angle: Float, wasCalc: Boolean): Angle {
            return Angle(
                    ComputedAngle.Rad(angle),
                    wasCalc
            )
        }

        fun fromCalc(radians: Float): Angle {
            return Angle(
                    ComputedAngle.Rad(radians),
                    true
            )
        }

        private val zero: Angle by lazy { Angle.fromDegree(0f, false) }

        fun zero(): Angle {
            return zero
        }

        fun parseDimension(value: Float, unit: String, wasCalc: Boolean): Result<Angle, Empty> {
            val angle = when (unit.toLowerCase()) {
                "deg" -> fromDegree(value, wasCalc)
                "grad" -> fromGradians(value, wasCalc)
                "turn" -> fromTurns(value, wasCalc)
                "rad" -> fromRadians(value, wasCalc)
                else -> return Err()
            }
            return Ok(angle)
        }

        fun parseAllowingUnitless(context: ParserContext, input: Parser): Result<Angle, ParseError> {
            return Err(input.newError(ParseErrorKind.Unknown))
        }

        fun parseInternal(context: ParserContext, input: Parser, allowUnitless: Boolean): Result<Angle, ParseError> {
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
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
