package de.hype.bingonet.projectsync

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import de.hype.bingonet.environment.packetconfig.BNGson
import kotlin.collections.forEach

/**
 * WARNING: This File is auto-generated. Do not edit it manually!
 * This is an adapter for automatic remapping serialization/deserialization of shared objects.
 *
 * Essentially. Some classes get remapped to some other class in the code and they have to be sent in the way that is the exact same everywhere.
 * This adapter handles that.
 */
object ProjectSyncAdapter {
    internal val internalGson = BNGson.createInternal()
    val adapters = mutableListOf<ProjectSyncTypeAdapter<*, *>>()

    init {
        //This List is auto-generated. Do not edit it manually!
        adapters += BNNEUItemAdapter()
    }

    fun applyAdapters(builder: GsonBuilder): GsonBuilder {
        adapters.forEach {
            builder.registerTypeAdapter(it.sharedClass, it)
            builder.registerTypeAdapter(it.envClass, it)
        }
        return builder
    }
}

fun GsonBuilder.registerProjectSyncAdapters(): GsonBuilder {
    return ProjectSyncAdapter.applyAdapters(this)
}

abstract class ProjectSyncTypeAdapter<Shared : Any?, Env : Any?> : JsonSerializer<Env>, JsonDeserializer<Env> {
    val sharedClass by lazy {
        this::class.java.typeParameters[0].bounds[0]
    }

    val envClass by lazy {
        this::class.java.typeParameters[1].bounds[0]
    }

    override fun serialize(
        src: Env?,
        typeOfSrc: java.lang.reflect.Type?,
        context: com.google.gson.JsonSerializationContext?,
    ): com.google.gson.JsonElement {
        if (src == null) return com.google.gson.JsonNull.INSTANCE
        return ProjectSyncAdapter.internalGson.toJsonTree(toShared(src))
    }

    override fun deserialize(
        json: com.google.gson.JsonElement?,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext,
    ): Env? {
        val shared = ProjectSyncAdapter.internalGson.fromJson<Shared>(json, sharedClass)
        return toEnv(shared)
    }

    abstract fun toShared(env: Env?): Shared?
    abstract fun toEnv(shared: Shared?): Env?
}
