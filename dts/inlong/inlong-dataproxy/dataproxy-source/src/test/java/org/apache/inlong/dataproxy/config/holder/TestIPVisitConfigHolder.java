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

package org.apache.inlong.dataproxy.config.holder;

import org.apache.inlong.dataproxy.config.ConfigManager;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link SourceReportConfigHolder}
 */
public class TestIPVisitConfigHolder {

    @Test
    public void testCase() {
        Assert.assertTrue(ConfigManager.getInstance().needChkIllegalIP());
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("127.0.0.1"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("127.0.0.5"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("127.0.0.2"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("1.2.53.3"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.187.0.1"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.168.1.64"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("192.168.1.192"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("192.168.1.224"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("192.168.1.192"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("192.168.1.193"));
        Assert.assertTrue(ConfigManager.getInstance().isIllegalIP("192.168.1.200"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.168.1.178"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.165.1.0"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.165.2.0"));
        Assert.assertFalse(ConfigManager.getInstance().isIllegalIP("192.165.2.105"));
    }
}
