/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.flume;

import junit.framework.TestCase;
import org.apache.flume.Context;
import org.apache.flume.conf.FlumeConfiguration;

import java.io.InputStream;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-07 14:51
 **/
public class TestTisPropertiesFileConfigurationProvider extends TestCase {

    public void testToMapWithPlaceholderHelper() {
        String dataDir = "/opt/data/tis";
        String agentName = "tis-agent1";
       // System.setProperty("tis.test.data.dir", dataDir);
        TisPropertiesFileConfigurationProvider configurationProvider = new TisPropertiesFileConfigurationProvider(agentName) {
            @Override
            protected InputStream getConfigResource() throws Exception {
                return
                        TisPropertiesFileConfigurationProvider.class.getResourceAsStream("tis-test-flume.properties");
            }
        };

        FlumeConfiguration flumeConfiguration = configurationProvider.getFlumeConfiguration();
        assertNotNull(flumeConfiguration);

        FlumeConfiguration.AgentConfiguration configurationFor = flumeConfiguration.getConfigurationFor(agentName);
        assertNotNull(configurationFor);

        Map<String, Context> channelContext = configurationFor.getChannelContext();
        Context ch1Context = channelContext.get("ch1");
        assertEquals("checkpointDir", dataDir + "/flume/checkpoint", ch1Context.getString("checkpointDir"));
        assertEquals("dataDirs", dataDir + "/flume", ch1Context.getString("dataDirs"));
    }
}
