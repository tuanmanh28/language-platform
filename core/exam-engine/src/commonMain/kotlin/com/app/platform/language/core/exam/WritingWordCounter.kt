package com.app.platform.language.core.exam

object WritingWordCounter {
  // En and em dashes join clauses, not words, so they separate words; hyphens keep a compound as one word.
  private val separators = Regex("[\\s–—]+")

  fun count(text: String): Int = text.split(separators).count { token -> token.any { it.isLetterOrDigit() } }
}
