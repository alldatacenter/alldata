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

import java.util.concurrent.ExecutionException;

import org.apache.drill.common.exceptions.DrillIOException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;

/**
 * Parent class for all rpc exceptions.
 */
public class RpcException extends DrillIOException {
  private static final long serialVersionUID = -5964230316010502319L;

  public RpcException() {
    super();
  }

  public RpcException(String message, Throwable cause) {
    super(format(message), cause);
  }

  private static String format(String message) {
    return DrillStringUtils.unescapeJava(message);
  }

  public RpcException(String message) {
    super(format(message));
  }

  public RpcException(Throwable cause) {
    super(cause);
  }

  public static RpcException mapException(Throwable t) {
    while (t instanceof ExecutionException) {
      t = t.getCause();
    }
    if (t instanceof RpcException) {
      return ((RpcException) t);
    }
    return new RpcException(t);
  }

  public static RpcException mapException(String message, Throwable t) {
    while (t instanceof ExecutionException) {
      t = t.getCause();
    }
    return new RpcException(message, t);
  }

  public boolean isRemote() {
    return false;
  }

  public DrillPBError getRemoteError() {
    throw new UnsupportedOperationException();
  }
}
