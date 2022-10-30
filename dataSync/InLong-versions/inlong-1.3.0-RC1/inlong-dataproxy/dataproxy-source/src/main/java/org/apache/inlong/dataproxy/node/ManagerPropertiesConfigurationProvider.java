/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.node;

import java.util.HashMap;
import java.util.Map;

import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.node.AbstractConfigurationProvider;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * ManagerPropertiesConfigurationProvider
 */
public class ManagerPropertiesConfigurationProvider extends
        AbstractConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ManagerPropertiesConfigurationProvider.class);

    /**
     * ManagerPropertiJesConfigurationProvider
     * 
     * @param agentName
     */
    public ManagerPropertiesConfigurationProvider(String agentName) {
        super(agentName);
    }

    /**
     * getFlumeConfiguration
     * 
     * @return
     */
    @Override
    public FlumeConfiguration getFlumeConfiguration() {
        try {
            Map<String, String> flumeProperties = RemoteConfigManager.getInstance().getFlumeProperties();
            LOGGER.info("flumeProperties:{}", flumeProperties);
            return new FlumeConfiguration(flumeProperties);
        } catch (Exception e) {
            LOGGER.error("exception catch:" + e.getMessage(), e);
        }
        return new FlumeConfiguration(new HashMap<String, String>());
    }
}