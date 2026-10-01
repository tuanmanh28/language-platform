package com.app.platform.language.backend.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivateAudioPathTest {
  @Test
  fun testIdAndFileNameIsValid() {
    assertTrue(isPrivateAudioPath("practice-01/section-1.mp3"))
    assertTrue(isPrivateAudioPath("Cam18_Test2/Part_3.m4a"))
  }

  @Test
  fun otherShapesAreInvalid() {
    listOf(
      "section-1.mp3",
      "/practice-01/section-1.mp3",
      "listening/practice-01/section-1.mp3",
      "practice-01/section 1.mp3",
      "practice-01/.hidden.mp3",
      "../section-1.mp3",
      "practice-01/",
      "",
    ).forEach { assertFalse(isPrivateAudioPath(it), it) }
  }
}
