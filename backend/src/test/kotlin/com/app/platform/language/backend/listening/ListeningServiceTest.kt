package com.app.platform.language.backend.listening

import com.app.platform.language.backend.fake.FakeListeningContentStore
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
  private val store = FakeListeningContentStore(listOf(StoredListeningTest(sample, 3)))
  private val service = ListeningService(store, audioBaseUrl = "https://cdn.example.com/audio")

  @Test
  fun testVersionChangesWhenTheStoredVersionChanges() =
    runTest {
      val original = service.getTest(sample.id).map { it.version }
      store.tests = listOf(StoredListeningTest(sample, 4))

      assertNotEquals(original, service.getTest(sample.id).map { it.version })
    }

  @Test
  fun testVersionChangesWhenTheAudioBaseUrlChanges() =
    runTest {
      val movedService = ListeningService(store, audioBaseUrl = "https://new-cdn.example.com/audio")

      assertNotEquals(service.getTest(sample.id).map { it.version }, movedService.getTest(sample.id).map { it.version })
    }

  @Test
  fun leadingSlashInAudioUrlIsNotDoubled() =
    runTest {
      val section = sample.sections.first().copy(audioUrl = "/listening/x.mp3")
      store.tests = listOf(StoredListeningTest(sample.copy(sections = listOf(section)), 3))

      val urls = service.getTest(sample.id).map { versioned -> versioned.value.sections.map { it.audioUrl } }

      assertEquals(Ok(listOf("https://cdn.example.com/audio/listening/x.mp3")), urls)
    }

  @Test
  fun audioUrlsAreResolvedAgainstTheBaseUrl() =
    runTest {
      val urls = service.getTest(sample.id).map { versioned -> versioned.value.sections.map { it.audioUrl } }

      assertEquals(Ok(sample.sections.map { "https://cdn.example.com/audio/${it.audioUrl}" }), urls)
    }

  @Test
  fun questionsAndTranscriptsAreServedUnchanged() =
    runTest {
      val served = service.getTest(sample.id).map { it.value }

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
      assertEquals(Err(ListeningError.NotFound), service.getTest("missing"))
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
      store.tests = listOf(StoredListeningTest(sample, 4))

      assertNotEquals(original, service.listTests().map { it.version })
    }

  @Test
  fun submitScoresWithTheListeningScale() =
    runTest {
      val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }

      assertEquals(Ok(9.0), service.submit(sample.id, SubmitAnswersRequest(perfect)).map { it.band })
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    runTest {
      assertEquals(Err(ListeningError.NotFound), service.submit("missing", SubmitAnswersRequest(emptyMap())))
    }

  @Test
  fun failingStoreMakesListUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(service.listTests().getError())
    }

  @Test
  fun failingStoreMakesGetUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(service.getTest(sample.id).getError())
    }

  @Test
  fun failingStoreMakesSubmitUnexpected() =
    runTest {
      store.nextError = IllegalStateException("store is down")

      assertIs<ListeningError.Unexpected>(service.submit(sample.id, SubmitAnswersRequest(emptyMap())).getError())
    }
}
