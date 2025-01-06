package cz.marvincz.transcript.tts.utils

import java.util.regex.Pattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val json = Json {
    explicitNulls = false

    serializersModule = SerializersModule {
        contextual(Regex::class, RegexSerializer)
    }
}

object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Regex") {
        element<String>("pattern")
        element<Int>("flags", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: Regex) {
        val pattern = value.toPattern()
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, pattern.pattern())
            encodeIntElement(descriptor, 1, pattern.flags())
        }
    }

    override fun deserialize(decoder: Decoder): Regex {
        return decoder.decodeStructure(descriptor) {
            var pattern: String? = null
            var flags: Int? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> pattern = decodeStringElement(descriptor, index)
                    1 -> flags = decodeIntElement(descriptor, index)
                    else -> throw SerializationException("Unknown index $index")
                }
            }

            if (pattern == null) throw SerializationException("pattern missing")

            Pattern.compile(pattern, flags ?: 0).toRegex()
        }
    }
}