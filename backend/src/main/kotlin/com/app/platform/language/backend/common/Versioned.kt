package com.app.platform.language.backend.common

import java.security.MessageDigest

data class Versioned<T>(
  val value: T,
  val version: String,
)

fun catalogVersion(versionsById: List<Pair<String, Int>>): String =
  fingerprintVersion(versionsById.joinToString(",") { (id, version) -> "$id:$version" })

fun fingerprintVersion(fingerprint: String): String =
  MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray()).toHexString()
