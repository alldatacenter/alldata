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
package org.apache.ambari.server.alerts;

import static org.apache.ambari.server.state.AlertState.CRITICAL;
import static org.apache.ambari.server.state.AlertState.OK;
import static org.apache.ambari.server.state.AlertState.UNKNOWN;
import static org.apache.ambari.server.state.AlertState.WARNING;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.apache.ambari.server.state.AlertState;
import org.junit.Test;

public class ThresholdTest {
  @Test
  public void testBetweenOkAndWarnIsOk_dirUp() throws Exception {
    Threshold threshold = new Threshold(10.0, 20.0, 30.0);
    assertState(threshold, OK, 10, 15, 19);
  }

  @Test
  public void testBetweenWarnAndCritIsWarn_dirUp() throws Exception {
    Threshold threshold = new Threshold(10.0, 20.0, 30.0);
    assertState(threshold, WARNING, 20, 25, 29);
  }

  @Test
  public void testAboveCritIsCrit_dirUp() throws Exception {
    Threshold threshold = new Threshold(10.0, 20.0, 30.0);
    assertState(threshold, CRITICAL, 30, 40, 99999);
  }

  @Test
  public void testBelowOkIsUnknown_dirUp() throws Exception {
    Threshold threshold = new Threshold(10.0, 20, 30);
    assertState(threshold, UNKNOWN, 9, 2, -99999);
  }

  @Test
  public void testBelowCritIsCrit_dirDown() throws Exception {
    Threshold threshold = new Threshold(40.0, 30.0, 20.0);
    assertState(threshold, CRITICAL, 20, 15, 2, -99999);
  }

  @Test
  public void testBetweenWarnAndCritIsWarn_dirDown() throws Exception {
    Threshold threshold = new Threshold(40.0, 30.0, 20.0);
    assertState(threshold, WARNING, 30, 25, 21);
  }

  @Test
  public void testBetweenOkAndWarnIsOk_dirDown() throws Exception {
    Threshold threshold = new Threshold(40.0, 30.0, 20.0);
    assertState(threshold, OK, 40, 35, 31);
  }

  @Test
  public void testAboveOkIsUnknown_dirDown() throws Exception {
    Threshold threshold = new Threshold(40.0, 30.0, 20.0);
    assertState(threshold, UNKNOWN, 41, 50, 9999);
  }

  @Test
  public void testOkIsOptional() throws Exception {
    Threshold threshold = new Threshold(null, 20.0, 30.0);
    assertState(threshold, OK, 10, 15, 19);
  }

  private void assertState(Threshold threshold, AlertState expectedState, int... values) {
    for (int value: values) {
      assertThat(expectedState, is(threshold.state(value)));
    }
  }
}