package de.hype.bingonet.shared.objects

class SplashLocation {
    @JvmField
    val coords: Position
    private val name: String?

    constructor(coords: Position, name: String?) {
        this.coords = coords
        this.name = name
    }

    constructor(name: String?, x: Int, y: Int, z: Int) {
        this.name = name
        coords = Position(x, y, z)
    }

    fun getName(): String {
        if (name == null) return coords.toString()
        return name
    }

    fun hasName(): Boolean {
        return !(name == null || name.isEmpty())
    }

    val displayString: String
        get() {
            return "$name (${coords.x} ${coords.y} ${coords.z})"
        }

    fun getCommandArgNames(): String {
        val name = getName()
        if (name.contains(" ")) return "\"$name\""
        return name
    }
}

