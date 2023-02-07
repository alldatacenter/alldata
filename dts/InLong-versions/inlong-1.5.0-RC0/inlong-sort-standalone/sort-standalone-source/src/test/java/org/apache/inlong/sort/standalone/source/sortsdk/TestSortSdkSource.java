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

package org.apache.inlong.sort.standalone.source.sortsdk;

import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sdk.commons.admin.AdminServiceRegister;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({SortClusterConfigHolder.class, LoggerFactory.class, Logger.class, MetricRegister.class,
        AdminServiceRegister.class})
public class TestSortSdkSource {

    private Context mockContext;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(LoggerFactory.class);
        Logger log = PowerMockito.mock(Logger.class);
        PowerMockito.when(LoggerFactory.getLogger(Mockito.any(Class.class))).thenReturn(log);
        PowerMockito.mockStatic(MetricRegister.class);

        PowerMockito.mockStatic(SortClusterConfigHolder.class);
        SortClusterConfig config = prepareSortClusterConfig(2);
        PowerMockito.when(SortClusterConfigHolder.getClusterConfig()).thenReturn(config);
        mockContext = PowerMockito.spy(new Context());
    }

    @Test
    public void testRun() {
        PowerMockito.mockStatic(AdminServiceRegister.class);
        try {
            PowerMockito.doNothing().when(AdminServiceRegister.class, "register", anyString(), anyString(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SortSdkSource testSource = new SortSdkSource();
        testSource.configure(mockContext);
        testSource.run();
        testSource.stop();
    }

    private SortClusterConfig prepareSortClusterConfig(final int size) {
        final SortClusterConfig testConfig = SortClusterConfig.builder().build();
        testConfig.setClusterName("testConfig");
        testConfig.setSortTasks(prepareSortTaskConfig(size));
        return testConfig;
    }

    private List<SortTaskConfig> prepareSortTaskConfig(final int size) {
        List<SortTaskConfig> configs = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            SortTaskConfig config = SortTaskConfig.builder().build();
            config.setName("testConfig" + i);
            configs.add(config);
        }
        Assert.assertEquals(size, configs.size());
        return configs;
    }

}