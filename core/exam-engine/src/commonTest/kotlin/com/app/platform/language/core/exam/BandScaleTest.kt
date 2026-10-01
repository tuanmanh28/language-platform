package com.app.platform.language.core.exam

import com.app.platform.language.core.model.IeltsModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BandScaleTest {
  @Test
  fun academicReadingBoundaries() {
    assertEquals(9.0, BandScale.readingBand(IeltsModule.ACADEMIC, 40, 40))
    assertEquals(7.0, BandScale.readingBand(IeltsModule.ACADEMIC, 30, 40))
    assertEquals(6.5, BandScale.readingBand(IeltsModule.ACADEMIC, 29, 40))
    assertEquals(6.0, BandScale.readingBand(IeltsModule.ACADEMIC, 23, 40))
    assertEquals(0.0, BandScale.readingBand(IeltsModule.ACADEMIC, 0, 40))
  }

  @Test
  fun generalTrainingIsStricterThanAcademic() {
    assertEquals(6.0, BandScale.readingBand(IeltsModule.GENERAL_TRAINING, 30, 40))
    assertEquals(7.0, BandScale.readingBand(IeltsModule.ACADEMIC, 30, 40))
  }

  @Test
  fun shortTestIsScaledToForty() {
    // 13/13 -> 40/40
    assertEquals(9.0, BandScale.readingBand(IeltsModule.ACADEMIC, 13, 13))
    // 10/13 -> 30.8 -> 31/40 -> 7.0
    assertEquals(7.0, BandScale.readingBand(IeltsModule.ACADEMIC, 10, 13))
  }

  @Test
  fun listening() {
    assertEquals(7.5, BandScale.listeningBand(32, 40))
    assertEquals(6.5, BandScale.listeningBand(26, 40))
  }

  @Test
  fun overallBandRounding() {
    assertEquals(6.5, BandScale.overallBand(6.5, 6.5, 5.0, 7.0)) // 6.25 -> 6.5
    assertEquals(6.0, BandScale.overallBand(6.0, 6.0, 6.0, 6.0))
    assertEquals(5.0, BandScale.overallBand(5.0, 5.0, 5.0, 5.5)) // 5.125 -> 5.0
    assertEquals(7.0, BandScale.overallBand(6.5, 7.0, 6.5, 7.0)) // 6.75 -> 7.0
  }

  @Test
  fun invalidInputIsRejected() {
    assertFailsWith<IllegalArgumentException> { BandScale.readingBand(IeltsModule.ACADEMIC, 5, 0) }
    assertFailsWith<IllegalArgumentException> { BandScale.readingBand(IeltsModule.ACADEMIC, 41, 40) }
  }
}
