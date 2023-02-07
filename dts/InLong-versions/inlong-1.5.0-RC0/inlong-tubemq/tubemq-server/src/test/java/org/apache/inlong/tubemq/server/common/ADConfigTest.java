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

package org.apache.inlong.tubemq.server.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.server.common.fileconfig.ADConfig;
import org.junit.Assert;
import org.junit.Test;

public class ADConfigTest {

    @Test
    public void checkProducerTopicTest() {
        ADConfig auditConfig = new ADConfig();
        Assert.assertFalse(auditConfig.isAuditEnable());
        Assert.assertEquals(auditConfig.getAuditIdConsume(), 10);
        auditConfig.setAuditIdConsume(7);
        Assert.assertEquals(auditConfig.getAuditIdConsume(), 7);
        auditConfig.setAuditIdProduce(5);
        Assert.assertEquals(auditConfig.getAuditIdProduce(), 5);
        auditConfig.setAuditCacheMaxRows(1000);
        Assert.assertEquals(auditConfig.getAuditCacheMaxRows(), 1000);
        auditConfig.setAuditEnable(true);
        Assert.assertTrue(auditConfig.isAuditEnable());
        List<String> addrs = new ArrayList<>();
        addrs.add("test");
        auditConfig.setAuditProxyAddrSet(addrs);
        Assert.assertEquals(auditConfig.getAuditProxyAddrSet().size(), addrs.size());
        for (String addr : addrs) {
            Assert.assertTrue(auditConfig.getAuditProxyAddrSet().contains(addr));
        }
        auditConfig.setAuditCacheFilePath("aaa");
        Assert.assertEquals(auditConfig.getAuditCacheFilePath(), "aaa");
    }

}
