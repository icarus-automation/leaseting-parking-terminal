package com.leaseting.parkingterminal.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * What `HttpExceptionFilter` in leaseting-api sends when it refuses a request.
 *
 * Worth decoding rather than swallowing: the API's own sentence is the one a
 * guard can act on. "Hourly has no price for Truck" or "Another attendant
 * already has a shift open on this terminal" tells them what to do next; a
 * generic "Something went wrong" sends them looking for a supervisor.
 */
@Serializable
data class ApiErrorResponse(
    val statusCode: Int? = null,
    val message: ApiErrorMessage? = null,
    val error: String? = null,
)

/**
 * The `message` field, which is a sentence for a domain error and a list of
 * them for a validation failure. Both shapes collapse to one line of text.
 */
@Serializable(with = ApiErrorMessageSerializer::class)
data class ApiErrorMessage(val lines: List<String>) {
    val text: String
        get() = lines.filter { it.isNotBlank() }.joinToString(" ")
}

private object ApiErrorMessageSerializer : KSerializer<ApiErrorMessage> {
    private val listSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ApiErrorMessage", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ApiErrorMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return ApiErrorMessage(listOf(decoder.decodeString()))

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> ApiErrorMessage(
                jsonDecoder.json.decodeFromJsonElement(listSerializer, element),
            )

            is JsonPrimitive -> ApiErrorMessage(listOf(element.content))
            else -> ApiErrorMessage(emptyList())
        }
    }

    /** Encoding never happens — the terminal only ever reads these. */
    override fun serialize(encoder: Encoder, value: ApiErrorMessage) {
        encoder.encodeString(value.text)
    }
}
