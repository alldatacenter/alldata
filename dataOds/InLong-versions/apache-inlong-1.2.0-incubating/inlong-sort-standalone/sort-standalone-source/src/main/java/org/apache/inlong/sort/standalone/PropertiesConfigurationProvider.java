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

package org.apache.inlong.sort.standalone;

import java.util.HashMap;
import java.util.Map;

import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.node.AbstractConfigurationProvider;
import org.slf4j.Logger;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;

/**
 * 
 * PropertiesConfigurationProvider
 */
public class PropertiesConfigurationProvider extends
        AbstractConfigurationProvider {

    public static final Logger LOG = InlongLoggerFactory.getLogger(PropertiesConfigurationProvider.class);

    private final Map<String, String> flumeConf;

    /**
     * PropertiesConfigurationProvider
     * 
     * @param agentName
     * @param flumeConf
     */
    public PropertiesConfigurationProvider(String agentName, Map<String, String> flumeConf) {
        super(agentName);
        this.flumeConf = flumeConf;
    }

    /**
     * getFlumeConfiguration
     * 
     * @return
     */
    @Override
    public FlumeConfiguration getFlumeConfiguration() {
        try {
            return new FlumeConfiguration(flumeConf);
        } catch (Exception e) {
            LOG.error("exception catch:" + e.getMessage(), e);
        }
        return new FlumeConfiguration(new HashMap<String, String>());
    }
}