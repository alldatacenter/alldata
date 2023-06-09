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
package org.apache.drill.exec.ops;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.alias.AliasRegistryProvider;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.compile.CodeCompiler;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.QueryContext.SqlStatementType;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.work.filter.RuntimeFilterWritable;

import io.netty.buffer.DrillBuf;

/**
 * Provides the resources required by a non-exchange operator to execute.
 */
public interface FragmentContext extends UdfUtilities, AutoCloseable {
  /**
   * Returns the UDF registry.
   * @return the UDF registry
   */
  FunctionLookupContext getFunctionRegistry();

  /**
   * Returns a read-only version of the session options.
   * @return the session options
   */
  OptionManager getOptions();

  boolean isImpersonationEnabled();

  /**
   * Generates code for a class given a {@link ClassGenerator},
   * and returns a single instance of the generated class. (Note
   * that the name is a misnomer, it would be better called
   * <tt>getImplementationInstance</tt>.)
   *
   * @param cg the class generator
   * @return an instance of the generated class
   */
  <T> T getImplementationClass(final ClassGenerator<T> cg);

  /**
   * Generates code for a class given a {@link CodeGenerator},
   * and returns a single instance of the generated class. (Note
   * that the name is a misnomer, it would be better called
   * <tt>getImplementationInstance</tt>.)
   *
   * @param cg the code generator
   * @return an instance of the generated class
   */
  <T> T getImplementationClass(final CodeGenerator<T> cg);

  /**
   * Generates code for a class given a {@link ClassGenerator}, and returns the
   * specified number of instances of the generated class. (Note that the name
   * is a misnomer, it would be better called
   * <tt>getImplementationInstances</tt>.)
   *
   * @param cg the class generator
   * @return list of instances of the generated class
   */
  <T> List<T> getImplementationClass(final ClassGenerator<T> cg, final int instanceCount);

  /**
   * Returns the statement type (e.g. SELECT, CTAS, ANALYZE) from the query context.
   *
   * @return query statement type {@link SqlStatementType}, if known.
   */
  SqlStatementType getSQLStatementType();

  /**
   * Get this node's identity.
   * @return A DrillbitEndpoint object.
   */
  <T> List<T> getImplementationClass(final CodeGenerator<T> cg, final int instanceCount);

  /**
   * Return the set of execution controls used to inject faults into running
   * code for testing.
   *
   * @return the execution controls
   */
  ExecutionControls getExecutionControls();

  /**
   * Returns the Drill configuration for this run. Note that the config is
   * global and immutable.
   *
   * @return the Drill configuration
   */
  DrillConfig getConfig();

  CodeCompiler getCompiler();

  ExecutorService getScanDecodeExecutor();

  ExecutorService getScanExecutor();

  ExecutorService getExecutor();

  ExecutorState getExecutorState();

  BufferAllocator getNewChildAllocator(final String operatorName,
                                       final int operatorId,
                                       final long initialReservation,
                                       final long maximumReservation);

  ExecProtos.FragmentHandle getHandle();

  BufferAllocator getAllocator();

  /**
   * @return ID {@link java.util.UUID} of the current query
   */
  QueryId getQueryId();

  /**
   * @return The string representation of the ID {@link java.util.UUID} of the current query
   */
  String getQueryIdString();

  OperatorContext newOperatorContext(PhysicalOperator popConfig);

  OperatorContext newOperatorContext(PhysicalOperator popConfig, OperatorStats stats);

  SchemaPlus getFullRootSchema();

  String getQueryUserName();

  String getFragIdString();

  DrillBuf replace(DrillBuf old, int newSize);

  @Override
  DrillBuf getManagedBuffer();

  DrillBuf getManagedBuffer(int size);

  BufferManager getManagedBufferManager();

  @Override
  void close();

  /**
   * Add a RuntimeFilter when the RuntimeFilter receiver belongs to the same MinorFragment.
   *
   * @param runtimeFilter runtime filter
   */
  void addRuntimeFilter(RuntimeFilterWritable runtimeFilter);

  RuntimeFilterWritable getRuntimeFilter(long rfIdentifier);

  /**
   * Get the RuntimeFilter with a blocking wait, if the waiting option is enabled.
   *
   * @param rfIdentifier runtime filter identifier
   * @param maxWaitTime max wait time
   * @param timeUnit time unit
   * @return the RFW or null
   */
  RuntimeFilterWritable getRuntimeFilter(long rfIdentifier, long maxWaitTime, TimeUnit timeUnit);

  /**
   * Get instance of Metastore registry to obtain Metastore instance if needed.
   *
   * @return Metastore registry
   */
  MetastoreRegistry getMetastoreRegistry();

  /**
   * Get an instance of alias registry provider for obtaining aliases.
   *
   * @return alias registry provider
   */
  AliasRegistryProvider getAliasRegistryProvider();

  /**
   * An operator is experiencing memory pressure. Asks the fragment
   * executor to poll all operators to release an optional memory
   * (such as by spilling.) The request is advisory. The caller should
   * again try to allocate memory, and if the second request fails,
   * throw an <code>OutOfMemoryException</code>.
   */
  void requestMemory(RecordBatch requestor);

  interface ExecutorState {
    /**
     * Tells individual operations whether they should continue. In some cases,
     * an external event (typically cancellation) will mean that the fragment
     * should prematurely exit execution. Long running operations should check
     * this every so often so that Drill is responsive to cancellation
     * operations.
     *
     * @return False if the action should terminate immediately, true if
     *         everything is okay.
     */
    boolean shouldContinue();

    /**
     * Check if an operation should continue. In some cases,
     * an external event (typically cancellation) will mean that the fragment
     * should prematurely exit execution. Long running operations should check
     * this every so often so that Drill is responsive to cancellation
     * operations.
     * <p>
     * Throws QueryCancelledException if the query (fragment) should stop.
     * The fragment executor interprets this as an exception it, itself,
     * requested, and will call the operator's close() method to release
     * resources. Operators should not catch and handle this exception,
     * and should only call this method when the operator holds no
     * transient resources (such as local variables.)
     *
     * @throws QueryCancelledException if the query (fragment) should stop.
     */
    void checkContinue();

    /**
     * Inform the executor if a exception occurs and fragment should be failed.
     *
     * @param t
     *          The exception that occurred.
     */
    void fail(final Throwable t);

    @VisibleForTesting
    @Deprecated
    boolean isFailed();

    @VisibleForTesting
    @Deprecated
    Throwable getFailureCause();
  }
}
