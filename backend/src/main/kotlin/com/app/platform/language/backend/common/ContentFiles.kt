package com.app.platform.language.backend.common

import com.app.platform.language.core.model.ContentJson
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import kotlinx.serialization.KSerializer
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

fun <T> loadContentFiles(
  directory: Path,
  serializer: KSerializer<T>,
): Result<List<T>, SeedError> =
  runCatching { directory.listDirectoryEntries("*.json").sorted() }
    .mapError { SeedError.UnreadableDirectory(directory, it) }
    .andThen { files -> files.map { parseContentFile(it, serializer) }.combine() }

private fun <T> parseContentFile(
  file: Path,
  serializer: KSerializer<T>,
): Result<T, SeedError> =
  runCatching { ContentJson.decodeFromString(serializer, file.readText()) }
    .mapError { SeedError.InvalidContent(file, it) }
