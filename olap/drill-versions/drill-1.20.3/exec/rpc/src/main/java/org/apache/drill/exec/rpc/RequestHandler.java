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

import io.netty.buffer.ByteBuf;

/**
 * Note that if a handler maintains any internal state, the state will be disposed if the handler on the connection
 * changes. So handler should not maintain state.
 *
 * @param <S> server connection type
 */
public interface RequestHandler<S extends ServerConnection<S>> {

  /**
   * Handle request of given type (rpcType) with message (pBody) and optional data (dBody)
   * on the connection, and return the appropriate response.
   *
   * The method must do one of three things:
   * + use {@link ResponseSender#send send} the response
   * + throw UserRpcException, in which case a response will be sent using {@link ResponseSender#send send}
   * + throw an Exception, in which case, the connection will be dropped
   *
   * @param connection remote connection
   * @param rpcType    rpc type
   * @param pBody      message
   * @param dBody      data, maybe null
   * @param sender     used to {@link ResponseSender#send send} the response
   * @throws RpcException
   */
  void handle(S connection, int rpcType, ByteBuf pBody, ByteBuf dBody, ResponseSender sender)
      throws RpcException;

}
