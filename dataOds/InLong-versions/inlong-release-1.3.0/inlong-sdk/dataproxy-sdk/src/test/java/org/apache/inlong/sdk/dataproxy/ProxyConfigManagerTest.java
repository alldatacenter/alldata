/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy;

import org.apache.inlong.sdk.dataproxy.config.ProxyConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigManager;
import org.apache.inlong.sdk.dataproxy.network.ClientMgr;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

public class ProxyConfigManagerTest {

    private final String localFile = Paths.get(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("proxylist.json")).toURI())
            .toString();
    private final ProxyClientConfig clientConfig = PowerMockito.mock(ProxyClientConfig.class);
    private final ClientMgr clientMgr = PowerMockito.mock(ClientMgr.class);
    private final ProxyConfigManager proxyConfigManager = new ProxyConfigManager(clientConfig, "127.0.0.1",
            clientMgr);

    public ProxyConfigManagerTest() throws URISyntaxException {
    }

    @Test
    public void testProxyConfigParse() throws Exception {
        ProxyConfigEntry proxyEntry = proxyConfigManager.getLocalProxyListFromFile(localFile);
        Assert.assertEquals(proxyEntry.isInterVisit(), false);
        Assert.assertEquals(proxyEntry.getLoad(), 12);
        Assert.assertEquals(proxyEntry.getClusterId(), 1);
        Assert.assertEquals(proxyEntry.getSize(), 2);
        Assert.assertEquals(proxyEntry.getHostMap().containsKey("127.0.0.1:46801"), true);
        Assert.assertEquals(proxyEntry.getHostMap().containsKey("127.0.0.1:8080"), false);
    }

}
