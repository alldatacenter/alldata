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

package org.apache.inlong.tubemq.server.broker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import org.apache.inlong.tubemq.server.common.fileconfig.ADConfig;
import org.apache.inlong.tubemq.server.common.fileconfig.PrometheusConfig;
import org.junit.Assert;
import org.junit.Test;

public class BrokerConfigTest {

    @Test
    public void testAuditAndPrometheusConfig_1() throws Exception {
        final BrokerConfig brokerConfig = new BrokerConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("broker_audit_prom_1.ini")).toURI());
        brokerConfig.loadFromFile(configUrl.toString());

        final ADConfig auditConfig = brokerConfig.getAuditConfig();
        Assert.assertTrue(auditConfig.isAuditEnable());
        Assert.assertEquals(auditConfig.getAuditIdProduce(), 3);
        Assert.assertEquals(auditConfig.getAuditIdConsume(), 4);
        Assert.assertEquals(auditConfig.getAuditCacheMaxRows(), 20000);
        Assert.assertEquals(auditConfig.getAuditCacheFilePath(), "/data/inlong/audit/test");
        HashSet<String> valueSet =
                new HashSet<>(Arrays.asList("127.0.0.2:10081", "127.0.0.3:10081"));
        Assert.assertEquals(auditConfig.getAuditProxyAddrSet(), valueSet);

        final PrometheusConfig promConfig = brokerConfig.getPrometheusConfig();
        Assert.assertTrue(promConfig.isPromEnable());
        Assert.assertEquals(promConfig.getPromHttpPort(), 9088);
        Assert.assertEquals(promConfig.getPromClusterName(), "Test");
    }

    @Test
    public void testAuditAndPrometheusConfig_2() throws Exception {
        final BrokerConfig brokerConfig = new BrokerConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("broker_audit_prom_2.ini")).toURI());
        brokerConfig.loadFromFile(configUrl.toString());

        final ADConfig auditConfig = brokerConfig.getAuditConfig();
        Assert.assertFalse(auditConfig.isAuditEnable());
        Assert.assertEquals(auditConfig.getAuditIdProduce(), 9);
        Assert.assertEquals(auditConfig.getAuditIdConsume(), 10);
        Assert.assertEquals(auditConfig.getAuditCacheMaxRows(), 2000000);
        Assert.assertEquals(auditConfig.getAuditCacheFilePath(), "/data/inlong/audit");
        HashSet<String> valueSet =
                new HashSet<>(Arrays.asList("127.0.0.1:10081"));
        Assert.assertEquals(auditConfig.getAuditProxyAddrSet(), valueSet);

        final PrometheusConfig promConfig = brokerConfig.getPrometheusConfig();
        Assert.assertFalse(promConfig.isPromEnable());
        Assert.assertEquals(promConfig.getPromHttpPort(), 9081);
        Assert.assertEquals(promConfig.getPromClusterName(), "InLong");
    }
}
