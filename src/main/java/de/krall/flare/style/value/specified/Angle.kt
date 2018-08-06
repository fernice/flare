package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.Angle as ComputedAngle

class Angle(
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
            return Err(input.newError(ParseErrorKind.Unkown))
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
                            Ok(Angle.zero())
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