/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.testing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.concurrent.ExtendedLatch;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ZookeeperHelper;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import org.apache.drill.exec.proto.UserProtos.UserProperties;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.util.Pointer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPauseInjection extends BaseTestQuery {

  private static final UserSession session = UserSession.Builder.newBuilder()
      .withCredentials(UserCredentials.newBuilder()
        .setUserName("foo")
        .build())
      .withUserProperties(UserProperties.getDefaultInstance())
      .withOptionManager(bits[0].getContext().getOptionManager())
      .build();

  /**
   * Class whose methods we want to simulate pauses at run-time for testing
   * purposes. The class must have access to {@link org.apache.drill.exec.ops.QueryContext} or
   * {@link org.apache.drill.exec.ops.FragmentContextImpl}.
   */
  private static class DummyClass {
    private static final Logger logger = LoggerFactory.getLogger(DummyClass.class);
    private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(DummyClass.class);

    private final QueryContext context;
    private final CountDownLatch latch;

    public DummyClass(final QueryContext context, final CountDownLatch latch) {
      this.context = context;
      this.latch = latch;
    }

    public static final String PAUSES = "<<pauses>>";

    /**
     * Method that pauses.
     *
     * @return how long the method paused in milliseconds
     */
    public long pauses() {
      // ... code ...

      latch.countDown();
      final long startTime = System.currentTimeMillis();
      // simulated pause
      injector.injectPause(context.getExecutionControls(), PAUSES, logger);
      final long endTime = System.currentTimeMillis();

      // ... code ...
      return (endTime - startTime);
    }
  }

  private static class ResumingThread extends Thread {

    private final QueryContext context;
    private final ExtendedLatch latch;
    private final Pointer<Exception> ex;
    private final long millis;

    public ResumingThread(final QueryContext context, final ExtendedLatch latch, final Pointer<Exception> ex,
                          final long millis) {
      this.context = context;
      this.latch = latch;
      this.ex = ex;
      this.millis = millis;
    }

    @Override
    public void run() {
      latch.awaitUninterruptibly();
      try {
        Thread.sleep(millis);
      } catch (final InterruptedException ex) {
        this.ex.value = ex;
      }
      context.getExecutionControls().unpauseAll();
    }
  }

  @Test
  public void pauseInjected() {
    final long expectedDuration = 1000L;
    final ExtendedLatch trigger = new ExtendedLatch(1);
    final Pointer<Exception> ex = new Pointer<>();

    final String controls = Controls.newBuilder()
      .addPause(DummyClass.class, DummyClass.PAUSES)
      .build();

    ControlsInjectionUtil.setControls(session, controls);

    final QueryContext queryContext = new QueryContext(session, bits[0].getContext(), QueryId.getDefaultInstance());

    (new ResumingThread(queryContext, trigger, ex, expectedDuration)).start();

    // test that the pause happens
    final DummyClass dummyClass = new DummyClass(queryContext, trigger);
    final long actualDuration = dummyClass.pauses();
    assertTrue(String.format("Test should stop for at least %d milliseconds.", expectedDuration),
      expectedDuration <= actualDuration);
    assertNull("No exception should be thrown.", ex.value);
    try {
      queryContext.close();
    } catch (final Exception e) {
      fail("Failed to close query context: " + e);
    }
  }

  @Test
  public void timedPauseInjected() {
    final long pauseDuration = 2000L;
    final long expectedDuration = pauseDuration;
    final ExtendedLatch trigger = new ExtendedLatch(1);
    final Pointer<Exception> ex = new Pointer<>();
    final String controls = Controls.newBuilder()
      .addTimedPause(DummyClass.class, DummyClass.PAUSES, 0, pauseDuration)
      .build();

    ControlsInjectionUtil.setControls(session, controls);

    final QueryContext queryContext = new QueryContext(session, bits[0].getContext(), QueryId.getDefaultInstance());
    //We don't need a ResumingThread because this should automatically resume

    // test that the pause happens
    final DummyClass dummyClass = new DummyClass(queryContext, trigger);
    final long actualDuration = dummyClass.pauses();
    assertTrue(String.format("Test should stop for at least %d milliseconds.", expectedDuration),
      expectedDuration <= actualDuration);
    assertNull("No exception should be thrown.", ex.value);
    try {
      queryContext.close();
    } catch (final Exception e) {
      fail("Failed to close query context: " + e);
    }
  }

  @Test
  public void pauseOnSpecificBit() {
    final RemoteServiceSet remoteServiceSet = RemoteServiceSet.getLocalServiceSet();
    final ZookeeperHelper zkHelper = new ZookeeperHelper();
    zkHelper.startZookeeper(1);

    try {
      // Creating two drillbits
      final Drillbit drillbit1, drillbit2;
      final DrillConfig drillConfig = zkHelper.getConfig();
      try {
        drillbit1 = Drillbit.start(drillConfig, remoteServiceSet);
        drillbit2 = Drillbit.start(drillConfig, remoteServiceSet);
      } catch (final DrillbitStartupException e) {
        throw new RuntimeException("Failed to start two drillbits.", e);
      }

      final DrillbitContext drillbitContext1 = drillbit1.getContext();
      final DrillbitContext drillbitContext2 = drillbit2.getContext();

      final UserSession session = UserSession.Builder.newBuilder()
        .withCredentials(UserCredentials.newBuilder()
          .setUserName("foo")
          .build())
        .withUserProperties(UserProperties.getDefaultInstance())
        .withOptionManager(drillbitContext1.getOptionManager())
        .build();

      final DrillbitEndpoint drillbitEndpoint1 = drillbitContext1.getEndpoint();
      final String controls = Controls.newBuilder()
        .addPauseOnBit(DummyClass.class, DummyClass.PAUSES, drillbitEndpoint1)
        .build();

      ControlsInjectionUtil.setControls(session, controls);

      {
        final long expectedDuration = 1000L;
        final ExtendedLatch trigger = new ExtendedLatch(1);
        final Pointer<Exception> ex = new Pointer<>();
        final QueryContext queryContext = new QueryContext(session, drillbitContext1, QueryId.getDefaultInstance());
        (new ResumingThread(queryContext, trigger, ex, expectedDuration)).start();

        // test that the pause happens
        final DummyClass dummyClass = new DummyClass(queryContext, trigger);
        final long actualDuration = dummyClass.pauses();
        assertTrue(String.format("Test should stop for at least %d milliseconds.", expectedDuration), expectedDuration <= actualDuration);
        assertNull("No exception should be thrown.", ex.value);
        try {
          queryContext.close();
        } catch (final Exception e) {
          fail("Failed to close query context: " + e);
        }
      }

      {
        final ExtendedLatch trigger = new ExtendedLatch(1);
        final QueryContext queryContext = new QueryContext(session, drillbitContext2, QueryId.getDefaultInstance());

        // if the resume did not happen, the test would hang
        final DummyClass dummyClass = new DummyClass(queryContext, trigger);
        dummyClass.pauses();
        try {
          queryContext.close();
        } catch (final Exception e) {
          fail("Failed to close query context: " + e);
        }
      }
    } finally {
      zkHelper.stopZookeeper();
    }
  }


  @Test
  public void timedPauseOnSpecificBit() {
    final RemoteServiceSet remoteServiceSet = RemoteServiceSet.getLocalServiceSet();
    final ZookeeperHelper zkHelper = new ZookeeperHelper();
    zkHelper.startZookeeper(1);

    final long pauseDuration = 2000L;
    final long expectedDuration = pauseDuration;

    try {
      // Creating two drillbits
      final Drillbit drillbit1, drillbit2;
      final DrillConfig drillConfig = zkHelper.getConfig();
      try {
        drillbit1 = Drillbit.start(drillConfig, remoteServiceSet);
        drillbit2 = Drillbit.start(drillConfig, remoteServiceSet);
      } catch (final DrillbitStartupException e) {
        throw new RuntimeException("Failed to start two drillbits.", e);
      }

      final DrillbitContext drillbitContext1 = drillbit1.getContext();
      final DrillbitContext drillbitContext2 = drillbit2.getContext();

      final UserSession session = UserSession.Builder.newBuilder()
        .withCredentials(UserCredentials.newBuilder()
          .setUserName("foo")
          .build())
        .withUserProperties(UserProperties.getDefaultInstance())
        .withOptionManager(drillbitContext1.getOptionManager())
        .build();

      final DrillbitEndpoint drillbitEndpoint1 = drillbitContext1.getEndpoint();
      final String controls = Controls.newBuilder()
        .addTimedPauseOnBit(DummyClass.class, DummyClass.PAUSES, drillbitEndpoint1, 0, pauseDuration)
        .build();

      ControlsInjectionUtil.setControls(session, controls);
      {
        final ExtendedLatch trigger = new ExtendedLatch(1);
        final Pointer<Exception> ex = new Pointer<>();
        final QueryContext queryContext = new QueryContext(session, drillbitContext1, QueryId.getDefaultInstance());

        // test that the pause happens
        final DummyClass dummyClass = new DummyClass(queryContext, trigger);
        final long actualDuration = dummyClass.pauses();
        assertTrue(String.format("Test should stop for at least %d milliseconds.", expectedDuration), expectedDuration <= actualDuration);
        assertNull("No exception should be thrown.", ex.value);
        try {
          queryContext.close();
        } catch (final Exception e) {
          fail("Failed to close query context: " + e);
        }
      }

      {
        final ExtendedLatch trigger = new ExtendedLatch(1);
        final QueryContext queryContext = new QueryContext(session, drillbitContext2, QueryId.getDefaultInstance());

        // if the resume did not happen, the test would hang
        final DummyClass dummyClass = new DummyClass(queryContext, trigger);
        dummyClass.pauses();
        try {
          queryContext.close();
        } catch (final Exception e) {
          fail("Failed to close query context: " + e);
        }
      }
    } finally {
      zkHelper.stopZookeeper();
    }
  }
}
