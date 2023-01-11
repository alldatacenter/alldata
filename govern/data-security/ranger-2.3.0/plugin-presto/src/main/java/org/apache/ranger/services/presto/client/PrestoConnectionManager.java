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
package org.apache.ranger.services.presto.client;

import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class PrestoConnectionManager {
  private static final Logger LOG = LoggerFactory.getLogger(PrestoConnectionManager.class);

  protected ConcurrentMap<String, PrestoClient> prestoConnectionCache;
  protected ConcurrentMap<String, Boolean> repoConnectStatusMap;

  public PrestoConnectionManager() {
    prestoConnectionCache = new ConcurrentHashMap<>();
    repoConnectStatusMap = new ConcurrentHashMap<>();
  }

  public PrestoClient getPrestoConnection(final String serviceName, final String serviceType, final Map<String, String> configs) {
    PrestoClient prestoClient = null;

    if (serviceType != null) {
      prestoClient = prestoConnectionCache.get(serviceName);
      if (prestoClient == null) {
        if (configs != null) {
          final Callable<PrestoClient> connectPresto = new Callable<PrestoClient>() {
            @Override
            public PrestoClient call() throws Exception {
              return new PrestoClient(serviceName, configs);
            }
          };
          try {
            prestoClient = TimedEventUtil.timedTask(connectPresto, 5, TimeUnit.SECONDS);
          } catch (Exception e) {
            LOG.error("Error connecting to Presto repository: " +
            serviceName + " using config: " + configs, e);
          }

          PrestoClient oldClient = null;
          if (prestoClient != null) {
            oldClient = prestoConnectionCache.putIfAbsent(serviceName, prestoClient);
          } else {
            oldClient = prestoConnectionCache.get(serviceName);
          }

          if (oldClient != null) {
            if (prestoClient != null) {
              prestoClient.close();
            }
            prestoClient = oldClient;
          }
          repoConnectStatusMap.put(serviceName, true);
        } else {
          LOG.error("Connection Config not defined for asset :"
            + serviceName, new Throwable());
        }
      } else {
        try {
          prestoClient.getCatalogList("*", null);
        } catch (Exception e) {
          prestoConnectionCache.remove(serviceName);
          prestoClient.close();
          prestoClient = getPrestoConnection(serviceName, serviceType, configs);
        }
      }
    } else {
      LOG.error("Asset not found with name " + serviceName, new Throwable());
    }
    return prestoClient;
  }
}
