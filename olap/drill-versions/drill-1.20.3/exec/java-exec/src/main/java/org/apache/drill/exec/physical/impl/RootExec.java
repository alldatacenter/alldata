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
package org.apache.drill.exec.physical.impl;

import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;

/**
 * Node which is the last processing node in a query plan. FragmentTerminals
 * include Exchange output nodes and storage nodes. They are there driving force
 * behind the completion of a query.
 * </p>
 * Assumes that all implementations of {@link RootExec} assume that all their
 * methods are called by the same thread.
 */
public interface RootExec extends AutoCloseable {

  /**
   * Do the next batch of work.
   *
   * @return Whether or not additional batches of work are necessary. False
   *         means that this fragment is done.
   */
  boolean next();

  /**
   * Inform sender that receiving fragment is finished and doesn't need any more
   * data. This can be called multiple times (once for each downstream
   * receiver). If all receivers are finished then a subsequent call to
   * {@link #next()} will return false.
   *
   * @param handle
   *          The handle pointing to the downstream receiver that does not need
   *          anymore data.
   */
  void receivingFragmentFinished(FragmentHandle handle);

  /**
   * Dump failed batches' state preceded by its parent's state to logs. Invoked
   * when there is a failure during fragment execution.
   *
   * @param t the exception thrown by an operator and which therefore
   * records, in its stack trace, which operators were active on the stack
   */
  void dumpBatches(Throwable t);
}
