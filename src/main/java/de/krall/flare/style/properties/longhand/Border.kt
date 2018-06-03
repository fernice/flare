package de.krall.flare.style.properties.longhand

import de.krall.flare.style.properties.Longhand
import sun.security.util.Length

@Longhand(
        "border-top",
        Length::class,
        initialValue = "computed.Length.zero()",
        initialValueSpecified = "specified.Length.zero()"
        )
class Border