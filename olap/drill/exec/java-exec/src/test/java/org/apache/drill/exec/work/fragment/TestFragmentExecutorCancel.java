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
package org.apache.drill.exec.work.fragment;

import static org.junit.Assert.fail;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.testing.ControlsInjectionUtil;
import org.junit.Test;

/**
 * Run several tpch queries and inject an OutOfMemoryException in ScanBatch that will cause an OUT_OF_MEMORY outcome to
 * be propagated downstream. Make sure the proper "memory error" message is sent to the client.
 */
public class TestFragmentExecutorCancel extends BaseTestQuery {

  @Test
  public void testCancelNonRunningFragments() throws Exception{
    test("alter session set `planner.slice_target` = 10");

    // Inject an out of memory exception in the ScanBatch
    CoordinationProtos.DrillbitEndpoint endpoint = bits[0].getContext().getEndpoint();
    String controlsString = "{\"injections\":[{"
      + "\"address\":\"" + endpoint.getAddress() + "\","
      + "\"port\":\"" + endpoint.getUserPort() + "\","
      + "\"type\":\"exception\","
      + "\"siteClass\":\"" + "org.apache.drill.exec.physical.impl.ScanBatch" + "\","
      + "\"desc\":\"" + "next-allocate" + "\","
      + "\"nSkip\":0,"
      + "\"nFire\":1,"
        + "\"exceptionClass\":\"" + OutOfMemoryException.class.getName() + "\""
      + "}]}";
    ControlsInjectionUtil.setControls(client, controlsString);

    String query = getFile("queries/tpch/04.sql");

    try {
      test(query);
      fail("The query should have failed!!!");
    } catch(UserException uex) {
      // The query should fail
    }

    try {
      closeClient();
    } catch (IllegalStateException ex) {
      fail("failed to close the drillbits properly");
    }
  }
}
