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
package org.apache.ambari.server.controller.logging;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.slf4j.Logger;

public class UtilsTest {

  @Test
  public void testLogErrorMsgWaitDefault() throws Exception {
    final String expectedErrorMessage = "This is a test error message!";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    // expect that the the call to the logger is only
    // executed once in this test
    loggerMock.error(expectedErrorMessage);
    expectLastCall().times(1);

    mockSupport.replayAll();

    for (int i = 0; i < 1000; i++) {
      Utils.logErrorMessageWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage);
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgWaitDefaultExceedsMaxCount() throws Exception {
    final String expectedErrorMessage = "This is a test error message that should only repeat once!";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    // expect that the the call to the logger is only
    // executed twice in this test
    loggerMock.error(expectedErrorMessage);
    expectLastCall().times(2);

    mockSupport.replayAll();

    for (int i = 0; i < 2000; i++) {
      Utils.logErrorMessageWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage);
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgWithCustomWaitMax() throws Exception {
    final String expectedErrorMessage = "This is a test error message!";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    // expect that the the call to the logger is only
    // executed once in this test
    loggerMock.error(expectedErrorMessage);
    expectLastCall().times(1);

    mockSupport.replayAll();

    for (int i = 0; i < 5; i++) {
      Utils.logErrorMessageWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, 5);
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgWaitExceedsCustomMaxCount() throws Exception {
    final String expectedErrorMessage = "This is a test error message that should only repeat once!";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    // expect that the the call to the logger is only
    // executed twice in this test
    loggerMock.error(expectedErrorMessage);
    expectLastCall().times(2);

    mockSupport.replayAll();

    for (int i = 0; i < 10; i++) {
      Utils.logErrorMessageWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, 5);
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgAndThrowableWaitDefault() throws Exception {
    final String expectedErrorMessage = "This is a test error message!";
    final Exception expectedException = new Exception("test exception");

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    Capture<Exception> exceptionCaptureOne = EasyMock.newCapture();

    // expect that the the call to the logger is only
    // executed once in this test
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureOne));
    expectLastCall().times(1);

    mockSupport.replayAll();

    for (int i = 0; i < 1000; i++) {
      Utils.logErrorMessageWithThrowableWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, expectedException);
    }

    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureOne.getValue());

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgAndThrowableWaitDefaultExceedsMaxCount() throws Exception {
    final String expectedErrorMessage = "This is a test error message that should only repeat once!";
    final Exception expectedException = new Exception("test exception");

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    Capture<Exception> exceptionCaptureOne = EasyMock.newCapture();
    Capture<Exception> exceptionCaptureTwo = EasyMock.newCapture();

    // expect that the the call to the logger is only
    // executed twice in this test
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureOne));
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureTwo));

    mockSupport.replayAll();

    for (int i = 0; i < 2000; i++) {
      Utils.logErrorMessageWithThrowableWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, expectedException);
    }

    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureOne.getValue());
    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureTwo.getValue());

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgAndThrowableWithCustomWaitMax() throws Exception {
    final String expectedErrorMessage = "This is a test error message!";
    final Exception expectedException = new Exception("test exception");

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    Capture<Exception> exceptionCaptureOne = EasyMock.newCapture();

    // expect that the the call to the logger is only
    // executed once in this test
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureOne));
    expectLastCall().times(1);

    mockSupport.replayAll();

    for (int i = 0; i < 5; i++) {
      Utils.logErrorMessageWithThrowableWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, expectedException, 5);
    }

    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureOne.getValue());

    mockSupport.verifyAll();
  }

  @Test
  public void testLogErrorMsgAndThrowableWaitExceedsCustomMaxCount() throws Exception {
    final String expectedErrorMessage = "This is a test error message that should only repeat once!";
    final Exception expectedException = new Exception("test exception");

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    Logger loggerMock =
      mockSupport.createMock(Logger.class);

    AtomicInteger testAtomicInteger = new AtomicInteger(0);

    Capture<Exception> exceptionCaptureOne = EasyMock.newCapture();
    Capture<Exception> exceptionCaptureTwo = EasyMock.newCapture();

    // expect that the the call to the logger is only
    // executed twice in this test
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureOne));
    loggerMock.error(eq(expectedErrorMessage), capture(exceptionCaptureTwo));

    mockSupport.replayAll();

    for (int i = 0; i < 10; i++) {
      Utils.logErrorMessageWithThrowableWithCounter(loggerMock, testAtomicInteger, expectedErrorMessage, expectedException, 5);
    }

    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureOne.getValue());
    assertSame("Exception passed to Logger should have been the same instance passed into the Utils method",
      expectedException, exceptionCaptureTwo.getValue());

    mockSupport.verifyAll();
  }

}
