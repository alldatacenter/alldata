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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.services.trino.client;

import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class TrinoConnectionManager {
  private static final Logger LOG = LoggerFactory.getLogger(TrinoConnectionManager.class);

  protected ConcurrentMap<String, TrinoClient> trinoConnectionCache;
  protected ConcurrentMap<String, Boolean> repoConnectStatusMap;

  public TrinoConnectionManager() {
    trinoConnectionCache = new ConcurrentHashMap<>();
    repoConnectStatusMap = new ConcurrentHashMap<>();
  }

  public TrinoClient getTrinoConnection(final String serviceName, final String serviceType, final Map<String, String> configs) {
    TrinoClient trinoClient = null;

    if (serviceType != null) {
      trinoClient = trinoConnectionCache.get(serviceName);
      if (trinoClient == null) {
        if (configs != null) {
          final Callable<TrinoClient> connectTrino = new Callable<TrinoClient>() {
            @Override
            public TrinoClient call() throws Exception {
              return new TrinoClient(serviceName, configs);
            }
          };
          try {
            trinoClient = TimedEventUtil.timedTask(connectTrino, 5, TimeUnit.SECONDS);
          } catch (Exception e) {
            LOG.error("Error connecting to Trino repository: " +
            serviceName + " using config: " + configs, e);
          }

          TrinoClient oldClient = null;
          if (trinoClient != null) {
            oldClient = trinoConnectionCache.putIfAbsent(serviceName, trinoClient);
          } else {
            oldClient = trinoConnectionCache.get(serviceName);
          }

          if (oldClient != null) {
            if (trinoClient != null) {
              trinoClient.close();
            }
            trinoClient = oldClient;
          }
          repoConnectStatusMap.put(serviceName, true);
        } else {
          LOG.error("Connection Config not defined for asset :"
            + serviceName, new Throwable());
        }
      } else {
        try {
          trinoClient.getCatalogList("*", null);
        } catch (Exception e) {
          trinoConnectionCache.remove(serviceName);
          trinoClient.close();
          trinoClient = getTrinoConnection(serviceName, serviceType, configs);
        }
      }
    } else {
      LOG.error("Asset not found with name " + serviceName, new Throwable());
    }
    return trinoClient;
  }
}
