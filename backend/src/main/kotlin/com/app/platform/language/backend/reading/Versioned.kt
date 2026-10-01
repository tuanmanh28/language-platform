package com.app.platform.language.backend.reading

data class Versioned<T>(
  val value: T,
  val version: String,
)
