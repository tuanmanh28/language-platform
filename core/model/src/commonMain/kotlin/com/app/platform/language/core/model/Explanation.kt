package com.app.platform.language.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Explanation(
  val text: String,
  val evidence: Evidence? = null,
  val paraphrases: List<Paraphrase> = emptyList(),
  val trap: String? = null,
)

@Serializable
sealed interface Evidence {
  val quote: String
}

@Serializable
@SerialName("passage")
data class PassageEvidence(
  val paragraphId: String,
  override val quote: String,
) : Evidence

@Serializable
@SerialName("transcript")
data class TranscriptEvidence(
  val sectionNumber: Int,
  val startSeconds: Int,
  val endSeconds: Int,
  override val quote: String,
) : Evidence

@Serializable
data class Paraphrase(
  val inQuestion: String,
  val inSource: String,
)
