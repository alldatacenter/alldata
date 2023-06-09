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
package org.apache.drill.exec.client;

import static org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.mockito.Mockito;

/**
 * The unit test case will read a physical plan in json format. The physical plan contains a "trace" operator,
 * which will produce a dump file.  The dump file will be input into DumpCat to test query mode and batch mode.
 */

public class DumpCatTest  extends ExecTest {
  private final DrillConfig c = DrillConfig.create();

  @Test
  public void testDumpCat() throws Throwable
  {
      final DrillbitContext bitContext = mockDrillbitContext();
      final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

      final PhysicalPlanReader reader = defaultPhysicalPlanReader(c);
      final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/trace/simple_trace.json"), Charsets.UTF_8).read());
      final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
      final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
      final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

      while(exec.next()) {
      }

      if(context.getExecutorState().getFailureCause() != null) {
          throw context.getExecutorState().getFailureCause();
      }
      assertTrue(!context.getExecutorState().isFailed());

      exec.close();

      FragmentHandle handle = context.getHandle();

      /* Form the file name to which the trace output will dump the record batches */
      String qid = QueryIdHelper.getQueryId(handle.getQueryId());

      final int majorFragmentId = handle.getMajorFragmentId();
      final int minorFragmentId = handle.getMinorFragmentId();

      final String logLocation = c.getString(ExecConstants.TRACE_DUMP_DIRECTORY);
      final String filename = String.format("%s//%s_%d_%d_mock-scan", logLocation, qid, majorFragmentId, minorFragmentId);

      final Configuration conf = new Configuration();
      conf.set(FileSystem.FS_DEFAULT_NAME_KEY, c.getString(ExecConstants.TRACE_DUMP_FILESYSTEM));

      final FileSystem fs = FileSystem.get(conf);
      final Path path = new Path(filename);
      assertTrue("Trace file does not exist", fs.exists(path));

      final DumpCat dumpCat = new DumpCat();

      //Test Query mode
      try (final FileInputStream input = new FileInputStream(filename)) {
        dumpCat.doQuery(input);
      }

      //Test Batch mode
      try(final FileInputStream input = new FileInputStream(filename)) {
        dumpCat.doBatch(input, 0, true);
      }
  }
}
