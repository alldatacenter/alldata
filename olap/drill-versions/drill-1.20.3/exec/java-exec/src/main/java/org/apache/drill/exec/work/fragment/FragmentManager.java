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

import java.io.IOException;

import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.rpc.data.IncomingDataBatch;

/**
 * The Fragment Manager is responsible managing incoming data and executing a fragment. Once enough data and resources
 * are avialable, a fragment manager will start a fragment executor to run the associated fragment.
 */
public interface FragmentManager {
  /**
   * Handle the next incoming record batch.
   *
   * @param batch
   * @return True if the fragment has enough incoming data to be able to be run.
   * @throws FragmentSetupException, IOException
   */
  boolean handle(IncomingDataBatch batch) throws FragmentSetupException, IOException;

  /**
   * Get the fragment runner for this incoming fragment. Note, this can only be requested once.
   *
   * @return
   */
  FragmentExecutor getRunnable();

  void cancel();

  /**
   * Find out if the FragmentManager has been cancelled.
   *
   * @return true if the FragmentManager has been cancelled.
   */
  boolean isCancelled();

  /**
   * If the executor is paused (for testing), this method should unpause the executor. This method should handle
   * multiple calls.
   */
  void unpause();

  boolean isWaiting();

  FragmentHandle getHandle();

  FragmentContext getFragmentContext();

  void receivingFragmentFinished(final FragmentHandle handle);

}
