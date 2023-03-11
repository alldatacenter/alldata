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

import org.apache.commons.pool2.ObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

public class ThriftClient<T extends TServiceClient> implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(ThriftClient.class);

  private final TServiceClient client;

  private final ObjectPool<ThriftClient<T>> pool;

  private final ServiceInfo serviceInfo;

  private boolean finish;

  public ThriftClient(TServiceClient client, ObjectPool<ThriftClient<T>> pool,
                      ServiceInfo serviceInfo) {
    super();
    this.client = client;
    this.pool = pool;
    this.serviceInfo = serviceInfo;
  }

  /**
   * get backend service which this client current connect to
   *
   * @return
   */
  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  /**
   * Retrieve the IFace
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public T iface() {
    return (T) client;
  }

  @Override
  public void close() {
    try {
      if (finish) {
        logger.info("return object to pool: " + this);
        finish = false;
        pool.returnObject(this);
      } else {
        logger.warn("not return object cause not finish {}", client);
        closeClient();
        pool.invalidateObject(this);
      }
    } catch (Exception e) {
      logger.warn("return object fail, close", e);
      closeClient();
    }
  }

  void closeClient() {
    logger.debug("close client {}", this);
    ThriftUtil.closeClient(this.client);
  }

  /**
   * client should return to pool
   */
  public void finish() {
    this.finish = true;
  }

  void setFinish(boolean finish) {
    this.finish = finish;
  }

  public boolean isDisConnected() {
    TTransport inputTransport = client.getInputProtocol().getTransport();
    TTransport outputTransport = client.getOutputProtocol().getTransport();
    return !inputTransport.isOpen() || !outputTransport.isOpen();
  }

}
