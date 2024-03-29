/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.std.Second
import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.background.Attachment
import org.fernice.flare.style.properties.longhand.background.BackgroundAttachmentDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundClipDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundColorDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundImageDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundOriginDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionXDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundPositionYDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundRepeatDeclaration
import org.fernice.flare.style.properties.longhand.background.BackgroundSizeDeclaration
import org.fernice.flare.style.properties.longhand.background.Clip
import org.fernice.flare.style.properties.longhand.background.Origin
import org.fernice.flare.style.value.computed.BackgroundRepeat
import org.fernice.flare.style.value.computed.BackgroundSize
import org.fernice.flare.style.value.computed.Color
import org.fernice.flare.style.value.computed.HorizontalPosition
import org.fernice.flare.style.value.computed.Image
import org.fernice.flare.style.value.computed.ImageLayer
import org.fernice.flare.style.value.computed.VerticalPosition

interface Background : StyleStruct<MutBackground> {

    val color: Color
    val image: List<ImageLayer>
    val attachment: List<Attachment>
    val positionX: List<HorizontalPosition>
    val positionY: List<VerticalPosition>
    val size: List<BackgroundSize>
    val repeat: List<BackgroundRepeat>
    val origin: List<Origin>
    val clip: Clip

    fun shapeHash(): Int {
        return clip.hashCode()
    }

    fun imageLayerIterator(): Iterator<BackgroundImageLayer> {
        return ImageLayerIterator.new(this)
    }

    fun reversedImageLayerIterator(): Iterator<BackgroundImageLayer> {
        return ImageLayerIterator.new(this)
    }

    companion object {

        val Initial: Background by lazy {
            StaticBackground(
                BackgroundColorDeclaration.InitialValue,
                BackgroundImageDeclaration.InitialValue,
                BackgroundAttachmentDeclaration.InitialValue,
                BackgroundPositionXDeclaration.InitialValue,
                BackgroundPositionYDeclaration.InitialValue,
                BackgroundSizeDeclaration.InitialValue,
                BackgroundRepeatDeclaration.InitialValue,
                BackgroundOriginDeclaration.InitialValue,
                BackgroundClipDeclaration.InitialValue
            )
        }
    }
}

private data class StaticBackground(
    override val color: Color,
    override val image: List<ImageLayer>,
    override val attachment: List<Attachment>,
    override val positionX: List<HorizontalPosition>,
    override val positionY: List<VerticalPosition>,
    override val size: List<BackgroundSize>,
    override val repeat: List<BackgroundRepeat>,
    override val origin: List<Origin>,
    override val clip: Clip
) : Background {

    override fun clone(): MutBackground {
        return MutBackground(
            color,
            image,
            attachment,
            positionX,
            positionY,
            size,
            repeat,
            origin,
            clip
        )
    }
}

data class MutBackground(
    override var color: Color,
    override var image: List<ImageLayer>,
    override var attachment: List<Attachment>,
    override var positionX: List<HorizontalPosition>,
    override var positionY: List<VerticalPosition>,
    override var size: List<BackgroundSize>,
    override var repeat: List<BackgroundRepeat>,
    override var origin: List<Origin>,
    override var clip: Clip
) : Background, MutStyleStruct {

    override fun clone(): MutBackground {
        return MutBackground(
            color,
            image,
            attachment,
            positionX,
            positionY,
            size,
            repeat,
            origin,
            clip
        )
    }
}

class BackgroundImageLayer(
    val image: Image,
    val attachment: Attachment,
    val positionX: HorizontalPosition,
    val positionY: VerticalPosition,
    val size: BackgroundSize,
    val repeat: BackgroundRepeat,
    val origin: Origin
)

class ImageLayerIterator(private val background: Background, private val indices: Iterator<Int>) : Iterator<BackgroundImageLayer> {

    companion object {

        fun new(background: Background): ImageLayerIterator {
            val indices = background.image
                .asSequence()
                .withIndex()
                .filter { (_, layer) -> layer is Second }
                .map { (index, _) -> index }
                .iterator()

            return ImageLayerIterator(background, indices)
        }
    }

    override fun hasNext(): Boolean = indices.hasNext()

    override fun next(): BackgroundImageLayer {
        val index = indices.next()

        val image = (background.image[index] as Second).value
        val attachment = background.attachment.drag(index)
        val positionX = background.positionX.drag(index)
        val positionY = background.positionY.drag(index)
        val size = background.size.drag(index)
        val repeat = background.repeat.drag(index)
        val origin = background.origin.drag(index)

        return BackgroundImageLayer(
            image,
            attachment,
            positionX,
            positionY,
            size,
            repeat,
            origin
        )
    }
}

private fun <E> List<E>.drag(index: Int): E {
    return if (index < this.size) this[index] else this[this.lastIndex]
}

