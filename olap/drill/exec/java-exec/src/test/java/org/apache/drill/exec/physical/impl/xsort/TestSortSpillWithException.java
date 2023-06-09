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
package org.apache.drill.exec.physical.impl.xsort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.exec.testing.Controls;
import org.apache.drill.exec.testing.ControlsInjectionUtil;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Testing External Sort's spilling to disk.
 * <br>
 * This class changes the following Drill property to force
 * external sort to spill after the 2nd batch:
 * {@link ExecConstants#EXTERNAL_SORT_SPILL_THRESHOLD} = 1
 * <br>
 * {@link ExecConstants#EXTERNAL_SORT_SPILL_GROUP_SIZE} = 1
 */
@Category(OperatorTest.class)
public class TestSortSpillWithException extends ClusterTest {
  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setup() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get("xsort"));

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty(ExecConstants.EXTERNAL_SORT_SPILL_THRESHOLD, 1) // Unmanaged
        .configProperty(ExecConstants.EXTERNAL_SORT_SPILL_GROUP_SIZE, 1) // Unmanaged
        // Using EXTERNAL_SORT_MAX_MEMORY to set low values of memory for SORT instead
        // of lowering MAX_QUERY_MEMORY_PER_NODE_KEY because computation of operator memory
        // cannot go lower than MIN_MEMORY_PER_BUFFERED_OP (the default value of this parameter
        // is 40MB). The 40MB memory is sufficient for this testcase to run sort without spilling.
        .configProperty(ExecConstants.EXTERNAL_SORT_MAX_MEMORY, 10 * 1024 * 1024)
        .sessionOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 60 * 1024 * 1024) // Spill early
        // Prevent the percent-based memory rule from second-guessing the above.
        .sessionOption(ExecConstants.PERCENT_MEMORY_PER_QUERY_KEY, 0.0)
        .maxParallelization(1);
    startCluster(builder);
  }

  @Test
  public void testSpillLeakManaged() throws Exception {
    // inject exception in sort while spilling
    final String controls = Controls.newBuilder()
      .addExceptionOnBit(
          ExternalSortBatch.class,
          ExternalSortBatch.INTERRUPTION_WHILE_SPILLING,
          IOException.class,
          cluster.drillbit().getContext().getEndpoint())
      .build();
    ControlsInjectionUtil.setControls(cluster.client(), controls);
    // run a simple order by query
    try {
      runAndLog("SELECT id_i, name_s250 FROM `mock`.`employee_500K` ORDER BY id_i");
      fail("Query should have failed!");
    } catch (UserRemoteException e) {
      assertEquals(ErrorType.RESOURCE, e.getErrorType());
      assertTrue("Incorrect error message",
        e.getMessage().contains("External Sort encountered an error while spilling to disk"));
    }
  }
}
