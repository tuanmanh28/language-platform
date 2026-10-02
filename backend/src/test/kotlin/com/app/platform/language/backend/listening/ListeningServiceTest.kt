package com.app.platform.language.backend.listening

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.fake.FakeListeningContentStore
import com.app.platform.language.backend.fake.FakeMediaStorage
import com.app.platform.language.backend.media.PublicMediaStorage
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ListeningError
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

class ListeningServiceTest {
  private val sample = BundledListeningTests.all.first()
  private val privateTest = sample.copy(id = "private-test")
  private val ownerEmail = "owner@example.com"
  private val owner = AuthIdentity("owner-uid", ownerEmail, "Owner", isEmailVerified = true)
  private val store = FakeListeningContentStore(listOf(StoredListeningTest(sample, 3, Visibility.PUBLIC)))
  private val privateAudio = FakeMediaStorage()
  private val access = ContentAccessPolicy(setOf(ownerEmail))
  private val service = serviceWithPublicAudioAt("https://cdn.example.com/audio")

  private fun serviceWithPublicAudioAt(baseUrl: String) =
    ListeningService(store, PublicMediaStorage(baseUrl), privateAudio, access)

  @Test
  fun testVersionChangesWhenTheStoredVersionChanges() =
    runTest {
      val original = service.getTest(sample.id, viewer = null).map { it.version }
      store.tests = listOf(StoredListeningTest(sample, 4, Visibility.PUBLIC))

      assertNotEquals(original, service.getTest(sample.id, viewer = null).map { it.version })
    }

  @Test
  fun testVersionChangesWhenTheAudioBaseUrlChanges() =
    runTest {
      val movedService = serviceWithPublicAudioAt("https://new-cdn.example.com/audio")

      assertNotEquals(
        service.getTest(sample.id, viewer = null).map { it.version },
        movedService.getTest(sample.id, viewer = null).map { it.version },
      )
    }

  @Test
  fun leadingSlashInAudioUrlIsNotDoubled() =
    runTest {
      val section = sample.sections.first().copy(audioUrl = "/listening/x.mp3")
      store.tests = listOf(StoredListeningTest(sample.copy(sections = listOf(section)), 3, Visibility.PUBLIC))

      val urls =
        service
          .getTest(
            sample.id,
            viewer = null,
          ).map { versioned -> versioned.value.sections.map { it.audioUrl } }

      assertEquals(Ok(listOf("https://cdn.example.com/audio/listening/x.mp3")), urls)
    }

  @Test
  fun audioUrlsAreResolvedAgainstTheBaseUrl() =
    runTest {
      val urls =
        service.getTest(sample.id, viewer = null).map { versioned ->
          versioned.value.sections.map { it.audioUrl }
        }

      assertEquals(Ok(sample.sections.map { "https://cdn.example.com/audio/${it.audioUrl}" }), urls)
    }

  @Test
  fun privateTestAudioComesFromThePrivateStorage() =
    runTest {
      store.tests = listOf(StoredListeningTest(privateTest, 1, Visibility.PRIVATE))

      val urls =
        service
          .getTest(
            privateTest.id,
            owner,
          ).map { versioned -> versioned.value.sections.map { it.audioUrl } }

      assertEquals(Ok(privateTest.sections.map { privateAudio.urlFor(it.audioUrl) }), urls)
    }

  @Test
  fun testVersionChangesWhenPrivateAudioUrlsChange() =
    runTest {
      store.tests = listOf(StoredListeningTest(privateTest, 1, Visibility.PRIVATE))
      val original = service.getTest(privateTest.id, owner).map { it.version }
      privateAudio.signature = "2"

      assertNotEquals(original, service.getTest(privateTest.id, owner).map { it.version })
    }

  @Test
  fun privateTestsAreHiddenFromAnonymousViewers() =
    runTest {
      store.tests += StoredListeningTest(privateTest, 1, Visibility.PRIVATE)

      assertEquals(Ok(listOf(sample.toSummary())), service.listTests(viewer = null).map { it.value })
      assertEquals(Err(ListeningError.NotFound), service.getTest(privateTest.id, viewer = null))
      assertEquals(
        Err(ListeningError.NotFound),
        service.submit(privateTest.id, SubmitAnswersRequest(emptyMap()), viewer = null),
      )
    }

  @Test
  fun ownerSeesPublicAndPrivateTests() =
    runTest {
      store.tests += StoredListeningTest(privateTest, 1, Visibility.PRIVATE)

      assertEquals(
        Ok(listOf(sample.toSummary(), privateTest.toSummary())),
        service.listTests(owner).map { it.value },
      )
    }

  @Test
  fun questionsAndTranscriptsAreServedUnchanged() =
    runTest {
      val served = service.getTest(sample.id, viewer = null).map { it.value }

      assertEquals(Ok(sample.allQuestions()), served.map { it.allQuestions() })
      assertEquals(
        Ok(sample.sections.map { it.transcript }),
        served.map { test ->
          test.sections.map { it.transcript }
        },
      )
    }

  @Test
  fun unknownTestIsNotFound() =
    runTest {
      assertEquals(Err(ListeningError.NotFound), service.getTest("missing", viewer = null))
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
      store.tests = listOf(StoredListeningTest(sample, 4, Visibility.PUBLIC))

      assertNotEquals(original, service.listTests(viewer = null).map { it.version })
    }

  @Test
  fun submitScoresWithTheListeningScale() =
    runTest {
      val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }

      assertEquals(Ok(9.0), service.submit(sample.id, SubmitAnswersRequest(perfect), viewer = null).map { it.band })
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    runTest {
      assertEquals(
        Err(ListeningError.NotFound),
        service.submit("missing", SubmitAnswersRequest(emptyMap()), viewer = null),
      )
    }

  @Test
  fun failingStoreMakesListUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(service.listTests(viewer = null).getError())
    }

  @Test
  fun failingStoreMakesGetUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(service.getTest(sample.id, viewer = null).getError())
    }

  @Test
  fun failingStoreMakesSubmitUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(
        service.submit(sample.id, SubmitAnswersRequest(emptyMap()), viewer = null).getError(),
      )
    }
}
