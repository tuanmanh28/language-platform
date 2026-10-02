package com.app.platform.language.backend.listening

import com.app.platform.language.backend.audio.AudioStorage
import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.common.Versioned
import com.app.platform.language.backend.common.catalogVersion
import com.app.platform.language.backend.common.fingerprintVersion
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.core.exam.ListeningScorer
import com.app.platform.language.core.model.ListeningError
import com.app.platform.language.core.model.ListeningResult
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ListeningTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

class ListeningService(
  private val store: ListeningContentStore,
  private val publicAudio: AudioStorage,
  private val privateAudio: AudioStorage,
  private val access: ContentAccessPolicy,
) {
  suspend fun listTests(viewer: AuthIdentity?): Result<Versioned<List<ListeningTestSummary>>, ListeningError> =
    runSuspendCatching { store.listeningTests() }
      .mapError(ListeningError::Unexpected)
      .map { stored ->
        val tests = stored.filter { access.canSee(viewer, it.visibility) }
        Versioned(tests.map { it.test.toSummary() }, catalogVersion(tests.map { it.test.id to it.version }))
      }

  suspend fun getTest(
    id: String,
    viewer: AuthIdentity?,
  ): Result<Versioned<ListeningTest>, ListeningError> =
    findTest(id, viewer).map { stored ->
      val test = stored.withAudioUrls()
      Versioned(test, fingerprintVersion("${stored.version}:${test.sections.joinToString { it.audioUrl }}"))
    }

  suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
    viewer: AuthIdentity?,
  ): Result<ListeningResult, ListeningError> =
    findTest(id, viewer).map { stored ->
      ListeningScorer.score(stored.test, request.answers)
    }

  private suspend fun findTest(
    id: String,
    viewer: AuthIdentity?,
  ): Result<StoredListeningTest, ListeningError> =
    runSuspendCatching { store.listeningTest(id) }
      .mapError(ListeningError::Unexpected)
      .andThen { stored ->
        stored?.takeIf { access.canSee(viewer, it.visibility) }.toResultOr { ListeningError.NotFound }
      }

  private fun StoredListeningTest.withAudioUrls(): ListeningTest {
    val audio =
      when (visibility) {
        Visibility.PUBLIC -> publicAudio
        Visibility.PRIVATE -> privateAudio
      }
    return test.copy(sections = test.sections.map { it.copy(audioUrl = audio.urlFor(it.audioUrl)) })
  }
}
