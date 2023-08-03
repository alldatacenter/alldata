/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.model;


import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;


public class TestRangerService {

    @Test
    public void test_configToString() {
        RangerService svc = new RangerService("hdfs", "dev_hdfs", "HDFS", "dev_tag", new HashMap<>());
        String        str = svc.toString();

        Assert.assertTrue(!StringUtils.containsIgnoreCase(str, RangerService.CONFIG_PASSWORD));


        svc.getConfigs().put(RangerService.CONFIG_PASSWORD, "test1234");

        str = svc.toString();

        Assert.assertTrue(StringUtils.containsIgnoreCase(str, RangerService.CONFIG_PASSWORD));
        Assert.assertTrue(!StringUtils.containsIgnoreCase(str, "test1234"));
        Assert.assertTrue(StringUtils.containsIgnoreCase(str, RangerService.MASKED_PASSWORD_VALUE));
    }
}
