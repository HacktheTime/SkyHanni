package de.hype.bingonet.shared.objects

object SplashLocations {
    val HUB_SELECTOR: SplashLocation = SplashLocation("Hub Selector Corner", -9, 70, -21)


    @JvmStatic
    fun values(): List<SplashLocation> {
        return listOf(HUB_SELECTOR)
    }

    @JvmStatic
    fun getFromExactCoords(position: Position): SplashLocation {
        for (value in values()) {
            if (value.coords == position) {
                return value
            }
        }
        return SplashLocation(position, null)
    }
}
