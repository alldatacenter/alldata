/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.api.client;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ThriftUtil {

  private static final Logger logger = LoggerFactory.getLogger(ThriftUtil.class);

  /**
   * close internal transport
   *
   * @param client
   */
  public static void closeClient(TServiceClient client) {
    if (client == null) {
      return;
    }
    try {
      TProtocol proto = client.getInputProtocol();
      if (proto != null) {
        proto.getTransport().close();
      }
    } catch (Throwable e) {
      logger.warn("close input transport fail", e);
    }
    try {
      TProtocol proto = client.getOutputProtocol();
      if (proto != null) {
        proto.getTransport().close();
      }
    } catch (Throwable e) {
      logger.warn("close output transport fail", e);
    }

  }
}