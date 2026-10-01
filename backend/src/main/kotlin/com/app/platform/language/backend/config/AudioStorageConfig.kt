package com.app.platform.language.backend.config

sealed interface AudioStorageConfig {
  data class Local(
    val apiBaseUrl: String,
  ) : AudioStorageConfig

  data class R2(
    val accountId: String,
    val bucket: String,
    val accessKeyId: String,
    val secretAccessKey: String,
  ) : AudioStorageConfig {
    override fun toString(): String =
      "R2(accountId=$accountId, bucket=$bucket, accessKeyId=$accessKeyId, secretAccessKey=***)"
  }
}
