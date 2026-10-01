package com.app.platform.language.backend.reading

import com.app.platform.language.backend.fake.FakeContentStore
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class ReadingServiceTest {
  private val sample = BundledReadingTests.all.first()
  private val store = FakeContentStore(listOf(StoredReadingTest(sample, 3)))
  private val service = ReadingService(store)

  @Test
  fun knownTestIsReturnedWithItsVersion() =
    runTest {
      assertEquals(Ok(Versioned(sample, "3")), service.getTest(sample.id))
    }

  @Test
  fun unknownTestIsNotFound() =
    runTest {
      assertEquals(Err(ReadingError.NotFound), service.getTest("missing"))
    }

  @Test
  fun listSummarisesEveryTest() =
    runTest {
      assertEquals(Ok(listOf(sample.toSummary())), service.listTests().map { it.value })
    }

  @Test
  fun listVersionChangesWhenATestVersionChanges() =
    runTest {
      val original = service.listTests().map { it.version }
      store.tests = listOf(StoredReadingTest(sample, 4))

      assertNotEquals(original, service.listTests().map { it.version })
    }

  @Test
  fun submitWithoutAnswersScoresZero() =
    runTest {
      assertEquals(Ok(0), service.submit(sample.id, SubmitAnswersRequest(emptyMap())).map { it.correctCount })
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    runTest {
      assertEquals(Err(ReadingError.NotFound), service.submit("missing", SubmitAnswersRequest(emptyMap())))
    }

  @Test
  fun failingStoreMakesListUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(service.listTests().getError())
    }

  @Test
  fun failingStoreMakesGetUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(service.getTest(sample.id).getError())
    }

  @Test
  fun failingStoreMakesSubmitUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(service.submit(sample.id, SubmitAnswersRequest(emptyMap())).getError())
    }
}
