/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.network.server;

import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.protocol.RequestMessage;

/** Handler for sendRPC() messages sent by {@link TransportClient}s. */
public class BaseMessageHandler {

  public void receive(TransportClient client, RequestMessage msg) {
    throw new UnsupportedOperationException();
  }

  public boolean checkRegistered() {
    throw new UnsupportedOperationException();
  }

  /** Invoked when the channel associated with the given client is active. */
  public void channelActive(TransportClient client) {}

  /**
   * Invoked when the channel associated with the given client is inactive. No further requests will
   * come from this client.
   */
  public void channelInactive(TransportClient client) {}

  public void exceptionCaught(Throwable cause, TransportClient client) {}
}
