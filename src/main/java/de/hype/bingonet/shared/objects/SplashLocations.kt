package de.hype.bingonet.shared.objects

object SplashLocations {
    val HUB_SELECTOR: SplashLocation = SplashLocation("Hub Selector Corner", -9, 70, -21)
    val MAP_BB = SplashLocation("Hub Map(BB)", 6, 69, -6)
    val KAT_BSC = SplashLocation("Kat (BSC)", 8, 72, -54)
    val COMMUNITY_CENTER_CN = SplashLocation("Painting in Community Center (CN)", 0, 79, 21)


    @JvmStatic
    fun values(): List<SplashLocation> {
        return listOf(
            HUB_SELECTOR,
            MAP_BB,
            KAT_BSC,
            COMMUNITY_CENTER_CN,
        )
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
