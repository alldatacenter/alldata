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

package org.apache.ranger.authorization.hadoop.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerChainedPluginConfig extends RangerPluginConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RangerChainedPluginConfig.class);

    private final String[] legacySSLProperties           = new String[] {"xasecure.policymgr.clientssl.keystore", "xasecure.policymgr.clientssl.keystore.type", "xasecure.policymgr.clientssl.keystore.credential.file","xasecure.policymgr.clientssl.truststore", "xasecure.policymgr.clientssl.truststore.credential.file", "hadoop.security.credential.provider.path"};
    private final String[] chainedPluginPropertyPrefixes = new String[] { ".chained.services"};

    public RangerChainedPluginConfig(String serviceType, String serviceName, String appId, RangerPluginConfig sourcePluginConfig) {
        super(serviceType, serviceName, appId, sourcePluginConfig);

        // Copy all of properties from sourcePluginConfig except chained properties but with converted propertyPrefix
        copyProperties(sourcePluginConfig, sourcePluginConfig.getPropertyPrefix());

        // Copy SSL configurations from sourcePluginConfig
        copyLegacySSLProperties(sourcePluginConfig);

        // Override copied properties from those in sourcePluginConfig with getPropertyPrefix()
        copyProperties(sourcePluginConfig, getPropertyPrefix());

        // Copy chained properties
        copyChainedProperties(sourcePluginConfig, getPropertyPrefix());

        set(getPropertyPrefix() + ".service.name", serviceName);
    }

    private void copyProperties(RangerPluginConfig sourcePluginConfig, String propertyPrefix) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> copyProperties: propertyPrefix:[" + propertyPrefix + "]");
        }
        for (String propName : sourcePluginConfig.getProperties().stringPropertyNames()) {
            String value = sourcePluginConfig.get(propName);

            if (value != null && propName.startsWith(propertyPrefix)) {
                String suffix = propName.substring(propertyPrefix.length());
                if (!isExcludedSuffix(suffix)) {
                    set(getPropertyPrefix() + suffix, value);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("set property:[" + getPropertyPrefix() + suffix + "] to value:[" + value + "]");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Not copying property :[" + propName + "] value from sourcePluginConfig");
                    }
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== copyProperties: propertyPrefix:[" + propertyPrefix + "]");
        }
    }

    private void copyLegacySSLProperties(RangerPluginConfig sourcePluginConfig) {
        for (String legacyPropertyName : legacySSLProperties) {
            String value = sourcePluginConfig.get(legacyPropertyName);
            if (value != null) {
                set(legacyPropertyName, value);
            }
        }
    }

    private void copyChainedProperties(RangerPluginConfig sourcePluginConfig, String propertyPrefix) {
        for (String propName : sourcePluginConfig.getProperties().stringPropertyNames()) {
            String value = sourcePluginConfig.get(propName);

            if (value != null && propName.startsWith(propertyPrefix)) {
                String suffix = propName.substring(propertyPrefix.length());
                for (String chainedPropertyPrefix : chainedPluginPropertyPrefixes) {
                    if (StringUtils.startsWith(suffix, chainedPropertyPrefix)) {
                        set(getPropertyPrefix() + suffix, value);
                    }
                }
            }
        }
    }

    private boolean isExcludedSuffix(String suffix) {
        for (String excludedSuffix : chainedPluginPropertyPrefixes) {
            if (StringUtils.startsWith(suffix, excludedSuffix)) {
                return true;
            }
        }
        return false;
    }

    private String printProperties() {
        StringBuilder sb = new StringBuilder();
        boolean seenOneProp = false;
        for (String propName : this.getProperties().stringPropertyNames()) {
            String value = this.get(propName);
            if (!seenOneProp) {
                seenOneProp = true;
            } else {
                sb.append(",\n");
            }
            sb.append("{ propertyName:[").append(propName).append("], propertyValue:[").append(value).append("] }");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : { " + printProperties() + " }";
    }
}
