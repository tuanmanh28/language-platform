package com.app.platform.language.backend.plugins

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import net.logstash.logback.encoder.LogstashEncoder
import kotlin.test.Test
import kotlin.test.assertIs

class LoggingConfigTest {
  @Test
  fun logsArePlainTextWhenEnvIsUnset() {
    assertIs<PatternLayoutEncoder>(stdoutEncoderFor(appEnv = null))
  }

  @Test
  fun localLogsArePlainText() {
    assertIs<PatternLayoutEncoder>(stdoutEncoderFor("local"))
  }

  @Test
  fun nonLocalLogsAreJson() {
    assertIs<LogstashEncoder>(stdoutEncoderFor("prod"))
  }

  private fun stdoutEncoderFor(appEnv: String?): Any? {
    val context = LoggerContext()
    appEnv?.let { context.putProperty("APP_ENV", it) }
    JoranConfigurator().apply { setContext(context) }.doConfigure(javaClass.getResource("/logback.xml"))
    val appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT")
    return (appender as ConsoleAppender<ILoggingEvent>).encoder
  }
}
