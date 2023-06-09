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
package org.apache.drill.exec;

import com.codahale.metrics.MetricRegistry;
import org.apache.drill.common.parser.LogicalExpressionParser;
import mockit.Mock;
import mockit.MockUp;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.exec.compile.CodeCompilerTestFactory;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.physical.impl.OperatorCreatorRegistry;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.DrillTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecTest extends DrillTest {

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  protected static SystemOptionManager optionManager;

  protected static final DrillConfig c = DrillConfig.create();

  @After
  public void clear(){
    // TODO:  (Re DRILL-1735) Check whether still needed now that
    // BootstrapContext.close() resets the metrics.
    DrillMetrics.resetMetrics();
  }

  @BeforeClass
  public static void setupOptionManager() throws Exception{
    final LocalPersistentStoreProvider provider = new LocalPersistentStoreProvider(c);
    provider.start();
    optionManager = new SystemOptionManager(PhysicalPlanReaderTestFactory.defaultLogicalPlanPersistence(c), provider,c);
    optionManager.init();
  }

  /**
   * Creates instance of local file system.
   *
   * @return local file system
   */
  public static FileSystem getLocalFileSystem() throws IOException {
    Configuration configuration = new Configuration();
    configuration.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    return FileSystem.get(configuration);
  }

  protected DrillbitContext mockDrillbitContext() throws Exception {
    final DrillbitContext context = mock(DrillbitContext.class);

    when(context.getMetrics()).thenReturn(new MetricRegistry());
    when(context.getAllocator()).thenReturn(RootAllocatorFactory.newRoot(c));
    when(context.getOperatorCreatorRegistry()).thenReturn(new OperatorCreatorRegistry(ClassPathScanner.fromPrescan(c)));
    when(context.getConfig()).thenReturn(c);
    when(context.getOptionManager()).thenReturn(optionManager);
    when(context.getCompiler()).thenReturn(CodeCompilerTestFactory.getTestCompiler(c));

    return context;
  }

  public LogicalExpression parseExpr(String expr) {
    return LogicalExpressionParser.parse(expr);
  }

  /**
   * This utility is to mock the method DateTimeUtils.getDateFormatSymbols()
   * to mock the current local as US.
   */
  public static void mockUsDateFormatSymbols() {
    new MockUp<DateTimeUtils>() {
      @Mock
      public DateFormatSymbols getDateFormatSymbols(Locale locale) {
        return new DateFormatSymbols(Locale.US);
      }
    };
  }

  /**
   * This utility is to mock the method DateTimeZone.getDefault() to
   * mock current timezone as UTC.
   */
  public static void mockUtcDateTimeZone() {
    new MockUp<DateTimeZone>() {
      @Mock
      public DateTimeZone getDefault() {
        return DateTimeZone.UTC;
      }
    };
  }
}
