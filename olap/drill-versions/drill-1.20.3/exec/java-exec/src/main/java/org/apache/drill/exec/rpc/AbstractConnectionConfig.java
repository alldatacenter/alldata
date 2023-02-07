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
package org.apache.drill.exec.rpc;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.security.AuthenticatorProvider;
import org.apache.drill.exec.server.BootStrapContext;

public abstract class AbstractConnectionConfig implements ConnectionConfig {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractConnectionConfig.class);

  private final BufferAllocator allocator;
  private final BootStrapContext context;
  protected EncryptionContext encryptionContext;

  protected AbstractConnectionConfig(BufferAllocator allocator, BootStrapContext context) {
    this.allocator = allocator;
    this.context = context;
    this.encryptionContext = new EncryptionContextImpl();
  }

  @Override
  public BootStrapContext getBootstrapContext() {
    return context;
  }

  @Override
  public BufferAllocator getAllocator() {
    return allocator;
  }

  @Override
  public AuthenticatorProvider getAuthProvider() {
    return context.getAuthProvider();
  }

  @Override
  public boolean isEncryptionEnabled() {
    return encryptionContext.isEncryptionEnabled();
  }

  public EncryptionContext getEncryptionCtxt() {
    return encryptionContext;
  }
}
