/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.shorthand.background

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.unwrapOr
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.std.Second
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.background.Attachment
import org.fernice.flare.style.properties.longhand.background.BackgroundAttachmentDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundAttachmentId
import org.fernice.flare.style.properties.longhand.background.BackgroundClipDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundClipId
import org.fernice.flare.style.properties.longhand.background.BackgroundColorDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundColorId
import org.fernice.flare.style.properties.longhand.background.BackgroundImageDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundImageId
import org.fernice.flare.style.properties.longhand.background.BackgroundOriginDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundOriginId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionXDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionXId
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionYDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionYId
import org.fernice.flare.style.properties.longhand.background.BackgroundRepeatDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundRepeatId
import org.fernice.flare.style.properties.longhand.background.BackgroundSizeDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundSizeId
import org.fernice.flare.style.properties.longhand.background.Clip
import org.fernice.flare.style.properties.longhand.background.Origin
import org.fernice.flare.style.value.specified.BackgroundRepeat
import org.fernice.flare.style.value.specified.BackgroundSize
import org.fernice.flare.style.value.specified.Color
import org.fernice.flare.style.value.specified.HorizontalPosition
import org.fernice.flare.style.value.specified.Image
import org.fernice.flare.style.value.specified.ImageLayer
import org.fernice.flare.style.value.specified.Position
import org.fernice.flare.style.value.specified.VerticalPosition

private class Longhands(
    val backgroundColor: Color,
    val backgroundImage: List<ImageLayer>,
    val backgroundPositionX: List<HorizontalPosition>,
    val backgroundPositionY: List<VerticalPosition>,
    val backgroundRepeat: List<BackgroundRepeat>,
    val backgroundSize: List<BackgroundSize>,
    val backgroundAttachment: List<Attachment>,
    val backgroundOrigin: List<Origin>,
    val backgroundClip: List<Clip>
)

private fun parse(context: ParserContext, input: Parser): Result<Longhands, ParseError> {
    var backgroundColor: Option<Color> = None
    val backgroundImage: MutableList<ImageLayer> = mutableListOf()
    val backgroundPositionX: MutableList<HorizontalPosition> = mutableListOf()
    val backgroundPositionY: MutableList<VerticalPosition> = mutableListOf()
    val backgroundRepeat: MutableList<BackgroundRepeat> = mutableListOf()
    val backgroundSize: MutableList<BackgroundSize> = mutableListOf()
    val backgroundAttachment: MutableList<Attachment> = mutableListOf()
    val backgroundOrigin: MutableList<Origin> = mutableListOf()
    val backgroundClip: MutableList<Clip> = mutableListOf()

    val result = input.parseCommaSeparated { parser ->
        if (backgroundColor.isSome()) {
            return@parseCommaSeparated Err(input.newError(ParseErrorKind.Unspecified))
        }

        var image: Option<ImageLayer> = None
        var position: Option<Position> = None
        var repeat: Option<BackgroundRepeat> = None
        var size: Option<BackgroundSize> = None
        var attachment: Option<Attachment> = None
        var origin: Option<Origin> = None
        var clip: Option<Clip> = None

        loop@
        while (true) {
            if (backgroundColor.isNone()) {
                val result = parser.tryParse { nestedParser -> Color.parse(context, nestedParser) }

                if (result is Ok) {
                    backgroundColor = Some(result.value)
                    continue@loop
                }
            }
            if (position.isNone()) {
                val result = parser.tryParse { nestedParser -> Position.parse(context, nestedParser) }

                if (result is Ok) {
                    position = Some(result.value)

                    size = parser.tryParse { nestedParser ->
                        when (val solidus = nestedParser.expectSolidus()) {
                            is Err -> return@tryParse Err(solidus.value)
                        }
                        BackgroundSize.parse(context, nestedParser)
                    }.ok()

                    continue@loop
                }
            }
            if (image.isNone()) {
                val result = parser.tryParse { nestedParser -> Image.parse(context, nestedParser).map(::Second) }

                if (result is Ok) {
                    image = Some(result.value)
                    continue@loop
                }
            }
            if (repeat.isNone()) {
                val result = parser.tryParse { nestedParser -> BackgroundRepeat.parse(context, nestedParser) }

                if (result is Ok) {
                    repeat = Some(result.value)
                    continue@loop
                }
            }
            if (size.isNone()) {
                val result = parser.tryParse { nestedParser -> BackgroundSize.parse(context, nestedParser) }

                if (result is Ok) {
                    size = Some(result.value)
                    continue@loop
                }
            }
            if (attachment.isNone()) {
                val result = parser.tryParse { nestedParser -> Attachment.parse(context, nestedParser) }

                if (result is Ok) {
                    attachment = Some(result.value)
                    continue@loop
                }
            }
            if (origin.isNone()) {
                val result = parser.tryParse { nestedParser -> Origin.parse(nestedParser) }

                if (result is Ok) {
                    origin = Some(result.value)
                    continue@loop
                }
            }
            if (clip.isNone()) {
                val result = parser.tryParse { nestedParser -> Clip.parse(nestedParser) }

                if (result is Ok) {
                    clip = Some(result.value)
                    continue@loop
                }
            }
            break
        }
        if (clip.isNone() && origin is Some) {
            clip = Some(origin.value.toClip())
        }
        val any = image.isSome()
                || position.isSome()
                || repeat.isSome()
                || size.isSome()
                || attachment.isSome()
                || origin.isSome()
                || clip.isSome()
                || backgroundColor.isSome()

        if (any) {
            if (position is Some) {
                backgroundPositionX.add(position.value.horizontal)
                backgroundPositionY.add(position.value.vertical)
            } else {
                backgroundPositionX.add(HorizontalPosition.zero())
                backgroundPositionY.add(VerticalPosition.zero())
            }

            if (image is Some) {
                backgroundImage.add(image.value)
            } else {
                backgroundImage.add(BackgroundImageDeclaration.InitialSingleValue)
            }
            if (repeat is Some) {
                backgroundRepeat.add(repeat.value)
            } else {
                backgroundRepeat.add(BackgroundRepeatDeclaration.InitialSingleValue)
            }
            if (size is Some) {
                backgroundSize.add(size.value)
            } else {
                backgroundSize.add(BackgroundSize.auto())
            }
            if (attachment is Some) {
                backgroundAttachment.add(attachment.value)
            } else {
                backgroundAttachment.add(BackgroundAttachmentDeclaration.InitialSingleValue)
            }
            if (origin is Some) {
                backgroundOrigin.add(origin.value)
            } else {
                backgroundOrigin.add(BackgroundOriginDeclaration.InitialSingleValue)
            }
            if (clip is Some) {
                backgroundClip.add(clip.value)
            } else {
                backgroundClip.add(BackgroundClipDeclaration.InitialSingleValue)
            }

            Ok()
        } else {
            Err(input.newError(ParseErrorKind.Unspecified))
        }
    }

    if (result is Err) {
        return result
    }

    return Ok(
        Longhands(
            backgroundColor = backgroundColor.unwrapOr(Color.transparent()),
            backgroundImage = backgroundImage,
            backgroundPositionX = backgroundPositionX,
            backgroundPositionY = backgroundPositionY,
            backgroundRepeat = backgroundRepeat,
            backgroundSize = backgroundSize,
            backgroundAttachment = backgroundAttachment,
            backgroundOrigin = backgroundOrigin,
            backgroundClip = backgroundClip
        )
    )
}

private fun Origin.toClip(): Clip {
    return when (this) {
        is Origin.BorderBox -> Clip.BorderBox
        is Origin.ContentBox -> Clip.ContentBox
        is Origin.PaddingBox -> Clip.PaddingBox
    }
}

object BackgroundId : ShorthandId() {

    override val name: String = "background"

    override fun parseInto(
        declarations: MutableList<PropertyDeclaration>,
        context: ParserContext,
        input: Parser
    ): Result<Unit, ParseError> {
        val longhands = when (val result = input.parseEntirely { parser -> parse(context, parser) }) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BackgroundColorDeclaration(longhands.backgroundColor))
        declarations.add(BackgroundImageDeclaration(longhands.backgroundImage))
        declarations.add(BackgroundPositionXDeclaration(longhands.backgroundPositionX))
        declarations.add(BackgroundPositionYDeclaration(longhands.backgroundPositionY))
        declarations.add(BackgroundRepeatDeclaration(longhands.backgroundRepeat))
        declarations.add(BackgroundSizeDeclaration(longhands.backgroundSize))
        declarations.add(BackgroundAttachmentDeclaration(longhands.backgroundAttachment))
        declarations.add(BackgroundOriginDeclaration(longhands.backgroundOrigin))
        declarations.add(BackgroundClipDeclaration(longhands.backgroundClip.first()))

        return Ok()
    }

    override val longhands: List<LonghandId> = listOf(
        BackgroundColorId,
        BackgroundImageId,
        BackgroundPositionXId,
        BackgroundPositionYId,
        BackgroundRepeatId,
        BackgroundSizeId,
        BackgroundAttachmentId,
        BackgroundOriginId,
        BackgroundClipId
    )
}