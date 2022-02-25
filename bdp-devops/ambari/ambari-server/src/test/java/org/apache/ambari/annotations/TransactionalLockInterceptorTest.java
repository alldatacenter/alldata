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
package org.apache.ambari.annotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.TransactionalLocks;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;

/**
 * Tests {@link TransactionalLock} and associated classes.
 */
public class TransactionalLockInterceptorTest {

  private Injector m_injector;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(
        Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(m_injector);
  }

  /**
   * Tests that the {@link Transactional} and {@link TransactionalLock}
   * annotations cause the interceptors to lock the right area.
   *
   * @throws Throwable
   */
  @Test
  public void testTransactionalLockInvocation() throws Throwable {
    // create mocks
    TransactionalLocks transactionalLocks = m_injector.getInstance(TransactionalLocks.class);
    ReadWriteLock readWriteLock = EasyMock.createStrictMock(ReadWriteLock.class);
    Lock readLock = EasyMock.createStrictMock(Lock.class);
    Lock writeLock = EasyMock.createStrictMock(Lock.class);

    // expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // replay
    EasyMock.replay(transactionalLocks, readWriteLock, readLock, writeLock);

    // invoke method with annotations
    HostRoleCommandDAO hostRoleCommandDAO = m_injector.getInstance(HostRoleCommandDAO.class);
    hostRoleCommandDAO.mergeAll(new ArrayList<>());

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * Tests that a {@link TransactionalLock} called within the constructs of an
   * earlier transaction will still lock.
   *
   * @throws Throwable
   */
  @Test
  public void testNestedTransactional() throws Throwable {
    // create mocks
    TransactionalLocks transactionalLocks = m_injector.getInstance(TransactionalLocks.class);
    ReadWriteLock readWriteLock = EasyMock.createStrictMock(ReadWriteLock.class);
    Lock readLock = EasyMock.createStrictMock(Lock.class);
    Lock writeLock = EasyMock.createStrictMock(Lock.class);

    // expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // replay
    EasyMock.replay(transactionalLocks, readWriteLock, readLock, writeLock);

    // invoke method with annotations
    TestObject testObject = m_injector.getInstance(TestObject.class);
    testObject.testLockMethodAsChildOfActiveTransaction();

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * Tests that a {@link TransactionalLock} called within the constructs of an
   * earlier transaction will still lock.
   *
   * @throws Throwable
   */
  @Test
  public void testMultipleLocks() throws Throwable {
    // create mocks
    TransactionalLocks transactionalLocks = m_injector.getInstance(TransactionalLocks.class);
    ReadWriteLock readWriteLock = EasyMock.createStrictMock(ReadWriteLock.class);
    Lock readLock = EasyMock.createStrictMock(Lock.class);
    Lock writeLock = EasyMock.createStrictMock(Lock.class);

    // expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // another round of expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // replay
    EasyMock.replay(transactionalLocks, readWriteLock, readLock, writeLock);

    // invoke method with annotations
    TestObject testObject = m_injector.getInstance(TestObject.class);
    testObject.testMultipleLocks();

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * Tests that two invocations of a {@link TransactionalLock} with the same
   * {@link TransactionalLock} will only lock once on the {@link LockArea}.
   *
   * @throws Throwable
   */
  @Test
  public void testNestedMultipleLocks() throws Throwable {
    // create mocks
    TransactionalLocks transactionalLocks = m_injector.getInstance(TransactionalLocks.class);
    ReadWriteLock readWriteLock = EasyMock.createStrictMock(ReadWriteLock.class);
    Lock readLock = EasyMock.createStrictMock(Lock.class);
    Lock writeLock = EasyMock.createStrictMock(Lock.class);

    // expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // replay
    EasyMock.replay(transactionalLocks, readWriteLock, readLock, writeLock);

    // invoke method with annotations
    TestObject testObject = m_injector.getInstance(TestObject.class);
    testObject.testMultipleNestedLocks();

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * A test object which has methods annotated for use with this test class.
   */
  public static class TestObject {
    /**
     * Calls:
     * <ul>
     * <li>@Transactional</li>
     * <li>-> @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType
     * = LockType.WRITE)</li>
     * </ul>
     */
    public void testLockMethodAsChildOfActiveTransaction() {
      transactionMethodCallingAnotherWithLock();
    }

    /**
     * Calls:
     * <ul>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * </ul>
     */
    public void testMultipleLocks() {
      transactionMethodWithLock();
      transactionMethodWithLock();
    }

    /**
     * Calls:
     * <ul>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * <li>-> @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType
     * = LockType.WRITE)</li>
     * </ul>
     */
    public void testMultipleNestedLocks() {
      transactionMethodWithLockCallingAnotherWithLock();
    }

    @Transactional
    public void transactionMethodCallingAnotherWithLock() {
      transactionMethodWithLock();
    }

    @Transactional
    @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
    public void transactionMethodWithLock() {
    }


    @Transactional
    @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
    public void transactionMethodWithLockCallingAnotherWithLock() {
      transactionMethodWithLock();
    }
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(TransactionalLocks.class).toInstance(
          EasyMock.createNiceMock(TransactionalLocks.class));
    }
  }
}
