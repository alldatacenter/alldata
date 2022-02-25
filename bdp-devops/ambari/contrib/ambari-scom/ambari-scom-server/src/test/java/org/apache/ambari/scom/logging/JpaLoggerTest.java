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

import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * JpaLogger Tests
 */
public class JpaLoggerTest {
  @Test
  public void testLog() throws Exception {

    Logger logger = createNiceMock(Logger.class);
    SessionLogEntry severeEntry = createNiceMock(SessionLogEntry.class);
    SessionLogEntry configEntry = createNiceMock(SessionLogEntry.class);
    SessionLogEntry finestEntry = createNiceMock(SessionLogEntry.class);
    Formatter formatter = createNiceMock(Formatter.class);

    JpaLogger jpaLogger = new TestJpaLogger(logger, formatter);

    // set expectations

    expect(severeEntry.getLevel()).andReturn(SessionLog.SEVERE);
    expect(formatter.format((LogRecord) anyObject())).andReturn("severe log message");
    logger.error("severe log message");

    expect(configEntry.getLevel()).andReturn(SessionLog.CONFIG);
    expect(formatter.format((LogRecord) anyObject())).andReturn("config log message");
    logger.info("config log message");

    expect(finestEntry.getLevel()).andReturn(SessionLog.FINEST);
    expect(formatter.format((LogRecord) anyObject())).andReturn("finest log message");
    logger.debug("finest log message");

    // replay
    replay(logger, severeEntry, configEntry, finestEntry, formatter);

    jpaLogger.log(severeEntry);
    jpaLogger.log(configEntry);
    jpaLogger.log(finestEntry);

    // verify
    verify(logger, severeEntry, configEntry, finestEntry, formatter);
  }

  @Test
  public void testThrowing() throws Exception {
    Logger logger = createNiceMock(Logger.class);
    Formatter formatter = createNiceMock(Formatter.class);

    Exception exception = new IllegalStateException("Something went wrong!");

    JpaLogger jpaLogger = new TestJpaLogger(logger, formatter);

    // set expectations
    logger.error(null, exception);

    // replay
    replay(logger, formatter);

    jpaLogger.throwing(exception);

    // verify
    verify(logger, formatter);
  }

  @Test
  public void testShouldLog() throws Exception {
    JpaLogger logger = new JpaLogger();
    Assert.assertTrue(logger.shouldLog(SessionLog.SEVERE, ""));
    Assert.assertTrue(logger.shouldLog(SessionLog.WARNING, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.CONFIG, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.FINER, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.ALL, ""));

    logger.setLogLevel(Level.FINER);
    Assert.assertTrue(logger.shouldLog(SessionLog.SEVERE, ""));
    Assert.assertTrue(logger.shouldLog(SessionLog.WARNING, ""));
    Assert.assertTrue(logger.shouldLog(SessionLog.CONFIG, ""));
    Assert.assertTrue(logger.shouldLog(SessionLog.FINER, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.ALL, ""));

    logger.setLogLevel(Level.SEVERE);
    Assert.assertTrue(logger.shouldLog(SessionLog.SEVERE, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.WARNING, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.CONFIG, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.FINER, ""));
    Assert.assertFalse(logger.shouldLog(SessionLog.ALL, ""));
  }

  @Test
  public void testGetSetLogLevel() throws Exception {
    JpaLogger logger = new JpaLogger();
    Assert.assertEquals(Level.WARNING, logger.getLogLevel());

    logger.setLogLevel(Level.ALL);
    Assert.assertEquals(Level.ALL, logger.getLogLevel());

    logger.setLogLevel(Level.FINER);
    Assert.assertEquals(Level.FINER, logger.getLogLevel());

    logger.setLogLevel(Level.OFF);
    Assert.assertEquals(Level.OFF, logger.getLogLevel());
  }


  private static class TestJpaLogger extends JpaLogger {

    private final Logger logger;
    private final Formatter formatter;

    private TestJpaLogger(Logger logger, Formatter formatter) {
      this.logger = logger;
      this.formatter = formatter;
    }

    @Override
    protected Logger getLogger() {
      return logger;
    }

    @Override
    protected Formatter getFormatter() {
      return formatter;
    }

    @Override
    protected String formatMessage(SessionLogEntry entry) {
      return "message";
    }
  }
}
