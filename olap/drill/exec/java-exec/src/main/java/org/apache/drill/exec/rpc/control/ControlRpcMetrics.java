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
package org.apache.drill.exec.rpc.control;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.rpc.AbstractRpcMetrics;
import com.codahale.metrics.Counter;
import org.apache.drill.exec.rpc.RpcMetrics;

/**
 * Holds metrics related to bit control rpc layer
 */
public class ControlRpcMetrics extends AbstractRpcMetrics {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlRpcMetrics.class);

  // Total number of control connection's as client and server for a DrillBit.
  // i.e. Sum of incoming and outgoing control connections.
  private static final Counter encryptedConnections = DrillMetrics.getRegistry()
      .counter(CONNECTION_COUNTER_PREFIX + "control.encrypted");

  private static final Counter unencryptedConnection = DrillMetrics.getRegistry()
      .counter(CONNECTION_COUNTER_PREFIX + "control.unencrypted");

  private static final RpcMetrics INSTANCE = new ControlRpcMetrics();

  // prevent instantiation
  private ControlRpcMetrics() {
  }

  public static RpcMetrics getInstance() {
    return INSTANCE;
  }

  /**
   * Should only be called when first access to getInstance is made. In this case inside {@link ControllerImpl}.
   * {@link ControlConnection} using the singleton instance should not call initialize.
   *
   * @param useEncryptedCounter
   * @param allocator
   */
  @Override
  public void initialize(boolean useEncryptedCounter, BufferAllocator allocator) {
    this.useEncryptedCounter = useEncryptedCounter;
    registerAllocatorMetrics(allocator);
  }

  @Override
  public void addConnectionCount() {
    if (useEncryptedCounter) {
      encryptedConnections.inc();
    } else {
      unencryptedConnection.inc();
    }
  }

  @Override
  public void decConnectionCount() {
    if (useEncryptedCounter) {
      encryptedConnections.dec();
    } else {
      unencryptedConnection.dec();
    }
  }

  @Override
  public long getEncryptedConnectionCount() {
    return encryptedConnections.getCount();
  }

  @Override
  public long getUnEncryptedConnectionCount() {
    return unencryptedConnection.getCount();
  }

  private void registerAllocatorMetrics(final BufferAllocator allocator) {
    registerAllocatorMetrics(allocator, ALLOCATOR_METRICS_PREFIX + "bit.control.");
  }
}