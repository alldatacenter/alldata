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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricRegister;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

/**
 * Test cases for {@link SQLServerSource}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SQLServerSource.class, MetricRegister.class})
@PowerMockIgnore({"javax.management.*"})
public class TestSQLServerSource {

    @Mock
    JobProfile jobProfile;

    @Mock
    private AgentMetricItemSet agentMetricItemSet;

    @Mock
    private AgentMetricItem agentMetricItem;

    private AtomicLong sourceSuccessCount;

    private AtomicLong sourceFailCount;

    @Before
    public void setup() throws Exception {
        sourceSuccessCount = new AtomicLong(0);
        sourceFailCount = new AtomicLong(0);

        // mock metrics
        whenNew(AgentMetricItemSet.class).withArguments(anyString()).thenReturn(agentMetricItemSet);
        when(agentMetricItemSet.findMetricItem(any())).thenReturn(agentMetricItem);
        field(AgentMetricItem.class, "sourceSuccessCount").set(agentMetricItem, sourceSuccessCount);
        field(AgentMetricItem.class, "sourceFailCount").set(agentMetricItem, sourceFailCount);
        PowerMockito.mockStatic(MetricRegister.class);
        PowerMockito.doNothing().when(
                MetricRegister.class, "register", any(MetricItem.class));
    }

    /**
     * Test cases for {@link SQLServerSource#split(JobProfile)}.
     */
    @Test
    public void testSplit() {

        // build mock
        final SQLServerSource source = new SQLServerSource();
        // assert
        assertEquals(1, source.split(jobProfile).size());
    }
}
