/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties.shorthand.background

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.std.Second
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
    val backgroundClip: List<Clip>,
)

private fun parse(context: ParserContext, input: Parser): Result<Longhands, ParseError> {
    var backgroundColor: Color? = null
    val backgroundImage: MutableList<ImageLayer> = mutableListOf()
    val backgroundPositionX: MutableList<HorizontalPosition> = mutableListOf()
    val backgroundPositionY: MutableList<VerticalPosition> = mutableListOf()
    val backgroundRepeat: MutableList<BackgroundRepeat> = mutableListOf()
    val backgroundSize: MutableList<BackgroundSize> = mutableListOf()
    val backgroundAttachment: MutableList<Attachment> = mutableListOf()
    val backgroundOrigin: MutableList<Origin> = mutableListOf()
    val backgroundClip: MutableList<Clip> = mutableListOf()

    val result = input.parseCommaSeparated { parser ->
        if (backgroundColor != null) {
            return@parseCommaSeparated Err(input.newError(ParseErrorKind.Unspecified))
        }

        var image: ImageLayer? = null
        var position: Position? = null
        var repeat: BackgroundRepeat? = null
        var size: BackgroundSize? = null
        var attachment: Attachment? = null
        var origin: Origin? = null
        var clip: Clip? = null

        loop@
        while (true) {
            if (backgroundColor == null) {
                val result = parser.tryParse { nestedParser -> Color.parse(context, nestedParser) }

                if (result is Ok) {
                    backgroundColor = result.value
                    continue@loop
                }
            }
            if (position == null) {
                val result = parser.tryParse { nestedParser -> Position.parse(context, nestedParser) }

                if (result is Ok) {
                    position = result.value

                    size = parser.tryParse { nestedParser ->
                        when (val solidus = nestedParser.expectSolidus()) {
                            is Err -> return@tryParse Err(solidus.value)
                            else -> {}
                        }
                        BackgroundSize.parse(context, nestedParser)
                    }.ok()

                    continue@loop
                }
            }
            if (image == null) {
                val result = parser.tryParse { nestedParser -> Image.parse(context, nestedParser).map(::Second) }

                if (result is Ok) {
                    image = result.value
                    continue@loop
                }
            }
            if (repeat == null) {
                val result = parser.tryParse { nestedParser -> BackgroundRepeat.parse(context, nestedParser) }

                if (result is Ok) {
                    repeat = result.value
                    continue@loop
                }
            }
            if (size == null) {
                val result = parser.tryParse { nestedParser -> BackgroundSize.parse(context, nestedParser) }

                if (result is Ok) {
                    size = result.value
                    continue@loop
                }
            }
            if (attachment == null) {
                val result = parser.tryParse { nestedParser -> Attachment.parse(context, nestedParser) }

                if (result is Ok) {
                    attachment = result.value
                    continue@loop
                }
            }
            if (origin == null) {
                val result = parser.tryParse { nestedParser -> Origin.parse(nestedParser) }

                if (result is Ok) {
                    origin = result.value
                    continue@loop
                }
            }
            if (clip == null) {
                val result = parser.tryParse { nestedParser -> Clip.parse(nestedParser) }

                if (result is Ok) {
                    clip = result.value
                    continue@loop
                }
            }
            break
        }
        if (clip == null && origin != null) {
            clip = origin.toClip()
        }
        val any = image != null
                || position != null
                || repeat != null
                || size != null
                || attachment != null
                || origin != null
                || clip != null
                || backgroundColor != null

        if (any) {
            if (position != null) {
                backgroundPositionX.add(position.horizontal)
                backgroundPositionY.add(position.vertical)
            } else {
                backgroundPositionX.add(HorizontalPosition.zero())
                backgroundPositionY.add(VerticalPosition.zero())
            }

            if (image != null) {
                backgroundImage.add(image)
            } else {
                backgroundImage.add(BackgroundImageDeclaration.InitialSingleValue)
            }
            if (repeat != null) {
                backgroundRepeat.add(repeat)
            } else {
                backgroundRepeat.add(BackgroundRepeatDeclaration.InitialSingleValue)
            }
            if (size != null) {
                backgroundSize.add(size)
            } else {
                backgroundSize.add(BackgroundSize.auto())
            }
            if (attachment != null) {
                backgroundAttachment.add(attachment)
            } else {
                backgroundAttachment.add(BackgroundAttachmentDeclaration.InitialSingleValue)
            }
            if (origin != null) {
                backgroundOrigin.add(origin)
            } else {
                backgroundOrigin.add(BackgroundOriginDeclaration.InitialSingleValue)
            }
            if (clip != null) {
                backgroundClip.add(clip)
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
            backgroundColor = backgroundColor ?: Color.transparent(),
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
        input: Parser,
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