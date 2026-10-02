package com.app.platform.language.backend.media

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivateMediaPathTest {
  @Test
  fun contentIdAndFileNameIsValid() {
    assertTrue(isPrivateMediaPath("practice-01/section-1.mp3"))
    assertTrue(isPrivateMediaPath("Cam18_Test2/Part_3.m4a"))
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
    ).forEach { assertFalse(isPrivateMediaPath(it), it) }
  }
}
