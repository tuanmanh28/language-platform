package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingServiceTest {
  private val sample = BundledReadingTests.all.first()
  private val service = ReadingService(BundledContentStore())

  @Test
  fun knownTestIsReturned() {
    assertEquals(Ok(sample), service.getTest(sample.id))
  }

  @Test
  fun unknownTestIsNotFound() {
    assertEquals(Err(ReadingError.NotFound), service.getTest("missing"))
  }

  @Test
  fun submitWithoutAnswersScoresZero() {
    assertEquals(Ok(0), service.submit(sample.id, SubmitAnswersRequest(emptyMap())).map { it.correctCount })
  }

  @Test
  fun submitToUnknownTestIsNotFound() {
    assertEquals(Err(ReadingError.NotFound), service.submit("missing", SubmitAnswersRequest(emptyMap())))
  }
}
