/*
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
package org.apache.ambari.server.logging;

import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EclipseLinkLogger} is a bridge between EclipseLink's internal
 * logging framework and SLF4J. EclipseLink does not have a rolling log, which
 * means that the standard output file can grow large and cause disk space
 * issues. This logger translates the following log levels:
 * <table>
 * <tr>
 * <td>EclipseLink</td>
 * <td>SLF4J</td>
 * </tr>
 * <tr>
 * <td>ALL</td>
 * <td>TRACE</td>
 * </tr>
 * <tr>
 * <td>FINER</td>
 * <td>TRACE</td>
 * </tr>
 * <tr>
 * <td>FINEST</td>
 * <td>TRACE</td>
 * </tr>
 * <tr>
 * <td>FINE</td>
 * <td>DEBUG</td>
 * </tr>
 * <tr>
 * <td>INFO</td>
 * <td>INFO</td>
 * </tr>
 * <tr>
 * <td>CONFIG</td>
 * <td>INFO</td>
 * </tr>
 * <tr>
 * <td>SEVERE</td>
 * <td>ERROR</td>
 * </tr>
 * <tr>
 * <td>WARNING</td>
 * <td>WARN</td>
 * </tr>
 * <tr>
 * <td>OFF</td>
 * <td>-</td>
 * </tr>
 * </table>
 * <p/>
 * Logging is still configured for EclipseLink via {@code persistence.xml}.
 */
public class EclipseLinkLogger extends AbstractSessionLog implements SessionLog {

  /**
   * A logger that is only for creating a bridge between EclipseLink's internal
   * logger and log4j.
   */
  private static final Logger JPA_LOG = LoggerFactory.getLogger("eclipselink");

  /**
   * The log template to use to create statements similar to EclipseLink's
   * internal logger.
   */
  private static final String LOG_TEMPLATE = "[EL {}]: {} {}";

  @Override
  public void log(SessionLogEntry sessionLogEntry) {
    // use the EclipseLink log level to determine if this should be logged
    int level = sessionLogEntry.getLevel();
    if (!shouldLog(level, sessionLogEntry.getNameSpace())) {
      return;
    }

    switch (level) {
      case SessionLog.ALL:
      case SessionLog.FINER:
      case SessionLog.FINEST:
        JPA_LOG.trace(LOG_TEMPLATE, "Trace", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
      case SessionLog.INFO:
      case SessionLog.CONFIG:
        JPA_LOG.info(LOG_TEMPLATE, "Info", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
      case SessionLog.FINE:
        JPA_LOG.debug(LOG_TEMPLATE, "Debug", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
      case SessionLog.SEVERE:
        // always log
        JPA_LOG.error(LOG_TEMPLATE, "Error", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
      case SessionLog.WARNING:
        JPA_LOG.warn(LOG_TEMPLATE, "Warning", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
      case SessionLog.OFF:
        // never log
        return;
      default:
        JPA_LOG.debug(LOG_TEMPLATE, "Unknown", getSupplementDetailString(sessionLogEntry),
            formatMessage(sessionLogEntry));
        return;
    }
  }
}
