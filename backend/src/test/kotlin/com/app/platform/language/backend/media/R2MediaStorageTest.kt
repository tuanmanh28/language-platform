package com.app.platform.language.backend.media

import com.app.platform.language.backend.config.AudioStorageConfig
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class R2MediaStorageTest {
  private val config =
    AudioStorageConfig.R2(
      accountId = "0123abcd",
      bucket = "private-audio",
      accessKeyId = "test-key-id",
      secretAccessKey = "test-secret",
    )
  private val now = Instant.parse("2026-10-02T09:30:00Z")

  private fun storageAt(
    instant: Instant,
    keyPrefix: String = "",
  ) = R2MediaStorage(config, keyPrefix, Clock.fixed(instant, ZoneOffset.UTC))

  @Test
  fun urlPointsAtTheObjectInTheBucketAndIsShortLived() {
    val url = storageAt(now).urlFor("/practice-01/section-1.mp3")

    assertTrue(url.startsWith("https://0123abcd.r2.cloudflarestorage.com/private-audio/practice-01/section-1.mp3?"))
    assertTrue("X-Amz-Credential=test-key-id%2F20261002%2Fauto%2Fs3%2Faws4_request" in url)
    assertTrue("X-Amz-Date=20261002T093000Z" in url)
    assertTrue("X-Amz-Expires=5400" in url)
    assertTrue("test-secret" !in url)
  }

  @Test
  fun keyPrefixPlacesTheObjectInItsFolder() {
    val url = storageAt(now, keyPrefix = "images/").urlFor("task-1/chart.png")

    assertTrue(url.startsWith("https://0123abcd.r2.cloudflarestorage.com/private-audio/images/task-1/chart.png?"))
  }

  @Test
  fun urlsAreStableWithinASigningWindow() {
    assertEquals(
      storageAt(now).urlFor("practice-01/section-1.mp3"),
      storageAt(now.plusSeconds(29 * 60 + 59)).urlFor("practice-01/section-1.mp3"),
    )
  }

  @Test
  fun urlsAreSignedAgainInTheNextWindow() {
    assertNotEquals(
      storageAt(now).urlFor("practice-01/section-1.mp3"),
      storageAt(now.plusSeconds(30 * 60)).urlFor("practice-01/section-1.mp3"),
    )
  }

  @Test
  fun secretIsHiddenFromToString() {
    assertFalse("test-secret" in config.toString())
  }
}
