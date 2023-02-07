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

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.BitConnectionConfig;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.work.batch.ControlMessageHandler;

// config for bit to bit connection
@VisibleForTesting
public class ControlConnectionConfig extends BitConnectionConfig {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlConnectionConfig.class);

  private final ControlMessageHandler handler;

  ControlConnectionConfig(BufferAllocator allocator, BootStrapContext context, ControlMessageHandler handler)
      throws DrillbitStartupException {
    super(allocator, context);
    this.handler = handler;
  }

  @Override
  public String getName() {
    return "control"; // unused
  }

  @VisibleForTesting
  public ControlMessageHandler getMessageHandler() {
    return handler;
  }
}
