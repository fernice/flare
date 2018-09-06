package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.style.parser.Parse
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import modern.std.Empty
import modern.std.Err
import modern.std.Ok
import modern.std.Option
import modern.std.Result
import modern.std.unwrapOr
import de.krall.flare.style.value.computed.BackgroundRepeat as ComputedBackgroundRepeat
import de.krall.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize

sealed class BackgroundSize : SpecifiedValue<ComputedBackgroundSize> {

    class Explicit(val width: NonNegativeLengthOrPercentageOrAuto, val height: NonNegativeLengthOrPercentageOrAuto) : BackgroundSize()

    object Cover : BackgroundSize()

    object Contain : BackgroundSize()

    override fun toComputedValue(context: Context): ComputedBackgroundSize {
        return when (this) {
            is BackgroundSize.Explicit -> ComputedBackgroundSize.Explicit(
                    width.toComputedValue(context),
                    height.toComputedValue(context)
            )
            is BackgroundSize.Cover -> ComputedBackgroundSize.Cover
            is BackgroundSize.Contain -> ComputedBackgroundSize.Contain
        }
    }

    companion object : Parse<BackgroundSize> {
        override fun parse(context: ParserContext, input: Parser): Result<BackgroundSize, ParseError> {
            val widthResult = input.tryParse { parser -> NonNegativeLengthOrPercentageOrAuto.parse(context, parser) }

            if (widthResult is Ok) {
                val width = widthResult.value
                val height = input.tryParse { parser -> NonNegativeLengthOrPercentageOrAuto.parse(context, parser) }
                        .unwrapOr(width)

                return Ok(BackgroundSize.Explicit(width, height))
            }

            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return Ok(when (ident.toLowerCase()) {
                "cover" -> BackgroundSize.Cover
                "contain" -> BackgroundSize.Contain
                else -> return Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            })
        }

        fun auto(): BackgroundSize {
            return BackgroundSize.Explicit(
                    NonNegativeLengthOrPercentageOrAuto.auto(),
                    NonNegativeLengthOrPercentageOrAuto.auto()
            )
        }
    }
}

sealed class BackgroundRepeat : SpecifiedValue<ComputedBackgroundRepeat> {

    object RepeatX : BackgroundRepeat()

    object RepeatY : BackgroundRepeat()

    class Keywords(val horizontal: BackgroundRepeatKeyword, val vertical: Option<BackgroundRepeatKeyword>) : BackgroundRepeat()

    override fun toComputedValue(context: Context): ComputedBackgroundRepeat {
        return when (this) {
            is BackgroundRepeat.RepeatX -> {
                ComputedBackgroundRepeat(
                        BackgroundRepeatKeyword.Repeat,
                        BackgroundRepeatKeyword.NoRepeat
                )
            }
            is BackgroundRepeat.RepeatY -> {
                ComputedBackgroundRepeat(
                        BackgroundRepeatKeyword.NoRepeat,
                        BackgroundRepeatKeyword.Repeat
                )
            }
            is BackgroundRepeat.Keywords -> {
                ComputedBackgroundRepeat(
                        horizontal,
                        vertical.unwrapOr(horizontal)
                )
            }
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<BackgroundRepeat, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            when (identifier.toLowerCase()) {
                "repeat-x" -> return Ok(BackgroundRepeat.RepeatX)
                "repeat-y" -> return Ok(BackgroundRepeat.RepeatY)
                else -> {
                }
            }

            val horizontalResult = BackgroundRepeatKeyword.fromIdent(identifier)
            val horizontal = when (horizontalResult) {
                is Ok -> horizontalResult.value
                is Err -> return Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }

            val vertical = input.tryParse(BackgroundRepeatKeyword.Companion::parse).ok()

            return Ok(BackgroundRepeat.Keywords(horizontal, vertical))
        }
    }
}

enum class BackgroundRepeatKeyword {

    Repeat,

    Space,

    Round,

    NoRepeat;

    companion object {
        fun fromIdent(keyword: String): Result<BackgroundRepeatKeyword, Empty> {
            return when (keyword.toLowerCase()) {
                "repeat" -> Ok(BackgroundRepeatKeyword.Repeat)
                "space" -> Ok(BackgroundRepeatKeyword.Space)
                "round" -> Ok(BackgroundRepeatKeyword.Round)
                "no-repeat" -> Ok(BackgroundRepeatKeyword.NoRepeat)
                else -> Err()
            }
        }

        fun parse(input: Parser): Result<BackgroundRepeatKeyword, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "repeat" -> Ok(BackgroundRepeatKeyword.Repeat)
                "space" -> Ok(BackgroundRepeatKeyword.Space)
                "round" -> Ok(BackgroundRepeatKeyword.Round)
                "no-repeat" -> Ok(BackgroundRepeatKeyword.NoRepeat)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}