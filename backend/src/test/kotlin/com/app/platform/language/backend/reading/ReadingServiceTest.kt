package com.app.platform.language.backend.reading

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.common.Versioned
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.content.withoutAnswerKey
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
  private val privateTest = sample.copy(id = "private-test")
  private val ownerEmail = "owner@example.com"
  private val owner = AuthIdentity("owner-uid", ownerEmail, "Owner", isEmailVerified = true)
  private val store = FakeContentStore(listOf(StoredReadingTest(sample, 3, Visibility.PUBLIC)))
  private val service = ReadingService(store, ContentAccessPolicy(setOf(ownerEmail)))

  @Test
  fun knownTestIsReturnedWithoutAnswerKeyWithItsVersion() =
    runTest {
      assertEquals(Ok(Versioned(sample.withoutAnswerKey(), "3")), service.getTest(sample.id, viewer = null))
    }

  @Test
  fun unknownTestIsNotFound() =
    runTest {
      assertEquals(Err(ReadingError.NotFound), service.getTest("missing", viewer = null))
    }

  @Test
  fun listSummarisesEveryTest() =
    runTest {
      assertEquals(Ok(listOf(sample.toSummary())), service.listTests(viewer = null).map { it.value })
    }

  @Test
  fun listVersionChangesWhenATestVersionChanges() =
    runTest {
      val original = service.listTests(viewer = null).map { it.version }
      store.tests = listOf(StoredReadingTest(sample, 4, Visibility.PUBLIC))

      assertNotEquals(original, service.listTests(viewer = null).map { it.version })
    }

  @Test
  fun privateTestsAreHiddenFromAnonymousViewers() =
    runTest {
      store.tests += StoredReadingTest(privateTest, 1, Visibility.PRIVATE)

      assertEquals(Ok(listOf(sample.toSummary())), service.listTests(viewer = null).map { it.value })
      assertEquals(Err(ReadingError.NotFound), service.getTest(privateTest.id, viewer = null))
      assertEquals(
        Err(ReadingError.NotFound),
        service.submit(privateTest.id, SubmitAnswersRequest(emptyMap()), viewer = null),
      )
    }

  @Test
  fun ownerSeesPublicAndPrivateTests() =
    runTest {
      store.tests += StoredReadingTest(privateTest, 1, Visibility.PRIVATE)

      assertEquals(
        Ok(listOf(sample.toSummary(), privateTest.toSummary())),
        service.listTests(owner).map { it.value },
      )
      assertEquals(Ok(privateTest.withoutAnswerKey()), service.getTest(privateTest.id, owner).map { it.value })
    }

  @Test
  fun listVersionDiffersBetweenOwnerAndAnonymousViewers() =
    runTest {
      store.tests += StoredReadingTest(privateTest, 1, Visibility.PRIVATE)

      assertNotEquals(service.listTests(viewer = null).map { it.version }, service.listTests(owner).map { it.version })
    }

  @Test
  fun submitWithoutAnswersScoresZero() =
    runTest {
      assertEquals(
        Ok(0),
        service.submit(sample.id, SubmitAnswersRequest(emptyMap()), viewer = null).map { it.correctCount },
      )
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    runTest {
      assertEquals(
        Err(ReadingError.NotFound),
        service.submit("missing", SubmitAnswersRequest(emptyMap()), viewer = null),
      )
    }

  @Test
  fun failingStoreMakesListUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(service.listTests(viewer = null).getError())
    }

  @Test
  fun failingStoreMakesGetUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(service.getTest(sample.id, viewer = null).getError())
    }

  @Test
  fun failingStoreMakesSubmitUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ReadingError.Unexpected>(
        service.submit(sample.id, SubmitAnswersRequest(emptyMap()), viewer = null).getError(),
      )
    }
}
