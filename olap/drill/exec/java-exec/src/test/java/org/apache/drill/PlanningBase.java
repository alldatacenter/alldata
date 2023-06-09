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
package org.apache.drill;

import java.io.IOException;
import java.net.URL;

import org.apache.calcite.jdbc.DynamicSchema;
import org.apache.drill.exec.alias.AliasRegistryProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import io.netty.buffer.DrillBuf;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.vector.ValueHolderHelper;
import org.apache.drill.test.TestTools;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.planner.sql.DrillSqlWorker;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.QueryOptionManager;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.StoragePluginRegistryImpl;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.apache.drill.exec.testing.ExecutionControls;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.codahale.metrics.MetricRegistry;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.mockito.Matchers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlanningBase extends ExecTest {
  @Rule public final TestRule TIMEOUT = TestTools.getTimeoutRule(10000);

  private final DrillConfig config = DrillConfig.create();

  BufferAllocator allocator = RootAllocatorFactory.newRoot(config);

  protected void testSqlPlanFromFile(String file) throws Exception {
    testSqlPlan(getFile(file));
  }

  protected void testSqlPlan(String sqlCommands) throws Exception {
    final DrillbitContext dbContext = mock(DrillbitContext.class);
    final QueryContext context = mock(QueryContext.class);

    final String[] sqlStrings = sqlCommands.split(";");
    final LocalPersistentStoreProvider provider = new LocalPersistentStoreProvider(config);
    provider.start();
    final ScanResult scanResult = ClassPathScanner.fromPrescan(config);
    final LogicalPlanPersistence logicalPlanPersistence = new LogicalPlanPersistence(config, scanResult);
    final SystemOptionManager systemOptions = new SystemOptionManager(logicalPlanPersistence, provider, config);
    systemOptions.init();
    final UserSession userSession = UserSession.Builder.newBuilder().withOptionManager(systemOptions).build();
    final SessionOptionManager sessionOptions = userSession.getOptions();
    final QueryOptionManager queryOptions = new QueryOptionManager(sessionOptions);
    final ExecutionControls executionControls = new ExecutionControls(queryOptions, DrillbitEndpoint.getDefaultInstance());

    when(dbContext.getMetrics()).thenReturn(new MetricRegistry());
    when(dbContext.getAllocator()).thenReturn(allocator);
    when(dbContext.getConfig()).thenReturn(config);
    when(dbContext.getOptionManager()).thenReturn(systemOptions);
    when(dbContext.getStoreProvider()).thenReturn(provider);
    when(dbContext.getClasspathScan()).thenReturn(scanResult);
    when(dbContext.getLpPersistence()).thenReturn(logicalPlanPersistence);

    final StoragePluginRegistry registry = new StoragePluginRegistryImpl(dbContext);
    registry.init();
    final FunctionImplementationRegistry functionRegistry = new FunctionImplementationRegistry(config);
    final DrillOperatorTable table = new DrillOperatorTable(functionRegistry, systemOptions);
    SchemaConfig schemaConfig = SchemaConfig.newBuilder("foo", context).build();
    SchemaPlus root = DynamicSchema.createRootSchema(registry, schemaConfig, new AliasRegistryProvider(dbContext));

    when(context.getNewDefaultSchema()).thenReturn(root);
    when(context.getLpPersistence()).thenReturn(new LogicalPlanPersistence(config, ClassPathScanner.fromPrescan(config)));
    when(context.getStorage()).thenReturn(registry);
    when(context.getFunctionRegistry()).thenReturn(functionRegistry);
    when(context.getSession()).thenReturn(
        UserSession.Builder.newBuilder().withOptionManager(sessionOptions).setSupportComplexTypes(true).build());
    when(context.getCurrentEndpoint()).thenReturn(DrillbitEndpoint.getDefaultInstance());
    when(context.getActiveEndpoints()).thenReturn(ImmutableList.of(DrillbitEndpoint.getDefaultInstance()));
    when(context.getPlannerSettings()).thenReturn(new PlannerSettings(queryOptions, functionRegistry));
    when(context.getOptions()).thenReturn(queryOptions);
    when(context.getConfig()).thenReturn(config);
    when(context.getDrillOperatorTable()).thenReturn(table);
    when(context.getAllocator()).thenReturn(allocator);
    when(context.getExecutionControls()).thenReturn(executionControls);
    when(context.getLpPersistence()).thenReturn(logicalPlanPersistence);
    // mocks for org.apache.drill.TestTpchPlanning#tpch06 test.
    // With changes for decimal types, subtract udf for decimals is used.
    when(context.getManagedBuffer()).thenReturn(allocator.buffer(4));
    when(context.getConstantValueHolder(eq("0.03"),
        eq(TypeProtos.MinorType.VARDECIMAL),
        Matchers.<Function<DrillBuf, ValueHolder>>any()))
      .thenReturn(ValueHolderHelper.getVarDecimalHolder(allocator.buffer(4), "0.03"));
    when(context.getConstantValueHolder(eq("0.01"),
        eq(TypeProtos.MinorType.VARDECIMAL),
        Matchers.<Function<DrillBuf, ValueHolder>>any()))
      .thenReturn(ValueHolderHelper.getVarDecimalHolder(allocator.buffer(4), "0.01"));
    when(context.getOption(anyString())).thenCallRealMethod();


    for (final String sql : sqlStrings) {
      if (sql.trim().isEmpty()) {
        continue;
      }
      @SuppressWarnings("unused")
      final PhysicalPlan p = DrillSqlWorker.getPlan(context, sql);
    }
  }

  protected static String getFile(String resource) throws IOException {
    final URL url = Resources.getResource(resource);
    if (url == null) {
      throw new IOException(String.format("Unable to find path %s.", resource));
    }
    return Resources.toString(url, Charsets.UTF_8);
  }
}
