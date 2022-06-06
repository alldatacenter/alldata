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

import static org.easymock.EasyMock.expect;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.configuration.Configuration;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

public class LockFactoryTest extends EasyMockSupport {

  @Test
  public void createsRegularLockIfDebugIsDisabled() {
    Configuration config = createNiceMock(Configuration.class);
    expect(config.isServerLocksProfilingEnabled()).andReturn(false);
    replayAll();

    LockFactory factory = new LockFactory(config);
    Lock lock = factory.newLock();
    Assert.assertTrue(lock instanceof ReentrantLock);
    verifyAll();
  }

  @Test
  public void createsRegularReadWriteLockIfDebugIsDisabled() {
    Configuration config = createNiceMock(Configuration.class);
    expect(config.isServerLocksProfilingEnabled()).andReturn(false);
    replayAll();

    LockFactory factory = new LockFactory(config);
    ReadWriteLock lock = factory.newReadWriteLock();
    Assert.assertTrue(lock instanceof ReentrantReadWriteLock);
    verifyAll();
  }

  @Test
  public void createsProfiledLockIfProfilingIsEnabled() {
    Configuration config = createNiceMock(Configuration.class);
    expect(config.isServerLocksProfilingEnabled()).andReturn(true);
    replayAll();

    LockFactory factory = new LockFactory(config);
    Lock lock = factory.newLock();

    Assert.assertTrue(lock instanceof ProfiledReentrantLock);

    String label = ((ProfiledLock) lock).getLabel();
    Assert.assertTrue(label, label.contains("LockFactoryTest.java"));

    verifyAll();
  }

  @Test
  public void createsProfiledReadWriteLockIfProfilingIsEnabled() {
    Configuration config = createNiceMock(Configuration.class);
    expect(config.isServerLocksProfilingEnabled()).andReturn(true);
    replayAll();

    LockFactory factory = new LockFactory(config);
    ReadWriteLock lock = factory.newReadWriteLock();

    Assert.assertTrue(lock instanceof ProfiledReentrantReadWriteLock);

    String readLockLabel = ((ProfiledReentrantReadWriteLock) lock).readLock().getLabel();
    Assert.assertTrue(readLockLabel, readLockLabel.contains("LockFactoryTest.java"));

    String writeLockLabel = ((ProfiledReentrantReadWriteLock) lock).writeLock().getLabel();
    Assert.assertTrue(writeLockLabel, writeLockLabel.contains("LockFactoryTest.java"));

    verifyAll();
  }

}
