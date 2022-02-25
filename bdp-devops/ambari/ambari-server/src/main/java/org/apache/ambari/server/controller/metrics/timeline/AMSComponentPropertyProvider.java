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
package org.apache.ambari.server.controller.metrics.timeline;

import java.util.Map;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Resource;

public class AMSComponentPropertyProvider extends AMSPropertyProvider {

  public AMSComponentPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                          URLStreamProvider streamProvider,
                                          ComponentSSLConfiguration configuration,
                                          TimelineMetricCacheProvider cacheProvider,
                                          MetricHostProvider hostProvider,
                                          String clusterNamePropertyId,
                                          String componentNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration,
      cacheProvider, hostProvider, clusterNamePropertyId, null, componentNamePropertyId);
  }

  @Override
  protected String getHostName(Resource resource) {
      return hostProvider
        .getExternalHostName((String) resource.getPropertyValue(clusterNamePropertyId), (String) resource.getPropertyValue(componentNamePropertyId))
        .orElse(null);
  }

  @Override
  protected String getComponentName(Resource resource) {
    return (String) resource.getPropertyValue(componentNamePropertyId);
  }
}
