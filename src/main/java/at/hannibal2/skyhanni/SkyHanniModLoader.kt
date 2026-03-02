package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.SkyHanniMod.modules
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class SkyHanniModLoader : ModInitializer {

    override fun onInitialize() {
        val modsDir = File(FabricLoader.getInstance().gameDir.toFile(),"./mods")
        if (modsDir.listFiles().count {
                (it.name.contains("bingonet",true) || it.name.contains("skyhanni",true)) && it.name.endsWith(".jar",true)
        } >= 2) throw IllegalStateException("It seems like you have multiple versions of SkyHanni and/or BingoNet installed. Please " +
            "remove the duplicate SkyHanni version. (Bingo Net is an Fork of SkyHanni with additional Features but same mod id,...)")
        SkyHanniMod.preInit()
        SkyHanniMod.init()
        loadedClasses.clear()
    }

    companion object {
        private val loadedClasses = mutableSetOf<String>()

        fun loadModule(obj: Any) {
            if (!loadedClasses.add(obj.javaClass.name)) throw IllegalStateException("Module ${obj.javaClass.name} is already loaded")
            modules.add(obj)
        }
    }
}
