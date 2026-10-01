package com.app.platform.language.backend.media

import com.app.platform.language.backend.config.AudioStorageConfig
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class R2MediaStorage(
  config: AudioStorageConfig.R2,
  private val keyPrefix: String = "",
  private val clock: Clock = Clock.systemUTC(),
) : MediaStorage {
  private val bucket = config.bucket
  private val presigner =
    S3UrlPresigner(
      host = "${config.accountId}.r2.cloudflarestorage.com",
      region = R2_REGION,
      accessKeyId = config.accessKeyId,
      secretAccessKey = config.secretAccessKey,
    )

  override fun urlFor(path: String): String =
    presigner.presignGet("/$bucket/$keyPrefix${path.trimStart('/')}", URL_LIFETIME, signingWindowStart())

  // Signing at the window start keeps URLs, and so the content's ETag, stable until the window ends.
  private fun signingWindowStart(): Instant {
    val windowMillis = SIGNING_WINDOW.inWholeMilliseconds
    return Instant.ofEpochMilli(clock.millis() / windowMillis * windowMillis)
  }

  private companion object {
    const val R2_REGION = "auto"
    val SIGNING_WINDOW = 30.minutes

    // Every URL stays valid for at least an hour after it is issued, long enough to finish a listening test.
    val URL_LIFETIME = SIGNING_WINDOW + 60.minutes
  }
}
