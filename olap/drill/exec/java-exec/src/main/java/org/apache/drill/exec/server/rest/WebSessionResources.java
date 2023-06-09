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
package org.apache.drill.exec.server.rest;

import io.netty.util.concurrent.Promise;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.ChannelClosedException;
import org.apache.drill.exec.rpc.user.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * Holds the resources required for Web User Session. This class is responsible
 * for the proper cleanup of all the resources.
 */
public class WebSessionResources implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(WebSessionResources.class);

  private final BufferAllocator allocator;

  private final SocketAddress remoteAddress;

  private final UserSession webUserSession;

  private Promise<Void> closeFuture;

  WebSessionResources(BufferAllocator allocator, SocketAddress remoteAddress,
                      UserSession userSession, Promise<Void> closeFuture) {
    this.allocator = allocator;
    this.remoteAddress = remoteAddress;
    this.webUserSession = userSession;
    this.closeFuture = closeFuture;
  }

  public UserSession getSession() {
    return webUserSession;
  }

  public BufferAllocator getAllocator() {
    return allocator;
  }

  public Promise<Void> getCloseFuture() {
    return closeFuture;
  }

  public SocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  @Override
  public void close() {
    try {
      AutoCloseables.close(webUserSession, allocator);
    } catch (Exception ex) {
      logger.error("Failure while closing the session resources", ex);
    }

    // Notify all the listeners of this closeFuture for failure events so that listeners can do cleanup related to this
    // WebSession. This will be called after every query execution by AnonymousWebUserConnection::cleanupSession and
    // for authenticated user it is called when session is invalidated.
    // For authenticated user it will cancel the in-flight queries based on session invalidation. Whereas for
    // unauthenticated user it's a no-op since there is no session associated with it. We don't have mechanism currently
    // to call this close future upon Http connection close.
    if (closeFuture != null) {
      closeFuture.setFailure(new ChannelClosedException("Http connection is closed by Web Client"));
      closeFuture = null;
    }
  }
}
