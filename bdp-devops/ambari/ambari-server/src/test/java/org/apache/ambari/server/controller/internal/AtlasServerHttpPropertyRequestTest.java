/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.junit.Assert;
import org.junit.Test;

/**
 * AtlasServerHttpPropertyRequest tests.
 */
public class AtlasServerHttpPropertyRequestTest {

  @Test
  public void testGetUrl() throws Exception {

    Cluster cluster = createNiceMock(Cluster.class);
    Config config = createNiceMock(Config.class);

    Map<String, String> map = new HashMap<>();
    map.put("atlas.enableTLS", "false");
    map.put("atlas.server.http.port", "21000");

    expect(cluster.getDesiredConfigByType("application-properties")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(map).anyTimes();

    replay(cluster, config);

    AtlasServerHttpPropertyRequest propertyRequest = new AtlasServerHttpPropertyRequest();

    String url = propertyRequest.getUrl(cluster, "host1");

    Assert.assertEquals("http://host1:21000/api/atlas/admin/status", url);
  }

  @Test
  public void testGetUrl_https() throws Exception {

    Cluster cluster = createNiceMock(Cluster.class);
    Config config = createNiceMock(Config.class);

    Map<String, String> map = new HashMap<>();
    map.put("atlas.enableTLS", "true");
    map.put("atlas.server.https.port", "21443");

    expect(cluster.getDesiredConfigByType("application-properties")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(map).anyTimes();

    replay(cluster, config);

    AtlasServerHttpPropertyRequest propertyRequest = new AtlasServerHttpPropertyRequest();

    String url = propertyRequest.getUrl(cluster, "host1");

    Assert.assertEquals("https://host1:21443/api/atlas/admin/status", url);
  }
}
