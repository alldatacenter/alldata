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
package org.apache.drill.test;

import org.apache.drill.exec.alias.AliasRegistryProvider;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import io.netty.buffer.DrillBuf;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.ClassBuilder;
import org.apache.drill.exec.compile.CodeCompiler;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.ops.BaseFragmentContext;
import org.apache.drill.exec.ops.BaseOperatorContext;
import org.apache.drill.exec.ops.BufferManager;
import org.apache.drill.exec.ops.BufferManagerImpl;
import org.apache.drill.exec.ops.ContextInformation;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OpProfileDef;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.PartitionExplorer;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.work.filter.RuntimeFilterWritable;
import org.apache.drill.test.ClusterFixtureBuilder.RuntimeOption;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.HyperRowSetImpl;
import org.apache.drill.exec.physical.rowSet.IndirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import java.util.concurrent.TimeUnit;

/**
 * Test fixture for operator and (especially) "sub-operator" tests.
 * These are tests that are done without the full Drillbit server.
 * Instead, this fixture creates a test fixture runtime environment
 * that provides "real" implementations of the classes required by
 * operator internals, but with implementations tuned to the test
 * environment. The services available from this fixture are:
 * <ul>
 * <li>Configuration (DrillConfig)</li>
 * <li>Memory allocator</li>
 * <li>Code generation (compilers, code cache, etc.)</li>
 * <li>Read-only version of system and session options (which
 * are set when creating the fixture.</li>
 * <li>Write-only version of operator stats (which are easy to
 * read to verify in tests.</li>
 * </ul>
 * What is <b>not</b> provided is anything that depends on a live server:
 * <ul>
 * <li>Network endpoints.</li>
 * <li>Persistent storage.</li>
 * <li>ZK access.</li>
 * <li>Multiple threads of execution.</li>
 * </ul>
 */
public class OperatorFixture extends BaseFixture implements AutoCloseable {

  public OperatorContext operatorContext(PhysicalOperator config) {
    return new MockOperatorContext(context, allocator(), config);
  }

  /**
   * Builds an operator fixture based on a set of config options and system/session
   * options.
   */
  public static class Builder
  {
    protected List<RuntimeOption> systemOptions = new ArrayList<>();
    protected ExecutionControls controls;
    private final ConfigBuilder configBuilder = new ConfigBuilder();
    protected ExecutorService scanExecutor;
    protected ExecutorService scanDecoderExecutor;

    public Builder(BaseDirTestWatcher dirTestWatcher)
    {
      // Set defaults for tmp dirs correctly

      if (dirTestWatcher != null) {
        configBuilder.put(ClassBuilder.CODE_DIR_OPTION, dirTestWatcher.getCodegenDir().getAbsolutePath());
        configBuilder.put(ExecConstants.DRILL_TMP_DIR, dirTestWatcher.getTmpDir().getAbsolutePath());
        configBuilder.put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH, dirTestWatcher.getStoreDir().getAbsolutePath());
        configBuilder.put(ExecConstants.SPILL_DIRS, Arrays.asList(dirTestWatcher.getSpillDir().getAbsolutePath()));
        configBuilder.put(ExecConstants.HASHJOIN_SPILL_DIRS, Arrays.asList(dirTestWatcher.getSpillDir().getAbsolutePath()));
        configBuilder.put(ExecConstants.UDF_DIRECTORY_ROOT, dirTestWatcher.getHomeDir().getAbsolutePath());
        configBuilder.put(ExecConstants.UDF_DIRECTORY_FS, FileSystem.DEFAULT_FS);
      }
    }

    public ConfigBuilder configBuilder() {
      return configBuilder;
    }

    public Builder systemOption(String key, Object value) {
      systemOptions.add(new RuntimeOption(key, value));
      return this;
    }

    public Builder setScanExecutor(final ExecutorService scanExecutor)
    {
      this.scanExecutor = Preconditions.checkNotNull(scanExecutor);
      return this;
    }

    public Builder setScanDecoderExecutor(final ExecutorService scanDecoderExecutor)
    {
      this.scanDecoderExecutor = Preconditions.checkNotNull(scanDecoderExecutor);
      return this;
    }

    public OperatorFixture build() {
      return new OperatorFixture(this);
    }
  }

  /**
   * Provide a simplified test-time code generation context that
   * uses the same code generation mechanism as the full Drill, but
   * provide test-specific versions of various other services.
   */
  public static class MockFragmentContext extends BaseFragmentContext {
    private final DrillConfig config;
    private final OptionManager options;
    private final CodeCompiler compiler;
    private final BufferManagerImpl bufferManager;
    private final BufferAllocator allocator;
    private final ExecutorService scanExecutorService;
    private final ExecutorService scanDecodeExecutorService;
    private final List<OperatorContext> contexts = Lists.newLinkedList();


    private final ExecutorState executorState = new OperatorFixture.MockExecutorState();
    private final ExecutionControls controls;

    public MockFragmentContext(final DrillConfig config,
                               final OptionManager options,
                               final BufferAllocator allocator,
                               final ExecutorService scanExecutorService,
                               final ExecutorService scanDecodeExecutorService) {
      super(newFunctionRegistry(config, options));
      this.config = Preconditions.checkNotNull(config);
      this.options = Preconditions.checkNotNull(options);
      this.allocator = Preconditions.checkNotNull(allocator);
      this.scanExecutorService = scanExecutorService;
      this.scanDecodeExecutorService = scanDecodeExecutorService;
      this.controls = new ExecutionControls(options);
      compiler = new CodeCompiler(config, options);
      bufferManager = new BufferManagerImpl(allocator);
    }

    private static FunctionImplementationRegistry newFunctionRegistry(
        DrillConfig config, OptionManager options) {
      ScanResult classpathScan = ClassPathScanner.fromPrescan(config);
      return new FunctionImplementationRegistry(config, classpathScan, options);
    }

    @Override
    public OptionManager getOptions() {
      return options;
    }

    @Override
    public boolean isImpersonationEnabled() {
      return false;
    }

    @Override
    public ExecutionControls getExecutionControls() {
      return controls;
    }

    @Override
    public DrillConfig getConfig() {
      return config;
    }

    @Override
    public ExecutorService getScanDecodeExecutor() {
      return scanDecodeExecutorService;
    }

    @Override
    public ExecutorService getScanExecutor() {
      return scanExecutorService;
    }

    @Override
    public ExecutorService getExecutor() {
      return null;
    }

    @Override
    public ExecutorState getExecutorState() {
      return executorState;
    }

    @Override
    public BufferAllocator getNewChildAllocator(String operatorName, int operatorId,
                                                long initialReservation, long maximumReservation) {
      return allocator.newChildAllocator(
        "op:" + operatorId + ":" + operatorName,
        initialReservation,
        maximumReservation);
    }

    @Override
    public ExecProtos.FragmentHandle getHandle() {
      return ExecProtos.FragmentHandle.newBuilder().build();
    }

    @Override
    public BufferAllocator getAllocator() {
      return allocator;
    }

    @Override
    public OperatorContext newOperatorContext(PhysicalOperator popConfig,
                                              OperatorStats stats) throws OutOfMemoryException {
      BufferAllocator childAllocator = allocator.newChildAllocator(
        "test:" + popConfig.getClass().getSimpleName(),
        popConfig.getInitialAllocation(),
        popConfig.getMaxAllocation()
      );
      OperatorContext context = new MockOperatorContext(this, childAllocator, popConfig);
      contexts.add(context);
      return context;
    }

    @Override
    public OperatorContext newOperatorContext(PhysicalOperator popConfig)
      throws OutOfMemoryException {
      return newOperatorContext(popConfig, null);
    }

    @Override
    public String getQueryUserName() {
      return "fred";
    }
    @Override
    public SchemaPlus getFullRootSchema() {
      return null;
    }

    @Override
    public String getFragIdString() {
      return null;
    }

    @Override
    public CodeCompiler getCompiler() {
      return compiler;
    }

    @Override
    protected BufferManager getBufferManager() {
      return bufferManager;
    }

    @Override
    public void close() {
      for(OperatorContext context : contexts) {
        context.close();
      }
      bufferManager.close();
    }

    @Override
    public void addRuntimeFilter(RuntimeFilterWritable runtimeFilter) {
    }

    @Override
    public RuntimeFilterWritable getRuntimeFilter(long rfIdentifier) {
      return null;
    }

    @Override
    public RuntimeFilterWritable getRuntimeFilter(long rfIdentifier, long maxWaitTime, TimeUnit timeUnit) {
      return null;
    }

    @Override
    public ContextInformation getContextInformation() {
      return null;
    }

    @Override
    public PartitionExplorer getPartitionExplorer() {
      return null;
    }

    @Override
    public ValueHolder getConstantValueHolder(String value, TypeProtos.MinorType type, Function<DrillBuf, ValueHolder> holderInitializer) {
      return null;
    }

    @Override
    public MetastoreRegistry getMetastoreRegistry() {
      return null;
    }

    @Override
    public AliasRegistryProvider getAliasRegistryProvider() {
      return null;
    }

    @Override
    public void requestMemory(RecordBatch requestor) {
      // Does nothing in a mock fragment.
    }
  }

  private final SystemOptionManager options;
  private final MockFragmentContext context;
  private PersistentStoreProvider provider;

  protected OperatorFixture(Builder builder) {
    config = builder.configBuilder().build();
    allocator = RootAllocatorFactory.newRoot(config);
    options = createOptionManager();
    context = new MockFragmentContext(config, options, allocator, builder.scanExecutor, builder.scanDecoderExecutor);
    applySystemOptions(builder.systemOptions);
  }

  private void applySystemOptions(List<RuntimeOption> systemOptions) {
    for (RuntimeOption option : systemOptions) {
      options.setLocalOption(option.key, option.value);
    }
  }

  public OptionManager getOptionManager() {
    return options;
  }

  private SystemOptionManager createOptionManager() {
    try {
      provider = new LocalPersistentStoreProvider(config);
      provider.start();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    final SystemOptionManager options = new SystemOptionManager(PhysicalPlanReaderTestFactory.defaultLogicalPlanPersistence(config), provider, config);

    try {
      options.init();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    return options;
  }

  public FragmentContext getFragmentContext() { return context; }

  @Override
  public void close() throws Exception {
    provider.close();
    context.close();
    allocator.close();
    options.close();
  }

  public static Builder builder(BaseDirTestWatcher dirTestWatcher) {
    Builder builder = new Builder(dirTestWatcher);
    builder.configBuilder()
      // Required to avoid Dynamic UDF calls for missing or
      // ambiguous functions.
      .put(ExecConstants.UDF_DISABLE_DYNAMIC, true);
    return builder;
  }

  public static OperatorFixture standardFixture(BaseDirTestWatcher dirTestWatcher) {
    return builder(dirTestWatcher).build();
  }

  public RowSetBuilder rowSetBuilder(BatchSchema schema) {
    return rowSetBuilder(MetadataUtils.fromFields(schema));
  }

  public RowSetBuilder rowSetBuilder(TupleMetadata schema) {
    return new RowSetBuilder(allocator, schema);
  }

  public ExtendableRowSet rowSet(BatchSchema schema) {
    return DirectRowSet.fromSchema(allocator, schema);
  }

  public ExtendableRowSet rowSet(TupleMetadata schema) {
    return DirectRowSet.fromSchema(allocator, schema);
  }

  public RowSet wrap(VectorContainer container) {
    switch (container.getSchema().getSelectionVectorMode()) {
    case FOUR_BYTE:
      return HyperRowSetImpl.fromContainer(container, container.getSelectionVector4());
    case NONE:
      return DirectRowSet.fromContainer(container);
    case TWO_BYTE:
      return IndirectRowSet.fromSv2(container, container.getSelectionVector2());
    default:
      throw new IllegalStateException( "Unexpected selection mode" );
    }
  }

  public static class MockOperatorContext extends BaseOperatorContext {
    private final OperatorStats operatorStats;

    public MockOperatorContext(FragmentContext fragContext,
                               BufferAllocator allocator,
                               PhysicalOperator config) {
      super(fragContext, allocator, config);
      this.operatorStats = new OperatorStats(new OpProfileDef(0, "", 100), allocator);
    }

    @Override
    public OperatorStats getStats() {
      return operatorStats;
    }

    @Override
    public <RESULT> ListenableFuture<RESULT> runCallableAs(
        UserGroupInformation proxyUgi, Callable<RESULT> callable) {
      throw new UnsupportedOperationException("Not yet");
    }
  }

  public static class MockExecutorState
      implements FragmentContext.ExecutorState {
    @Override
    public boolean shouldContinue() { return true; }

    @Override
    public void fail(Throwable t) { }

    @Override
    public boolean isFailed() { return false; }

    @Override
    public Throwable getFailureCause() { return null; }

    @Override
    public void checkContinue() { }
  }

  public OperatorContext newOperatorContext(PhysicalOperator popConfig) {
    BufferAllocator childAllocator = allocator.newChildAllocator(
      "test:" + popConfig.getClass().getSimpleName(),
      popConfig.getInitialAllocation(),
      popConfig.getMaxAllocation()
    );

    return new MockOperatorContext(context, childAllocator, popConfig);
  }

  public RowSet wrap(VectorContainer container, SelectionVector2 sv2) {
    if (sv2 == null) {
      assert container.getSchema().getSelectionVectorMode() == SelectionVectorMode.NONE;
      return DirectRowSet.fromContainer(container);
    } else {
      assert container.getSchema().getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE;
      return IndirectRowSet.fromSv2(container, sv2);
    }
  }
}
