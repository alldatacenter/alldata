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

import static org.apache.drill.exec.ExecConstants.DRILLBIT_CONTROL_INJECTIONS;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.rpc.user.UserSession.QueryCountIncrementer;
import org.apache.drill.exec.server.options.SessionOptionManager;

/**
 * Static methods for constructing exception and pause injections for testing purposes.
 */
public class ControlsInjectionUtil {
  /**
   * Constructor. Prevent instantiation of static utility class.
   */
  private ControlsInjectionUtil() {
  }

  private static final QueryCountIncrementer incrementer = new QueryCountIncrementer() {
    @Override
    public void increment(final UserSession session) {
      session.incrementQueryCount(this);
    }
  };

  public static void setSessionOption(final DrillClient drillClient, final String option, final String value) {
    try {
      final List<QueryDataBatch> results = drillClient.runQuery(
        UserBitShared.QueryType.SQL, String.format("ALTER session SET `%s` = %s",
          option, value));
      for (final QueryDataBatch data : results) {
        data.release();
      }
    } catch (final RpcException e) {
      fail("Could not set option: " + e);
    }
  }

  public static void setControls(final DrillClient drillClient, final String controls) {
    validateControlsString(controls);
    setSessionOption(drillClient, DRILLBIT_CONTROL_INJECTIONS, "'" + controls + "'");
  }

  public static void setControls(final UserSession session, final String controls) {
    validateControlsString(controls);

    final SessionOptionManager options = session.getOptions();
    try {
      options.setLocalOption(DRILLBIT_CONTROL_INJECTIONS, controls);
    } catch (final Exception e) {
      fail("Could not set controls options: " + e.getMessage());
    }
    incrementer.increment(session); // to simulate that a query completed
  }

  public static void validateControlsString(final String controls) {
    try {
      ExecutionControls.validateControlsString(controls);
    } catch (final Exception e) {
      fail("Could not validate controls JSON: " + e.getMessage());
    }
  }

  /**
   * Create a single exception injection. Note this format is not directly accepted by the injection mechanism. Use
   * the {@link Controls} to build exceptions.
   */
  public static String createException(final Class<?> siteClass, final String desc, final int nSkip,
                                       final int nFire, final Class<? extends Throwable> exceptionClass) {
    final String siteClassName = siteClass.getName();
    final String exceptionClassName = exceptionClass.getName();
    return "{ \"type\":\"exception\","
      + "\"siteClass\":\"" + siteClassName + "\","
      + "\"desc\":\"" + desc + "\","
      + "\"nSkip\":" + nSkip + ","
      + "\"nFire\":" + nFire + ","
      + "\"exceptionClass\":\"" + exceptionClassName + "\"}";
  }

  /**
   * Create a single exception injection on a specific bit. Note this format is not directly accepted by the injection
   * mechanism. Use the {@link Controls} to build exceptions.
   */
  public static String createExceptionOnBit(final Class<?> siteClass, final String desc, final int nSkip,
                                            final int nFire, final Class<? extends Throwable> exceptionClass,
                                            final DrillbitEndpoint endpoint) {
    final String siteClassName = siteClass.getName();
    final String exceptionClassName = exceptionClass.getName();
    return "{ \"type\":\"exception\","
      + "\"siteClass\":\"" + siteClassName + "\","
      + "\"desc\":\"" + desc + "\","
      + "\"nSkip\":" + nSkip + ","
      + "\"nFire\":" + nFire + ","
      + "\"exceptionClass\":\"" + exceptionClassName + "\","
      + "\"address\":\"" + endpoint.getAddress() + "\","
      + "\"port\":\"" + endpoint.getUserPort() + "\"}";
  }

  /**
   * Create a pause injection. Note this format is not directly accepted by the injection mechanism. Use the
   * {@link Controls} to build exceptions.
   */
  public static String createPause(final Class<?> siteClass, final String desc, final int nSkip) {
    return "{ \"type\" : \"pause\"," +
      "\"siteClass\" : \"" + siteClass.getName() + "\","
      + "\"desc\" : \"" + desc + "\","
      + "\"nSkip\" : " + nSkip + "}";
  }

  /**
   * Create a time-bound pause injection. Note this format is not directly accepted by the injection mechanism. Use the
   * {@link Controls} to build exceptions.
   */
  public static String createTimedPause(final Class<?> siteClass, final String desc, final int nSkip, final long msPause) {
    return "{ \"type\" : \"pause\"," +
      "\"siteClass\" : \"" + siteClass.getName() + "\","
      + "\"desc\" : \"" + desc + "\","
      + "\"nSkip\" : " + nSkip + ","
      + "\"msPause\" : " + msPause + "}";
  }

  /**
   * Create a pause injection on a specific bit. Note this format is not directly accepted by the injection
   * mechanism. Use the {@link Controls} to build exceptions.
   */
  public static String createPauseOnBit(final Class<?> siteClass, final String desc, final int nSkip,
                                        final DrillbitEndpoint endpoint) {
    return "{ \"type\" : \"pause\"," +
      "\"siteClass\" : \"" + siteClass.getName() + "\","
      + "\"desc\" : \"" + desc + "\","
      + "\"nSkip\" : " + nSkip + ","
      + "\"address\":\"" + endpoint.getAddress() + "\","
      + "\"port\":\"" + endpoint.getUserPort() + "\"}";
  }

  /**
   * Create a pause injection on a specific bit. Note this format is not directly accepted by the injection
   * mechanism. Use the {@link Controls} to build exceptions.
   */
  public static String createTimedPauseOnBit(final Class<?> siteClass, final String desc, final int nSkip,
                                        final DrillbitEndpoint endpoint, final long msPause) {
    return "{ \"type\" : \"pause\"," +
      "\"siteClass\" : \"" + siteClass.getName() + "\","
      + "\"desc\" : \"" + desc + "\","
      + "\"nSkip\" : " + nSkip + ","
      + "\"msPause\" : " + msPause + ","
      + "\"address\":\"" + endpoint.getAddress() + "\","
      + "\"port\":\"" + endpoint.getUserPort() + "\"}";
  }

  /**
   * Create a latch injection. Note this format is not directly accepted by the injection mechanism. Use the
   * {@link Controls} to build exceptions.
   */
  public static String createLatch(final Class<?> siteClass, final String desc) {
    return "{ \"type\":\"latch\"," +
      "\"siteClass\":\"" + siteClass.getName() + "\","
      + "\"desc\":\"" + desc + "\"}";
  }

  /**
   * Clears all the controls.
   */
  public static void clearControls(final DrillClient client) {
    setControls(client, ExecutionControls.EMPTY_CONTROLS);
  }
}
