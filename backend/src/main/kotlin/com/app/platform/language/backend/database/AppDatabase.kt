package com.app.platform.language.backend.database

import com.app.platform.language.backend.config.DatabaseConfig
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.runCatching
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class AppDatabase private constructor(
  private val dataSource: HikariDataSource,
) : AutoCloseable {
  private val database = Database.connect(dataSource)

  suspend fun <T> tx(block: JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) {
      suspendTransaction(database) { block() }
    }

  suspend fun isReachable(): Boolean =
    runSuspendCatching {
      withContext(Dispatchers.IO) {
        dataSource.connection.use { it.isValid(VALIDATION_TIMEOUT_SECONDS) }
      }
    }.onErr { logger.warn("Database health check failed", it) }
      .getOr(false)

  override fun close() = dataSource.close()

  companion object {
    private const val MAX_POOL_SIZE = 10
    private const val CONNECTION_TIMEOUT_MILLIS = 5_000L
    private const val VALIDATION_TIMEOUT_SECONDS = 2
    private val logger = LoggerFactory.getLogger(AppDatabase::class.java)

    fun connect(config: DatabaseConfig): Result<AppDatabase, DatabaseError> =
      runCatching { HikariDataSource(config.toHikariConfig()) }
        .mapError(DatabaseError::Unreachable)
        .andThen { dataSource ->
          migrate(dataSource)
            .map { AppDatabase(dataSource) }
            .onErr { dataSource.close() }
        }

    private fun migrate(dataSource: DataSource): Result<Unit, DatabaseError> =
      runCatching {
        Flyway
          .configure()
          .dataSource(dataSource)
          .load()
          .migrate()
      }.map { Unit }
        .mapError(DatabaseError::MigrationFailed)

    private fun DatabaseConfig.toHikariConfig() =
      HikariConfig().also {
        it.jdbcUrl = url
        it.username = user
        it.password = password
        it.maximumPoolSize = MAX_POOL_SIZE
        it.connectionTimeout = CONNECTION_TIMEOUT_MILLIS
      }
  }
}
