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
 * This handler fails any request on the connection. Example use case: the peer is making requests
 * before authenticating.
 *
 * @param <S> server connection type
 */
public class FailingRequestHandler<S extends ServerConnection<S>> implements RequestHandler<S> {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FailingRequestHandler.class);

  @Override
  public void handle(S connection, int rpcType, ByteBuf pBody, ByteBuf dBody, ResponseSender sender)
      throws RpcException {

    // drops connection
    throw new RpcException(String.format("Request of type %d is not yet allowed. Dropping connection to %s.",
            rpcType, connection.getRemoteAddress()));
  }
}
