package de.hype.bingonet.shared.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.awt.Color
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class InstantSerializer : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val epochSecond = jsonObject.get("epochSecond").asLong
            val nanoAdjustment = jsonObject.get("nanoAdjustment").asInt
            return Instant.ofEpochSecond(epochSecond, nanoAdjustment.toLong())
        } else {
            val epochMilli = json.asLong
            return Instant.ofEpochMilli(epochMilli)
        }
    }

    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("epochSecond", src.epochSecond)
        jsonObject.addProperty("nanoAdjustment", src.nano)
        jsonObject.addProperty("toString", src.atZone(ZoneId.of("UTC")).toString())
        return jsonObject
    }
}

class ColorSerializer : JsonSerializer<Color>, JsonDeserializer<Color> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Color {
        val jsonObject = json.asJsonObject
        val red = jsonObject.getAsJsonPrimitive("r").asInt
        val green = jsonObject.getAsJsonPrimitive("g").asInt
        val blue = jsonObject.getAsJsonPrimitive("b").asInt
        val alpha = jsonObject.getAsJsonPrimitive("a").asInt

        return Color(red, green, blue, alpha)
    }

    override fun serialize(src: Color, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("r", src.red)
        jsonObject.addProperty("g", src.green)
        jsonObject.addProperty("b", src.blue)
        jsonObject.addProperty("a", src.alpha)

        return jsonObject
    }
}

class DurationSerializer : JsonSerializer<Duration>, JsonDeserializer<Duration> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Duration {
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val seconds = jsonObject.get("seconds").asLong
            val nanoAdjustment = jsonObject.get("nanoAdjustment").asInt
            return Duration.ofSeconds(seconds, nanoAdjustment.toLong())
        } else {
            val millis = json.asLong
            return Duration.ofMillis(millis)
        }
    }

    override fun serialize(src: Duration, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("seconds", src.seconds)
        jsonObject.addProperty("nanoAdjustment", src.nano)
        jsonObject.addProperty("toString", formatTime(src))
        return jsonObject
    }

    fun formatTime(src: Duration): String {
        val seconds = src.seconds
        if (seconds == 0L) return "now"
        val prefix = if (seconds > 0) "in %s" else "%s ago"
        val days = (seconds / 86400).toInt()
        val hours = ((seconds % 86400) / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        val sb = StringBuilder()
        if (days != 0) sb.append(days).append("d ")
        if (hours != 0) sb.append(hours).append("h ")
        if (minutes != 0) sb.append(minutes).append("m ")
        if (secs != 0) sb.append(secs).append("s")
        return String.format(prefix, sb.toString())
    }
}

class ThrowableTypeAdapter : TypeAdapter<Throwable>() {
    override fun write(out: JsonWriter, value: Throwable?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("class").value(value::class.java.name)
        out.name("message").value(value.message)
        out.name("stackTrace")
        out.beginArray()
        for (el in value.stackTrace) {
            out.beginObject()
            out.name("className").value(el.className)
            out.name("methodName").value(el.methodName)
            out.name("fileName").value(el.fileName)
            out.name("lineNumber").value(el.lineNumber)
            out.endObject()
        }
        out.endArray()
        val cause = value.cause
        out.name("cause")
        write(out, cause) // recursive
        out.name("suppressed")
        out.beginArray()
        for (s in value.suppressed) write(out, s)
        out.endArray()
        out.endObject()
    }

    override fun read(reader: JsonReader): Throwable? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        var className: String? = null
        var message: String? = null
        val stack = mutableListOf<StackTraceElement>()
        var cause: Throwable? = null
        val suppressed = mutableListOf<Throwable>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "class" -> className = if (reader.peek() != JsonToken.NULL) reader.nextString() else { reader.nextNull(); null }
                "message" -> message = if (reader.peek() != JsonToken.NULL) reader.nextString() else { reader.nextNull(); null }
                "stackTrace" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var cn = ""
                        var mn = ""
                        var fn: String? = null
                        var ln = -1
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "className" -> cn = reader.nextString()
                                "methodName" -> mn = reader.nextString()
                                "fileName" -> fn = if (reader.peek() != JsonToken.NULL) reader.nextString() else { reader.nextNull(); null }
                                "lineNumber" -> ln = reader.nextInt()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        stack.add(StackTraceElement(cn, mn, fn, ln))
                    }
                    reader.endArray()
                }
                "cause" -> cause = read(reader)
                "suppressed" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val s = read(reader)
                        if (s != null) suppressed.add(s)
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val throwable = instantiateThrowable(className, message)
        throwable.stackTrace = stack.toTypedArray()
        if (cause != null) {
            try { throwable.initCause(cause) } catch (_: IllegalStateException) { /* already has cause */ }
        }
        for (s in suppressed) {
            try { throwable.addSuppressed(s) } catch (_: Throwable) {}
        }
        return throwable
    }

    private fun instantiateThrowable(className: String?, message: String?): Throwable {
        if (className == null) return Exception(message)
        return try {
            val cls = Class.forName(className).asSubclass(Throwable::class.java)
            // prefer (String) constructor
            val ctor: Constructor<out Throwable>? = try {
                cls.getDeclaredConstructor(String::class.java).also { if (!Modifier.isPublic(it.modifiers)) it.isAccessible = true }
            } catch (_: NoSuchMethodException) {
                try {
                    cls.getDeclaredConstructor().also { if (!Modifier.isPublic(it.modifiers)) it.isAccessible = true }
                } catch (_: NoSuchMethodException) { null }
            }
            when {
                ctor != null && ctor.parameterCount == 1 -> ctor.newInstance(message)
                ctor != null -> ctor.newInstance()
                else -> Exception(message)
            }
        } catch (e: Exception) {
            Exception(message)
        }
    }
}
