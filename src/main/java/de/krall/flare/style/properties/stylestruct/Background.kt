package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.Attachment
import de.krall.flare.style.properties.longhand.BackgroundAttachmentDeclaration
import de.krall.flare.style.properties.longhand.BackgroundClipDeclaration
import de.krall.flare.style.properties.longhand.BackgroundColorDeclaration
import de.krall.flare.style.properties.longhand.BackgroundImageDeclaration
import de.krall.flare.style.properties.longhand.BackgroundOriginDeclaration
import de.krall.flare.style.properties.longhand.BackgroundPositionXDeclaration
import de.krall.flare.style.properties.longhand.BackgroundPositionYDeclaration
import de.krall.flare.style.properties.longhand.BackgroundRepeatDeclaration
import de.krall.flare.style.properties.longhand.BackgroundSizeDeclaration
import de.krall.flare.style.properties.longhand.Clip
import de.krall.flare.style.properties.longhand.Origin
import de.krall.flare.style.value.computed.BackgroundRepeat
import de.krall.flare.style.value.computed.BackgroundSize
import de.krall.flare.style.value.computed.Color
import de.krall.flare.style.value.computed.HorizontalPosition
import de.krall.flare.style.value.computed.Image
import de.krall.flare.style.value.computed.VerticalPosition

interface Background : StyleStruct<MutBackground> {

    val color: Color
    val image: List<Image>
    val attachment: List<Attachment>
    val positionX: List<HorizontalPosition>
    val positionY: List<VerticalPosition>
    val size: List<BackgroundSize>
    val repeat: List<BackgroundRepeat>
    val origin: List<Origin>
    val clip: Clip

    companion object {

        val initial: Background by lazy {
            StaticBackground(
                    BackgroundColorDeclaration.initialValue,
                    BackgroundImageDeclaration.initialValue,
                    BackgroundAttachmentDeclaration.initialValue,
                    BackgroundPositionXDeclaration.initialValue,
                    BackgroundPositionYDeclaration.initialValue,
                    BackgroundSizeDeclaration.initialValue,
                    BackgroundRepeatDeclaration.initialValue,
                    BackgroundOriginDeclaration.initialValue,
                    BackgroundClipDeclaration.initialValue
            )
        }
    }
}

private class StaticBackground(
        override val color: Color,
        override val image: List<Image>,
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

class MutBackground(
        override var color: Color,
        override var image: List<Image>,
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