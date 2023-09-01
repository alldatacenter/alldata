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

package org.apache.uniffle.common.metrics;

import java.util.ArrayList;
import java.util.List;

import io.prometheus.client.CollectorRegistry;

import org.apache.uniffle.common.config.RssConf;

public abstract class AbstractMetricReporter implements MetricReporter {
  protected final RssConf conf;
  protected final String instanceId;
  protected List<CollectorRegistry> registryList = new ArrayList<>();

  public AbstractMetricReporter(RssConf conf, String instanceId) {
    this.conf = conf;
    this.instanceId = instanceId;
  }

  @Override
  public void addCollectorRegistry(CollectorRegistry registry) {
    registryList.add(registry);
  }
}
