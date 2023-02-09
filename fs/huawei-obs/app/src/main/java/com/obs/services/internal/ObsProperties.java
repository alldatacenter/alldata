/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

import java.io.Serializable;
import java.util.Properties;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;

public class ObsProperties implements Serializable {

    private static final long serialVersionUID = -822234326095333142L;

    private static final ILogger LOG = LoggerBuilder.getLogger(ObsProperties.class);

    private final Properties properties = new Properties();

    public void setProperty(String propertyName, String propertyValue) {
        if (propertyValue == null) {
            this.clearProperty(propertyName);
        } else {
            this.properties.setProperty(propertyName, trim(propertyValue));
        }
    }

    public void clearProperty(String propertyName) {
        this.properties.remove(propertyName);
    }

    public void clearAllProperties() {
        this.properties.clear();
    }

    public String getStringProperty(String propertyName, String defaultValue) {
        String stringValue = trim(properties.getProperty(propertyName, defaultValue));
        if (LOG.isDebugEnabled() && !"httpclient.proxy-user".equals(propertyName)
                && !"httpclient.proxy-password".equals(propertyName)) {
            LOG.debug(propertyName + "=" + stringValue);
        }
        return stringValue;
    }

    public int getIntProperty(String propertyName, int defaultValue) throws NumberFormatException {
        String value = trim(properties.getProperty(propertyName, String.valueOf(defaultValue)));
        if (LOG.isDebugEnabled()) {
            LOG.debug(propertyName + "=" + value);
        }
        return Integer.parseInt(value);
    }

    public boolean getBoolProperty(String propertyName, boolean defaultValue) throws IllegalArgumentException {
        String boolValue = trim(properties.getProperty(propertyName, String.valueOf(defaultValue)));
        if (LOG.isDebugEnabled()) {
            LOG.debug(propertyName + "=" + boolValue);
        }

        if (!"true".equalsIgnoreCase(boolValue) && !"false".equalsIgnoreCase(boolValue)) {
            throw new IllegalArgumentException("Boolean value '" + boolValue + "' for obs property '" + propertyName
                    + "' must be 'true' or 'false' (case-insensitive)");
        }

        return Boolean.parseBoolean(boolValue);
    }

    public boolean containsKey(String propertyName) {
        return properties.containsKey(propertyName);
    }

    private static String trim(String str) {
        if (null == str) {
            return null;
        }
        return str.trim();
    }

}
