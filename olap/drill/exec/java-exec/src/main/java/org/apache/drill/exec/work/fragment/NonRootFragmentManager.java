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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;

/**
 * This managers determines when to run a non-root fragment node.
 */
public class NonRootFragmentManager extends AbstractFragmentManager {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NonRootFragmentManager.class);

  private volatile boolean runnerRetrieved = false;

  public NonRootFragmentManager(final PlanFragment fragment, final FragmentExecutor fragmentExecutor,
                                final FragmentStatusReporter statusReporter) {
    super(fragment, fragmentExecutor, statusReporter);
  }

  /* (non-Javadoc)
   * @see org.apache.drill.exec.work.fragment.FragmentHandler#getRunnable()
   */
  @Override
  public FragmentExecutor getRunnable() {
    synchronized(this) {

      // historically, we had issues where we tried to run the same fragment multiple times. Let's check to make sure
      // this isn't happening.
      Preconditions.checkArgument(!runnerRetrieved, "Get Runnable can only be run once.");

      if (cancel) {
        return null;
      }
      runnerRetrieved = true;
      return super.getRunnable();
    }
  }

  @Override
  public void receivingFragmentFinished(final FragmentHandle handle) {
    fragmentExecutor.receivingFragmentFinished(handle);
  }
}
