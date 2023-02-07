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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;

import io.netty.buffer.DrillBuf;

/**
 * Per-operator services available for operator implementations.
 * The services allow access to the operator definition, to the
 * fragment context, and to per-operator services.
 * <p>
 * Use this interface in code to allow unit tests to provide
 * test-time implementations of this context.
 */

public interface OperatorContext {

  /**
   * Return the physical operator definition created by the planner and passed
   * into the Drillbit executing the query.
   * @return the physical operator definition
   */

  <T extends PhysicalOperator> T getOperatorDefn();

  /**
   * Return the memory allocator for this operator.
   *
   * @return the per-operator memory allocator
   */

  BufferAllocator getAllocator();

  FragmentContext getFragmentContext();

  DrillBuf replace(DrillBuf old, int newSize);

  DrillBuf getManagedBuffer();

  DrillBuf getManagedBuffer(int size);

  ExecutionControls getExecutionControls();

  /**
   * Drill statistics mechanism. Allows
   * operators to update statistics.
   * @return operator statistics
   */

  OperatorStats getStats();

  ExecutorService getExecutor();

  ExecutorService getScanExecutor();

  ExecutorService getScanDecodeExecutor();

  DrillFileSystem newFileSystem(Configuration conf) throws IOException;

  DrillFileSystem newNonTrackingFileSystem(Configuration conf) throws IOException;

  /**
   * Run the callable as the given proxy user.
   *
   * @param proxyUgi proxy user group information
   * @param callable callable to run
   * @param <RESULT> result type
   * @return Future<RESULT> future with the result of calling the callable
   */
  <RESULT> ListenableFuture<RESULT> runCallableAs(UserGroupInformation proxyUgi,
                                                                  Callable<RESULT> callable);

  void setInjector(ControlsInjector injector);

  /**
   * Returns the fault injection mechanism used to introduce faults at runtime
   * for testing.
   * @return the fault injector
   */

  ControlsInjector getInjector();

  /**
   * Insert an unchecked fault (exception). Handles the details of checking if
   * fault injection is enabled and this particular fault is selected.
   * @param desc the description of the fault used to match a fault
   * injection parameter to determine if the fault should be injected
   * @throws RuntimeException an unchecked exception if the fault is enabled
   */

  void injectUnchecked(String desc);

  /**
   * Insert a checked fault (exception) of the given class. Handles the details
   * of checking if fault injection is enabled and this particular fault is
   * selected.
   *
   * @param desc the description of the fault used to match a fault
   * injection parameter to determine if the fault should be injected
   * @param exceptionClass the class of exeception to be thrown
   * @throws T if the fault is enabled
   */

  <T extends Throwable> void injectChecked(String desc, Class<T> exceptionClass)
      throws T;

  void close();
}
