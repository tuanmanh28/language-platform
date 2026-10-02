package com.app.platform.language.core.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

// Content written before structured explanations stored the explanation as a plain string.
internal object LegacyExplanationSerializer : JsonTransformingSerializer<Explanation>(Explanation.serializer()) {
  override fun transformDeserialize(element: JsonElement): JsonElement =
    if (element is JsonPrimitive && element.isString) JsonObject(mapOf("text" to element)) else element
}
