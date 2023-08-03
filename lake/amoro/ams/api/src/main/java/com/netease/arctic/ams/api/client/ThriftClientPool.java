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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ThriftClientPool<T extends org.apache.thrift.TServiceClient> {

  private static final Logger LOG = LoggerFactory.getLogger(ThriftClientPool.class);
  // for thrift connects
  private static final int retries = 5;
  private static final int retryInterval = 2000;
  private static final int maxMessageSize = 100 * 1024 * 1024;
  private final ThriftClientFactory clientFactory;
  private final ThriftPingFactory pingFactory;
  private final GenericObjectPool<ThriftClient<T>> pool;
  private final PoolConfig poolConfig;
  private final String serviceName;
  private String url;
  private boolean serviceReset = false;

  /**
   * Construct a new pool using
   *
   * @param url
   * @param factory
   * @param config
   */
  public ThriftClientPool(
      String url, ThriftClientFactory factory, ThriftPingFactory pingFactory,
      PoolConfig config, String serviceName) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("url is empty!");
    }
    if (factory == null) {
      throw new IllegalArgumentException("factory is empty!");
    }
    if (config == null) {
      throw new IllegalArgumentException("config is empty!");
    }

    this.url = url;
    this.clientFactory = factory;
    this.pingFactory = pingFactory;
    this.poolConfig = config;
    // test if config change
    this.poolConfig.setTestOnReturn(true);
    this.poolConfig.setTestOnBorrow(true);
    this.serviceName = serviceName;
    this.pool = new GenericObjectPool<>(new BasePooledObjectFactory<ThriftClient<T>>() {

      @Override
      public ThriftClient<T> create() throws Exception {

        // get from global list first
        ArcticThriftUrl arcticThriftUrl = ArcticThriftUrl.parse(url, serviceName);
        ServiceInfo serviceInfo = new ServiceInfo(arcticThriftUrl.host(), arcticThriftUrl.port());
        TTransport transport = getTransport(serviceInfo);

        try {
          transport.open();
        } catch (TTransportException e) {
          LOG.warn("transport open fail service: host={}, port={}",
              serviceInfo.getHost(), serviceInfo.getPort());
          if (poolConfig.isFailover()) {
            for (int i = 0; i < 5; i++) {
              try {
                arcticThriftUrl = ArcticThriftUrl.parse(url, serviceName);
                serviceInfo.setHost(arcticThriftUrl.host());
                serviceInfo.setPort(arcticThriftUrl.port());
                transport = getTransport(serviceInfo); // while break here
                LOG.info("failover to next service host={}, port={}",
                    serviceInfo.getHost(), serviceInfo.getPort());
                transport.open();
                break;
              } catch (TTransportException e2) {
                LOG.warn("transport open fail service: host={}, port={}",
                    serviceInfo.getHost(), serviceInfo.getPort());
              }
              Thread.sleep(retryInterval);
            }
            if (!transport.isOpen()) {
              throw new ConnectionFailException(
                  "connect error after try 5 times, last connect is: host=" + serviceInfo.getHost() + ", ip=" +
                      serviceInfo.getPort(), e);
            }
          } else {
            throw new ConnectionFailException("host=" + serviceInfo.getHost() + ", ip=" + serviceInfo.getPort(), e);
          }
        }

        ThriftClient<T> client = new ThriftClient<>(clientFactory.createClient(transport),
            pool, serviceInfo);

        LOG.debug("create new object for pool {}", client);
        return client;
      }

      @Override
      public PooledObject<ThriftClient<T>> wrap(ThriftClient<T> obj) {
        return new DefaultPooledObject<>(obj);
      }

      @Override
      public void destroyObject(PooledObject<ThriftClient<T>> p) throws Exception {
        p.getObject().closeClient();
        super.destroyObject(p);
      }
    }, poolConfig);
  }

  private TTransport getTransport(ServiceInfo serviceInfo) throws TTransportException {

    if (serviceInfo == null) {
      throw new NoBackendServiceException();
    }

    TTransport transport = null;
    if (poolConfig.getTimeout() > 0) {
      transport = new TFramedTransport(new TSocket(serviceInfo.getHost(), serviceInfo.getPort(),
          poolConfig.getTimeout()), maxMessageSize);
    } else {
      transport = new TFramedTransport(new TSocket(serviceInfo.getHost(), serviceInfo.getPort()), maxMessageSize);
    }
    return transport;
  }

  /**
   * get a random service
   *
   * @param serviceList
   * @return
   */
  private ServiceInfo getRandomService(List<ServiceInfo> serviceList) {
    if (serviceList == null || serviceList.size() == 0) {
      return null;
    }
    return serviceList.get(new Random().nextInt(serviceList.size()));
  }

  private List<ServiceInfo> removeFailService(List<ServiceInfo> list, ServiceInfo serviceInfo) {
    LOG.info("remove service from current service list: host={}, port={}",
        serviceInfo.getHost(), serviceInfo.getPort());
    return list.stream() //
        .filter(si -> !serviceInfo.equals(si)) //
        .collect(Collectors.toList());
  }

  /**
   * get a client's IFace from pool
   *
   * <ul>
   * <li>
   * <span style="color:red">Important: Iface is totally generated by
   * thrift, a ClassCastException will be thrown if assign not
   * match!</span></li>
   * <li>
   * <span style="color:red">Limitation: The return object can only used
   * once.</span></li>
   * </ul>
   *
   * @return
   * @throws ThriftException
   * @throws NoBackendServiceException if
   *                                   {@link PoolConfig#setFailover(boolean)} is set and no
   *                                   service can connect to
   * @throws ConnectionFailException   if
   *                                   {@link PoolConfig#setFailover(boolean)} not set and
   *                                   connection fail
   * @throws IllegalStateException     if call method on return object twice
   */
  @SuppressWarnings("unchecked")
  public <X> X iface() {
    ThriftClient<T> client = null;
    int attempt;
    for (attempt = 0; attempt < retries; ++attempt) {
      try {
        client = pool.borrowObject();
        if (client.isDisConnected() || !pingFactory.ping(client.iface())) {
          if (attempt > 1) {
            // if attempt > 1, it means the server is maybe restarting, so we should wait a while
            LOG.warn("maybe server is restarting, wait a while");
            Thread.sleep(retryInterval);
          }
          pool.clear();
          client = pool.borrowObject();
        } else {
          break;
        }
      } catch (Exception e) {
        if (e instanceof ThriftException) {
          throw (ThriftException) e;
        }
        throw new ThriftException("Get client from pool failed.", e);
      }
    }
    if (attempt >= retries) {
      throw new ThriftException("Client can not connect.");
    }
    AtomicBoolean returnToPool = new AtomicBoolean(false);
    ThriftClient<T> finalClient = client;
    return (X) Proxy.newProxyInstance(this.getClass().getClassLoader(), finalClient.iface()
        .getClass().getInterfaces(), (proxy, method, args) -> {
        if (returnToPool.get()) {
          throw new IllegalStateException("Object returned via iface can only used once!");
        }
        boolean success = false;
        try {
          Object result = method.invoke(finalClient.iface(), args);
          success = true;
          return result;
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        } finally {
          if (success) {
            pool.returnObject(finalClient);
          } else {
            finalClient.closeClient();
            pool.invalidateObject(finalClient);
          }
          returnToPool.set(true);
        }
      });
  }
}
