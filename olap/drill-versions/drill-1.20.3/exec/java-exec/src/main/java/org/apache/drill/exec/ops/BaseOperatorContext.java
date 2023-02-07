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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.hadoop.conf.Configuration;

import io.netty.buffer.DrillBuf;

/**
 * Implementation of {@link OperatorContext} that provides services
 * needed by most run-time operators. Excludes services that need the
 * entire Drillbit. This class provides services common to the test-time
 * version of the operator context and the full production-time context
 * that includes network services.
 */
public abstract class BaseOperatorContext implements OperatorContext {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BaseOperatorContext.class);

  protected final FragmentContext context;
  protected final BufferAllocator allocator;
  protected final PhysicalOperator popConfig;
  protected final BufferManager manager;
  private List<DrillFileSystem> fileSystems;
  private ControlsInjector injector;
  private boolean allowCreatingFileSystem = true;

  public BaseOperatorContext(FragmentContext context, BufferAllocator allocator,
               PhysicalOperator popConfig) {
    this.context = context;
    this.allocator = allocator;
    this.popConfig = popConfig;
    this.manager = new BufferManagerImpl(allocator);
    this.fileSystems = new ArrayList<>();
  }

  @Override
  public FragmentContext getFragmentContext() {
    return context;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends PhysicalOperator> T getOperatorDefn() {
    return (T) popConfig;
  }

  public String getName() {
    return popConfig.getClass().getName();
  }

  @Override
  public DrillBuf replace(DrillBuf old, int newSize) {
    return manager.replace(old, newSize);
  }

  @Override
  public DrillBuf getManagedBuffer() {
    return manager.getManagedBuffer();
  }

  @Override
  public DrillBuf getManagedBuffer(int size) {
    return manager.getManagedBuffer(size);
  }

  @Override
  public ExecutionControls getExecutionControls() {
    return context.getExecutionControls();
  }

  @Override
  public BufferAllocator getAllocator() {
    if (allocator == null) {
      throw new UnsupportedOperationException("Operator context does not have an allocator");
    }
    return allocator;
  }

  // Allow an operator to use the thread pool
  @Override
  public ExecutorService getExecutor() {
    return context.getExecutor();
  }

  @Override
  public ExecutorService getScanExecutor() {
    return context.getScanExecutor();
  }

  @Override
  public ExecutorService getScanDecodeExecutor() {
    return context.getScanDecodeExecutor();
  }

  @Override
  public void setInjector(ControlsInjector injector) {
    this.injector = injector;
  }

  @Override
  public ControlsInjector getInjector() {
    return injector;
  }

  @Override
  public void injectUnchecked(String desc) {
    ExecutionControls executionControls = context.getExecutionControls();
    if (injector != null  &&  executionControls != null) {
      injector.injectUnchecked(executionControls, desc);
    }
  }

  @Override
  public <T extends Throwable> void injectChecked(String desc, Class<T> exceptionClass)
      throws T {
    ExecutionControls executionControls = context.getExecutionControls();
    if (injector != null  &&  executionControls != null) {
      injector.injectChecked(executionControls, desc, exceptionClass);
    }
  }

  @Override
  public void close() {
    RuntimeException ex = null;
    try {
      manager.close();
    } catch (RuntimeException e) {
      ex = e;
    }
    try {
      if (allocator != null) {
        allocator.close();
      }
    } catch (RuntimeException e) {
      ex = ex == null ? e : ex;
    }

    for (DrillFileSystem fs : fileSystems) {
      try {
        fs.close();
      } catch (IOException e) {
      throw UserException.resourceError(e)
          .addContext("Failed to close the Drill file system for " + getName())
          .build(logger);
      }
    }

    if (ex != null) {
      throw ex;
    }
  }

  /**
   * Creates DrillFileSystem that automatically tracks operator stats.
   * Only one tracking and no non-tracking file system per operator context.
   */
  @Override
  public DrillFileSystem newFileSystem(Configuration conf) throws IOException {
    Preconditions.checkState(allowCreatingFileSystem, "Only one tracking file system is allowed per Operator Context and it is already created.");
    Preconditions.checkState(fileSystems.isEmpty(), "Non-tracking file system(-s) is(are) already created.");
    DrillFileSystem fs = new DrillFileSystem(conf, getStats());
    fileSystems.add(fs);
    allowCreatingFileSystem = false;
    return fs;
  }

  /**
   * Creates a DrillFileSystem that does not automatically track operator stats.
   * Multiple non-tracking file system are allowed.
   */
  @Override
  public DrillFileSystem newNonTrackingFileSystem(Configuration conf) throws IOException {
    Preconditions.checkState(allowCreatingFileSystem, "Only one tracking file system is allowed per Operator Context and it is already created.");
    DrillFileSystem fs = new DrillFileSystem(conf, null);
    fileSystems.add(fs);
    return fs;
  }

}
