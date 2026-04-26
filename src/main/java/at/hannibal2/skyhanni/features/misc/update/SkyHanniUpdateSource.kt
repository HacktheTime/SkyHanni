package at.hannibal2.skyhanni.features.misc.update

import moe.nea.libautoupdate.UpdateSource

@Suppress("unused")
enum class SkyHanniUpdateSource(private val displayName: String, val source: UpdateSource) {
    MODRINTH("Modrinth", ModrinthUpdateSource("Cp13oI7e", "skyhanni")),
    GITHUB("GitHub", CustomGithubReleaseUpdateSource("HacktheTime", "SkyHanni")),
    ;

    override fun toString() = displayName
}
