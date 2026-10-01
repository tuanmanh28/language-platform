package com.app.platform.language.backend.database

import com.app.platform.language.backend.config.DatabaseConfig
import com.github.michaelbull.result.getError
import kotlin.test.Test
import kotlin.test.assertIs

class AppDatabaseConnectTest {
  @Test
  fun unreachableDatabaseFailsToConnect() {
    val config = DatabaseConfig(url = "jdbc:postgresql://localhost:1/language_platform", user = "app", password = "app")

    val result = AppDatabase.connect(config)

    assertIs<DatabaseError.Unreachable>(result.getError())
  }
}
