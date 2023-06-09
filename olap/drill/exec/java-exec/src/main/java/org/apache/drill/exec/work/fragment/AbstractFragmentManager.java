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

import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.rpc.data.IncomingDataBatch;
import org.apache.drill.exec.work.batch.IncomingBuffers;

import java.io.IOException;

public abstract class AbstractFragmentManager implements FragmentManager {

  protected final IncomingBuffers buffers;

  protected final FragmentExecutor fragmentExecutor;

  protected final FragmentHandle fragmentHandle;

  protected final ExecutorFragmentContext fragmentContext;

  protected volatile boolean cancel = false;


  public AbstractFragmentManager(final PlanFragment fragment, final FragmentExecutor executor, final FragmentStatusReporter statusReporter, final FragmentRoot rootOperator) {
    this.fragmentHandle = fragment.getHandle();
    this.fragmentContext = executor.getContext();
    this.buffers = new IncomingBuffers(fragment, fragmentContext);
    this.fragmentContext.setBuffers(buffers);
    this.fragmentExecutor = executor;
  }

  public AbstractFragmentManager(final PlanFragment fragment, final FragmentExecutor executor, final FragmentStatusReporter statusReporter) {
    this(fragment, executor, statusReporter, null);
  }

  @Override
  public boolean handle(final IncomingDataBatch batch) throws FragmentSetupException, IOException {
    return buffers.batchArrived(batch);
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  @Override
  public void unpause() {
    fragmentExecutor.unpause();
  }

  @Override
  public FragmentHandle getHandle() {
    return fragmentHandle;
  }

  @Override
  public boolean isWaiting() {
    return !buffers.isDone() && !cancel;
  }

  @Override
  public FragmentContext getFragmentContext() {
    return fragmentContext;
  }

  @Override
  public FragmentExecutor getRunnable() {
    return fragmentExecutor;
  }

  public abstract void receivingFragmentFinished(final FragmentHandle handle);

  @Override
  public void cancel() {
    cancel = true;
    fragmentExecutor.cancel();
  }
}