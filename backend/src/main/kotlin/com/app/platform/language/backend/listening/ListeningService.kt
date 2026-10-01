package com.app.platform.language.backend.listening

import com.app.platform.language.backend.common.Versioned
import com.app.platform.language.backend.common.catalogVersion
import com.app.platform.language.backend.common.fingerprintVersion
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
  private val audioBaseUrl: String,
) {
  suspend fun listTests(): Result<Versioned<List<ListeningTestSummary>>, ListeningError> =
    runSuspendCatching { store.listeningTests() }
      .mapError(ListeningError::Unexpected)
      .map { tests ->
        Versioned(tests.map { it.test.toSummary() }, catalogVersion(tests.map { it.test.id to it.version }))
      }

  suspend fun getTest(id: String): Result<Versioned<ListeningTest>, ListeningError> =
    findTest(id).map { stored ->
      Versioned(stored.test.withAbsoluteAudioUrls(), fingerprintVersion("${stored.version}:$audioBaseUrl"))
    }

  suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
  ): Result<ListeningResult, ListeningError> =
    findTest(id).map { stored ->
      ListeningScorer.score(stored.test, request.answers)
    }

  private suspend fun findTest(id: String): Result<StoredListeningTest, ListeningError> =
    runSuspendCatching { store.listeningTest(id) }
      .mapError(ListeningError::Unexpected)
      .andThen { stored -> stored.toResultOr { ListeningError.NotFound } }

  private fun ListeningTest.withAbsoluteAudioUrls(): ListeningTest =
    copy(sections = sections.map { it.copy(audioUrl = "$audioBaseUrl/${it.audioUrl.trimStart('/')}") })
}
