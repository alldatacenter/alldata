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

import java.net.SocketAddress;

import io.netty.util.concurrent.Future;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.rpc.AbstractDisposableUserClientConnection;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.UserSession;

public abstract class BaseWebUserConnection extends AbstractDisposableUserClientConnection implements ConnectionThrottle {

  protected WebSessionResources webSessionResources;

  public BaseWebUserConnection(WebSessionResources webSessionResources) {
    this.webSessionResources = webSessionResources;
  }

  @Override
  public UserSession getSession() {
    return webSessionResources.getSession();
  }

  @Override
  public Future<Void> getClosureFuture() {
    return webSessionResources.getCloseFuture();
  }

  @Override
  public SocketAddress getRemoteAddress() {
    return webSessionResources.getRemoteAddress();
  }

  @Override
  public void setAutoRead(boolean enableAutoRead) { }

  public WebSessionResources resources() {
    return webSessionResources;
  }

  protected String webDataType(MajorType majorType) {
    StringBuilder dataType = new StringBuilder(majorType.getMinorType().name());

    // For DECIMAL type
    if (majorType.hasPrecision()) {
      dataType.append("(");
      dataType.append(majorType.getPrecision());

      if (majorType.hasScale()) {
        dataType.append(", ");
        dataType.append(majorType.getScale());
      }

      dataType.append(")");
    } else if (majorType.hasWidth()) {
      // Case for VARCHAR columns with specified width
      dataType.append("(");
      dataType.append(majorType.getWidth());
      dataType.append(")");
    }
    return dataType.toString();
  }
}
