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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ZookeeperHelper;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserProtos.UserProperties;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.junit.Test;

public class TestExceptionInjection extends BaseTestQuery {
  private static final String NO_THROW_FAIL = "Didn't throw expected exception";

  private static final UserSession session = UserSession.Builder.newBuilder()
      .withCredentials(UserBitShared.UserCredentials.newBuilder().setUserName("foo").build())
      .withUserProperties(UserProperties.getDefaultInstance())
      .withOptionManager(bits[0].getContext().getOptionManager())
      .build();

  /**
   * Class whose methods we want to simulate runtime at run-time for testing
   * purposes. The class must have access to QueryId, UserSession and DrillbitEndpoint.
   * For instance, these are accessible from {@link org.apache.drill.exec.ops.QueryContext}.
   */
  private static class DummyClass {
    private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(DummyClass.class);
    private final QueryContext context;

    public DummyClass(final QueryContext context) {
      this.context = context;
    }

    /**
     * Method that injects an unchecked exception with the given site description.
     *
     * @param desc the injection site description
     */
    public void descPassthroughMethod(final String desc) {
      // ... code ...

      // simulated unchecked exception
      injector.injectUnchecked(context.getExecutionControls(), desc);

      // ... code ...
    }

    public final static String THROWS_IOEXCEPTION = "<<throwsIOException>>";

    /**
     * Method that injects an IOException with a site description of THROWS_IOEXCEPTION.
     *
     * @throws IOException
     */
    public void throwsIOException() throws IOException {
      // ... code ...

      // simulated IOException
      injector.injectChecked(context.getExecutionControls(), THROWS_IOEXCEPTION, IOException.class);

      // ... code ...
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void noInjection() throws Exception {
    test("select * from sys.memory");
  }

  @SuppressWarnings("static-method")
  @Test
  public void emptyInjection() throws Exception {
    ControlsInjectionUtil.setControls(session, "{\"injections\":[]}");
    test("select * from sys.memory");
  }

  /**
   * Assert that DummyClass.descPassThroughMethod does indeed throw the expected exception.
   *
   * @param dummyClass         the instance of DummyClass
   * @param exceptionClassName the expected exception
   * @param exceptionDesc      the expected exception site description
   */
  private static void assertPassthroughThrows(
    final DummyClass dummyClass, final String exceptionClassName, final String exceptionDesc) {
    try {
      dummyClass.descPassthroughMethod(exceptionDesc);
      fail(NO_THROW_FAIL);
    } catch (Exception e) {
      assertEquals(exceptionClassName, e.getClass().getName());
      assertEquals(exceptionDesc, e.getMessage());
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void uncheckedInjection() {
    // set exceptions via a string
    final String exceptionDesc = "<<injected from descPassthroughMethod()>>";
    final String exceptionClassName = "java.lang.RuntimeException";
    final String jsonString = "{\"injections\":[{"
      + "\"type\":\"exception\"," +
      "\"siteClass\":\"org.apache.drill.exec.testing.TestExceptionInjection$DummyClass\","
      + "\"desc\":\"" + exceptionDesc + "\","
      + "\"nSkip\":0,"
      + "\"nFire\":1,"
      + "\"exceptionClass\":\"" + exceptionClassName + "\""
      + "}]}";
    ControlsInjectionUtil.setControls(session, jsonString);

    final QueryContext context = new QueryContext(session, bits[0].getContext(), QueryId.getDefaultInstance());

    // test that the exception gets thrown
    final DummyClass dummyClass = new DummyClass(context);
    assertPassthroughThrows(dummyClass, exceptionClassName, exceptionDesc);
    try {
      context.close();
    } catch (Exception e) {
      fail();
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void checkedInjection() {
    // set the injection via the parsing POJOs
    final String controls = Controls.newBuilder()
      .addException(DummyClass.class, DummyClass.THROWS_IOEXCEPTION, IOException.class, 0, 1)
      .build();
    ControlsInjectionUtil.setControls(session, controls);

    final QueryContext context = new QueryContext(session, bits[0].getContext(), QueryId.getDefaultInstance());

    // test that the expected exception (checked) gets thrown
    final DummyClass dummyClass = new DummyClass(context);
    try {
      dummyClass.throwsIOException();
      fail(NO_THROW_FAIL);
    } catch (IOException e) {
      assertEquals(DummyClass.THROWS_IOEXCEPTION, e.getMessage());
    }
    try {
      context.close();
    } catch (Exception e) {
      fail();
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void skipAndLimit() {
    final String passthroughDesc = "<<injected from descPassthrough>>";
    final int nSkip = 7;
    final int nFire = 3;
    final Class<? extends Throwable> exceptionClass = RuntimeException.class;
    final String controls = Controls.newBuilder()
      .addException(DummyClass.class, passthroughDesc, exceptionClass, nSkip, nFire)
      .build();
    ControlsInjectionUtil.setControls(session, controls);

    final QueryContext context = new QueryContext(session, bits[0].getContext(), QueryId.getDefaultInstance());

    final DummyClass dummyClass = new DummyClass(context);

    // these shouldn't throw
    for (int i = 0; i < nSkip; ++i) {
      dummyClass.descPassthroughMethod(passthroughDesc);
    }

    // these should throw
    for (int i = 0; i < nFire; ++i) {
      assertPassthroughThrows(dummyClass, exceptionClass.getName(), passthroughDesc);
    }

    // this shouldn't throw
    dummyClass.descPassthroughMethod(passthroughDesc);
    try {
      context.close();
    } catch (Exception e) {
      fail();
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void injectionOnSpecificBit() {
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
      } catch (DrillbitStartupException e) {
        throw new RuntimeException("Failed to start drillbits.", e);
      }

      final DrillbitContext drillbitContext1 = drillbit1.getContext();
      final DrillbitContext drillbitContext2 = drillbit2.getContext();

      final UserSession session = UserSession.Builder.newBuilder()
          .withCredentials(UserBitShared.UserCredentials.newBuilder().setUserName("foo").build())
          .withUserProperties(UserProperties.getDefaultInstance())
          .withOptionManager(drillbitContext1.getOptionManager())
          .build();

      final String passthroughDesc = "<<injected from descPassthrough>>";
      final int nSkip = 7;
      final int nFire = 3;
      final Class<? extends Throwable> exceptionClass = RuntimeException.class;
      // only drillbit1's (address, port)
      final String controls = Controls.newBuilder()
      .addExceptionOnBit(DummyClass.class, passthroughDesc, exceptionClass, drillbitContext1.getEndpoint(), nSkip, nFire)
        .build();

      ControlsInjectionUtil.setControls(session, controls);

      {
        final QueryContext queryContext1 = new QueryContext(session, drillbitContext1, QueryId.getDefaultInstance());
        final DummyClass class1 = new DummyClass(queryContext1);

        // these shouldn't throw
        for (int i = 0; i < nSkip; ++i) {
          class1.descPassthroughMethod(passthroughDesc);
        }

        // these should throw
        for (int i = 0; i < nFire; ++i) {
          assertPassthroughThrows(class1, exceptionClass.getName(), passthroughDesc);
        }

        // this shouldn't throw
        class1.descPassthroughMethod(passthroughDesc);
        try {
          queryContext1.close();
        } catch (Exception e) {
          fail();
        }
      }
      {
        final QueryContext queryContext2 = new QueryContext(session, drillbitContext2, QueryId.getDefaultInstance());
        final DummyClass class2 = new DummyClass(queryContext2);

        // these shouldn't throw
        for (int i = 0; i < nSkip; ++i) {
          class2.descPassthroughMethod(passthroughDesc);
        }

        // these shouldn't throw
        for (int i = 0; i < nFire; ++i) {
          class2.descPassthroughMethod(passthroughDesc);
        }

        // this shouldn't throw
        class2.descPassthroughMethod(passthroughDesc);
        try {
          queryContext2.close();
        } catch (Exception e) {
          fail();
        }
      }
    } finally {
      zkHelper.stopZookeeper();
    }
  }
}
