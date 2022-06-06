/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.scom.logging;

import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.EclipseLinkLogRecord;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

/**
 * Logger for JPA log messages.
 */
public class JpaLogger extends AbstractSessionLog {

  /**
   * The formatter.
   */
  private final Formatter formatter = new SimpleFormatter();

  /**
   * The log level.
   */
  public Level logLevel = Level.WARNING;


  // ----- Constants ---------------------------------------------------------

  /**
   * The logger.
   */
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JpaLogger.class);

  /**
   * The java log levels.
   */
  public static final Level[] JAVA_LOG_LEVELS = new Level[]{
      Level.ALL,     Level.FINEST, Level.FINER,
      Level.FINE,    Level.CONFIG, Level.INFO,
      Level.WARNING, Level.SEVERE, Level.OFF
  };


  // ----- AbstractSessionLog ------------------------------------------------

  @Override
  public void log(SessionLogEntry sessionLogEntry) {

    Logger logger = getLogger();

    switch (sessionLogEntry.getLevel()) {
      case SEVERE:
        logger.error(getLogMessage(sessionLogEntry));
        break;

      case WARNING:
        logger.warn(getLogMessage(sessionLogEntry));
        break;

      case INFO:
      case CONFIG:
        logger.info(getLogMessage(sessionLogEntry));
        break;

      case FINE:
      case FINER:
      case FINEST:
        logger.debug(getLogMessage(sessionLogEntry));
        break;

      case ALL:
        logger.trace(getLogMessage(sessionLogEntry));
        break;
    }
  }

  @Override
  public void throwing(Throwable throwable) {
    getLogger().error(null, throwable);
  }

  @Override
  public boolean shouldLog(int level, String category) {
    return getJavaLogLevel(level).intValue() >= logLevel.intValue();
  }


// ----- accessors ---------------------------------------------------------

  /**
   * Get the log level.
   *
   * @return the log level
   */
  public Level getLogLevel() {
    return logLevel;
  }

  /**
   * Set the log level.
   *
   * @param logLevel the log level
   */
  public void setLogLevel(Level logLevel) {
    this.logLevel = logLevel;
  }

  /**
   * Get the associated logger.
   *
   * @return the logger
   */
  protected org.slf4j.Logger getLogger() {
    return LOG;
  }

  /**
   * Get the associated formatter.
   *
   * @return the formatter
   */
  protected Formatter getFormatter() {
    return formatter;
  }


  // ----- helper methods ----------------------------------------------------

  // gets the log message from the given session log entry
  private String getLogMessage(SessionLogEntry sessionLogEntry) {
    return getFormatter().format(getLogRecord(sessionLogEntry,
        getJavaLogLevel(sessionLogEntry.getLevel())));
  }

  // get a log record for the given session log entry
  private EclipseLinkLogRecord getLogRecord(SessionLogEntry sessionLogEntry, Level level) {
    EclipseLinkLogRecord logRecord =
        new EclipseLinkLogRecord(level, formatMessage(sessionLogEntry));

    logRecord.setLoggerName(sessionLogEntry.getNameSpace());
    logRecord.setShouldPrintDate(shouldPrintDate());
    logRecord.setShouldPrintThread(shouldPrintThread());

    Throwable exception = sessionLogEntry.getException();
    if (exception != null) {
      logRecord.setThrown(exception);
      logRecord.setShouldLogExceptionStackTrace(shouldLogExceptionStackTrace());
    }

    if (shouldPrintConnection()) {
      logRecord.setConnection(sessionLogEntry.getConnection());
    }

    if (shouldPrintSession()) {
      logRecord.setSessionString(getSessionString(sessionLogEntry.getSession()));
    }

    return logRecord;
  }

  // get the Java log level for the given eclipse log level
  private static Level getJavaLogLevel(int level) {
    return level >= ALL && level <= OFF ? JAVA_LOG_LEVELS[level] : Level.OFF;
  }
}
