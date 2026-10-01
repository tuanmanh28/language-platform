package com.app.platform.language.core.exam

import com.app.platform.language.core.model.IeltsModule
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Converts a raw score (correct answers out of 40) to a band.
 *
 * The tables below are the widely published REFERENCE conversions; the official IELTS tables vary
 * slightly per test. Results shown to users must be labelled "estimated band".
 */
object BandScale {
  const val FULL_TEST_QUESTIONS = 40

  /** (minimum correct answers out of 40, band), sorted descending. */
  private val academicReading =
    listOf(
      39 to 9.0,
      37 to 8.5,
      35 to 8.0,
      33 to 7.5,
      30 to 7.0,
      27 to 6.5,
      23 to 6.0,
      19 to 5.5,
      15 to 5.0,
      13 to 4.5,
      10 to 4.0,
      8 to 3.5,
      6 to 3.0,
      4 to 2.5,
      2 to 2.0,
      1 to 1.0,
      0 to 0.0,
    )

  private val generalTrainingReading =
    listOf(
      40 to 9.0,
      39 to 8.5,
      37 to 8.0,
      36 to 7.5,
      34 to 7.0,
      32 to 6.5,
      30 to 6.0,
      27 to 5.5,
      23 to 5.0,
      19 to 4.5,
      15 to 4.0,
      12 to 3.5,
      9 to 3.0,
      6 to 2.5,
      3 to 2.0,
      1 to 1.0,
      0 to 0.0,
    )

  private val listening =
    listOf(
      39 to 9.0,
      37 to 8.5,
      35 to 8.0,
      32 to 7.5,
      30 to 7.0,
      26 to 6.5,
      23 to 6.0,
      18 to 5.5,
      16 to 5.0,
      13 to 4.5,
      10 to 4.0,
      8 to 3.5,
      6 to 3.0,
      4 to 2.5,
      2 to 2.0,
      1 to 1.0,
      0 to 0.0,
    )

  fun readingBand(
    module: IeltsModule,
    correct: Int,
    total: Int,
  ): Double {
    val table =
      when (module) {
        IeltsModule.ACADEMIC -> academicReading
        IeltsModule.GENERAL_TRAINING -> generalTrainingReading
      }
    return lookup(table, scaleToFullTest(correct, total))
  }

  fun listeningBand(
    correct: Int,
    total: Int,
  ): Double = lookup(listening, scaleToFullTest(correct, total))

  /**
   * Overall band = mean of the four skills, rounded the IELTS way:
   * fraction < .25 -> .0, < .75 -> .5, otherwise up to the next whole band.
   */
  fun overallBand(
    listening: Double,
    reading: Double,
    writing: Double,
    speaking: Double,
  ): Double {
    val average = (listening + reading + writing + speaking) / 4.0
    val whole = floor(average)
    val fraction = average - whole
    return when {
      fraction < 0.25 -> whole
      fraction < 0.75 -> whole + 0.5
      else -> whole + 1.0
    }
  }

  /** Short practice tests (e.g. 13 questions) are scaled to 40 questions to estimate a band. */
  internal fun scaleToFullTest(
    correct: Int,
    total: Int,
  ): Int {
    require(total > 0) { "total must be > 0" }
    require(correct in 0..total) { "correct must be within 0..total" }
    if (total == FULL_TEST_QUESTIONS) return correct
    return (correct * FULL_TEST_QUESTIONS.toDouble() / total).roundToInt()
  }

  private fun lookup(
    table: List<Pair<Int, Double>>,
    raw: Int,
  ): Double = table.first { (minimum, _) -> raw >= minimum }.second
}
