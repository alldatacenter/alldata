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
package org.apache.ambari.server.controller.metrics;

import java.util.Map;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetricsReportPropertyProvider extends AbstractPropertyProvider {

  protected final StreamProvider streamProvider;

  protected final MetricHostProvider hostProvider;

  protected final String clusterNamePropertyId;

  protected final ComponentSSLConfiguration configuration;

  protected static final MetricsPaddingMethod DEFAULT_PADDING_METHOD =
    new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

  // ----- Constants --------------------------------------------------------

  private static final Logger LOG =
    LoggerFactory.getLogger(MetricsReportPropertyProvider.class);


  // ----- Constructors ------------------------------------------------------

  public MetricsReportPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                       StreamProvider streamProvider,
                                       ComponentSSLConfiguration configuration,
                                       MetricHostProvider hostProvider,
                                       String clusterNamePropertyId) {
    super(componentPropertyInfoMap);

    this.streamProvider = streamProvider;
    this.hostProvider = hostProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.configuration = configuration;
  }

  public static MetricsReportPropertyProviderProxy createInstance(
          Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
          URLStreamProvider streamProvider,
          ComponentSSLConfiguration configuration,
          TimelineMetricCacheProvider cacheProvider,
          MetricHostProvider hostProvider,
          MetricsServiceProvider serviceProvider,
          String clusterNamePropertyId) {

    return new MetricsReportPropertyProviderProxy(componentPropertyInfoMap,
                                                  streamProvider,
                                                  configuration,
                                                  cacheProvider,
                                                  hostProvider,
                                                  serviceProvider,
                                                  clusterNamePropertyId);
  }
}
