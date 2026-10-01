package com.app.platform.language.shared.reading.data

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

internal class KtorReadingApi(
  private val client: HttpClient,
) : ReadingApi {
  override suspend fun listTests(): Result<List<ReadingTestSummary>, ReadingError> =
    runSuspendCatching { client.get("reading/tests").body<List<ReadingTestSummary>>() }
      .mapError { it.toReadingError() }

  override suspend fun getTest(id: String): Result<ReadingTest, ReadingError> =
    runSuspendCatching { client.get("reading/tests/$id").body<ReadingTest>() }
      .mapError { it.toReadingError() }
}

private fun Throwable.toReadingError(): ReadingError =
  when (this) {
    is ClientRequestException -> {
      if (response.status == HttpStatusCode.NotFound) ReadingError.NotFound else ReadingError.Unexpected(this)
    }

    is IOException -> {
      ReadingError.Offline
    }

    else -> {
      ReadingError.Unexpected(this)
    }
  }
