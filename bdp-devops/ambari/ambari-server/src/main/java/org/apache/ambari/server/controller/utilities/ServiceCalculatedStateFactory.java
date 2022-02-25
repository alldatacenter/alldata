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

package org.apache.ambari.server.controller.utilities;

import java.util.Map;

import org.apache.ambari.server.controller.utilities.state.DefaultServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.FlumeServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.HBaseServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.HDFSServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.HiveServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.OozieServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.ServiceCalculatedState;
import org.apache.ambari.server.controller.utilities.state.YARNServiceCalculatedState;
import org.apache.ambari.server.state.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.util.concurrent.ConcurrentHashMap;


public class ServiceCalculatedStateFactory {

  private final static ServiceCalculatedState DEFAULT_SERVICE_CALCULATED_STATE = new DefaultServiceCalculatedState();
  private final static Logger LOG = LoggerFactory.getLogger(ServiceCalculatedStateFactory.class);

  // cache for the service calculated state objects
  private final static Map<Service.Type, ServiceCalculatedState> serviceStateProviders = new ConcurrentHashMap<>();


  public static ServiceCalculatedState getServiceStateProvider(String service){
    ServiceCalculatedState suggestedServiceProvider;
    Service.Type serviceType = null;

    try {
      serviceType = Service.Type.valueOf(service);
    } catch (Exception e){
      LOG.debug("Could not parse service name \"{}\", will use default state provider", service);
    }

    if (serviceType == null) {  // service is unknown, return default service state provider
      return DEFAULT_SERVICE_CALCULATED_STATE;
    }

    if (serviceStateProviders.containsKey(serviceType)) {   // use cache on hit
      suggestedServiceProvider = serviceStateProviders.get(serviceType);
    } else {
      switch (serviceType) {
        case HDFS:
          suggestedServiceProvider = new HDFSServiceCalculatedState();
          break;
        case FLUME:
          suggestedServiceProvider = new FlumeServiceCalculatedState();
          break;
        case OOZIE:
          suggestedServiceProvider = new OozieServiceCalculatedState();
          break;
        case YARN:
          suggestedServiceProvider = new YARNServiceCalculatedState();
          break;
        case HIVE:
          suggestedServiceProvider = new HiveServiceCalculatedState();
          break;
        case HBASE:
          suggestedServiceProvider = new HBaseServiceCalculatedState();
          break;
        default:
          suggestedServiceProvider = DEFAULT_SERVICE_CALCULATED_STATE;
          break;
      }
      // lazy cache initialization, create just required objects
      serviceStateProviders.put(serviceType, suggestedServiceProvider);
    }
    return suggestedServiceProvider;
  }
}
