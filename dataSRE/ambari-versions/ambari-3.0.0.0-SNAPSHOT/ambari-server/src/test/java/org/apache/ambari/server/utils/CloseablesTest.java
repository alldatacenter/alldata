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
package org.apache.ambari.server.utils;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;

public class CloseablesTest {

  @Test
  public void silentIfSucceeds() throws Exception {
    Closeable normalCloseable = createNiceMock(Closeable.class);
    normalCloseable.close();
    replayAll();

    Closeables.closeSilently(normalCloseable);

    verifyAll();
  }

  @Test
  public void silentIfThrows() throws Exception {
    Closeable throwingCloseable = createNiceMock(Closeable.class);
    throwingCloseable.close();
    expectLastCall().andThrow(new IOException());
    replayAll();

    Closeables.closeSilently(throwingCloseable);

    verifyAll();
  }

  @Test
  public void succeedsWithoutLog() throws Exception {
    Closeable normalCloseable = createNiceMock(Closeable.class);
    Logger logger = createStrictMock(Logger.class);
    normalCloseable.close();
    replayAll();

    Closeables.closeLoggingExceptions(normalCloseable, logger);

    verifyAll();
  }

  @Test
  public void warnWithThrownException() throws Exception {
    Closeable throwingCloseable = createNiceMock(Closeable.class);
    Logger logger = createNiceMock(Logger.class);
    IOException e = new IOException();
    throwingCloseable.close();
    expectLastCall().andThrow(e);
    logger.warn(anyString(), eq(e));
    replayAll();

    Closeables.closeLoggingExceptions(throwingCloseable, logger);

    verifyAll();
  }

  @Test
  public void ignoresNullCloseable() {
    Closeables.closeSilently(null);
  }

}
