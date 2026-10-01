package com.app.platform.language.backend.media

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class S3UrlPresignerTest {
  // Example request and credentials from the AWS Signature Version 4 query string documentation.
  private val presigner =
    S3UrlPresigner(
      host = "examplebucket.s3.amazonaws.com",
      region = "us-east-1",
      accessKeyId = "AKIAIOSFODNN7EXAMPLE",
      secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
    )
  private val signedAt = Instant.parse("2013-05-24T00:00:00Z")

  @Test
  fun signatureMatchesTheAwsReferenceExample() {
    assertEquals(
      "https://examplebucket.s3.amazonaws.com/test.txt" +
        "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
        "&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request" +
        "&X-Amz-Date=20130524T000000Z" +
        "&X-Amz-Expires=86400" +
        "&X-Amz-SignedHeaders=host" +
        "&X-Amz-Signature=aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404",
      presigner.presignGet("/test.txt", 24.hours, signedAt),
    )
  }

  @Test
  fun pathSegmentsArePercentEncodedButSlashesAreKept() {
    val url = presigner.presignGet("/audio/test one/section+1.mp3", 1.hours, signedAt)

    assertEquals("https://examplebucket.s3.amazonaws.com/audio/test%20one/section%2B1.mp3", url.substringBefore('?'))
  }
}
