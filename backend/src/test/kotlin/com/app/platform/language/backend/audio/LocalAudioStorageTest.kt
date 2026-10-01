package com.app.platform.language.backend.audio

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalAudioStorageTest {
  @TempDir
  lateinit var tempDir: Path

  private val audioDir by lazy { tempDir.resolve("content/audio").createDirectories() }
  private val storage by lazy { LocalAudioStorage(audioDir, "http://localhost:8080") }

  @Test
  fun urlPointsAtTheStreamingEndpoint() {
    assertEquals(
      "http://localhost:8080/api/v1/listening/audio/practice-01/section-1.mp3",
      storage.urlFor("practice-01/section-1.mp3"),
    )
  }

  @Test
  fun urlOfAFileIsTheUrlItIsServedAt() {
    audioDir
      .resolve("practice-01")
      .createDirectories()
      .resolve("section-1.mp3")
      .writeText("audio")
    val (testId, fileName) = storage.urlFor("practice-01/section-1.mp3").split('/').takeLast(2)

    assertEquals(audioDir.resolve("practice-01/section-1.mp3"), storage.file(testId, fileName))
  }

  @Test
  fun existingFileIsFound() {
    val file = audioDir.resolve("practice-01").createDirectories().resolve("section-1.mp3")
    file.writeText("audio")

    assertEquals(file, storage.file("practice-01", "section-1.mp3"))
  }

  @Test
  fun missingFileIsNotFound() {
    assertNull(storage.file("practice-01", "section-9.mp3"))
  }

  @Test
  fun pathTraversalIsRejected() {
    audioDir.resolveSibling("secret.txt").writeText("secret")

    assertNull(storage.file("..", "secret.txt"))
    assertNull(storage.file(".", "..%2Fsecret.txt"))
  }

  @Test
  fun symlinkLeavingTheAudioDirectoryIsRejected() {
    val outside = tempDir.resolve("outside.mp3").apply { writeText("audio") }
    audioDir
      .resolve("practice-01")
      .createDirectories()
      .resolve("link.mp3")
      .createSymbolicLinkPointingTo(outside)

    assertNull(storage.file("practice-01", "link.mp3"))
  }
}
