package com.app.platform.language.backend.media

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration

class S3UrlPresigner(
  private val host: String,
  private val region: String,
  private val accessKeyId: String,
  private val secretAccessKey: String,
) {
  fun presignGet(
    objectPath: String,
    lifetime: Duration,
    signedAt: Instant,
  ): String {
    val amzDate = amzDateFormat.format(signedAt)
    val date = amzDate.take(DATE_LENGTH)
    val scope = "$date/$region/$SERVICE/$TERMINATOR"
    val canonicalPath = objectPath.split('/').joinToString("/") { it.uriEncode() }
    val query =
      sortedMapOf(
        "X-Amz-Algorithm" to ALGORITHM,
        "X-Amz-Credential" to "$accessKeyId/$scope",
        "X-Amz-Date" to amzDate,
        "X-Amz-Expires" to lifetime.inWholeSeconds.toString(),
        "X-Amz-SignedHeaders" to "host",
      ).entries.joinToString("&") { (name, value) -> "${name.uriEncode()}=${value.uriEncode()}" }
    val canonicalRequest = listOf("GET", canonicalPath, query, "host:$host", "", "host", UNSIGNED_PAYLOAD)
    val stringToSign = listOf(ALGORITHM, amzDate, scope, sha256Hex(canonicalRequest.joinToString("\n")))
    val signingKey =
      listOf(date, region, SERVICE, TERMINATOR).fold("AWS4$secretAccessKey".toByteArray()) { key, part ->
        hmac(key, part)
      }
    val signature = hmac(signingKey, stringToSign.joinToString("\n")).toHexString()
    return "https://$host$canonicalPath?$query&X-Amz-Signature=$signature"
  }

  private fun String.uriEncode(): String =
    toByteArray().joinToString("") { byte ->
      val char = byte.toInt().toChar()
      if (byte >= 0 && (char.isLetterOrDigit() || char in UNRESERVED)) char.toString() else "%%%02X".format(byte)
    }

  private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toHexString()

  private fun hmac(
    key: ByteArray,
    value: String,
  ): ByteArray = Mac.getInstance(HMAC).apply { init(SecretKeySpec(key, HMAC)) }.doFinal(value.toByteArray())

  private companion object {
    const val ALGORITHM = "AWS4-HMAC-SHA256"
    const val SERVICE = "s3"
    const val TERMINATOR = "aws4_request"
    const val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"
    const val HMAC = "HmacSHA256"
    const val UNRESERVED = "-._~"
    const val DATE_LENGTH = 8
    val amzDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
  }
}
