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
package org.apache.ambari.server.controller.internal;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.state.ConfigHelper;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OverriddenMetricsHostProviderTest extends EasyMockSupport {
  private static final String COMPONENT_WITH_OVERRIDDEN_HOST = "component1";
  private static final String CLUSTER_1 = "cluster1";
  private static final String COMPONENT_WITHOUT_OVERRIDDEN_HOST = "componentWithoutOverriddenHost";
  private static final String OVERRIDEN_HOST = "overridenHost1";
  private static final String COMPONENT_WITH_OVERRIDDEN_HOST_PLACEHOLDER = "${hdfs-site/dfs.namenode.http-address}";
  private static final String RESOLVED_HOST = "resolved.fqdn";
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);
  @Mock
  private MetricHostProvider defaultHostProvider;
  @Mock
  private ConfigHelper configHelper;
  private MetricHostProvider hostProvider;

  @Before
  public void setUp() throws Exception {
    hostProvider = new OverriddenMetricsHostProvider(overrideHosts(), defaultHostProvider, configHelper);
  }

  @Test
  public void testReturnsDefaultWhenNotOverridden() throws Exception {
    replayAll();
    assertThat(hostProvider.getExternalHostName(CLUSTER_1, COMPONENT_WITHOUT_OVERRIDDEN_HOST), is(Optional.empty()));
    verifyAll();
  }

  @Test
  public void testReturnOverriddenHostIfPresent() throws Exception {
    expect(configHelper.getEffectiveConfigProperties(CLUSTER_1, null)).andReturn(emptyMap()).anyTimes();
    replayAll();
    assertThat(hostProvider.getExternalHostName(CLUSTER_1, COMPONENT_WITH_OVERRIDDEN_HOST), is(Optional.of(OVERRIDEN_HOST)));
    verifyAll();
  }

  @Test
  public void testReplacesPlaceholderInOverriddenHost() throws Exception {
    expect(configHelper.getEffectiveConfigProperties(CLUSTER_1, null)).andReturn(config()).anyTimes();
    replayAll();
    assertThat(hostProvider.getExternalHostName(CLUSTER_1, COMPONENT_WITH_OVERRIDDEN_HOST_PLACEHOLDER), is(Optional.of(RESOLVED_HOST)));
    verifyAll();
  }

  private Map<String, String> overrideHosts() {
    return new HashMap<String, String>() {{
      put(COMPONENT_WITH_OVERRIDDEN_HOST, OVERRIDEN_HOST);
      put(COMPONENT_WITH_OVERRIDDEN_HOST_PLACEHOLDER, "${hdfs-site/dfs.namenode.http-address}");
    }};
  }

  private Map<String, Map<String, String>> config() {
    return new HashMap<String, Map<String, String>>() {{
      put("hdfs-site", new HashMap<String, String>() {{
        put("dfs.namenode.http-address", "http://" + RESOLVED_HOST + ":8080");
      }});
    }};
  }
}