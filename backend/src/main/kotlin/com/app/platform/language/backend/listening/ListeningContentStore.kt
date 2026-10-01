package com.app.platform.language.backend.listening

import com.app.platform.language.core.model.ListeningTest

interface ListeningContentStore {
  suspend fun listeningTests(): List<StoredListeningTest>

  suspend fun listeningTest(id: String): StoredListeningTest?
}

data class StoredListeningTest(
  val test: ListeningTest,
  val version: Int,
)
