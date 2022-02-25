/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.metrics.system.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsConfiguration {
  public static final String CONFIG_FILE = "metrics.properties";

  private static final Logger LOG = LoggerFactory.getLogger(MetricsConfiguration.class);
  private Properties properties;

  public static MetricsConfiguration getMetricsConfiguration() {
    Properties properties = readConfigFile();
    if (properties == null || properties.isEmpty()) {
      return null;
    }
    return new MetricsConfiguration(properties);
  }

  public MetricsConfiguration(Properties properties) {
    this.properties = properties;
  }

  private static Properties readConfigFile() {
    Properties properties = new Properties();

    //Get property file stream from classpath
    InputStream inputStream = MetricsConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

    if (inputStream == null) {
      LOG.info(CONFIG_FILE + " not found in classpath");
      return null;
    }

    // load the properties
    try {
      properties.load(inputStream);
      inputStream.close();
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.");
      return null;
    } catch (IOException ie) {
      LOG.error("Can't read configuration file " + CONFIG_FILE, ie);
      return null;
    }

    return properties;
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public Properties getProperties() {
    return properties;
  }

  /**
   * Get
   *
   * @param metricsConfiguration
   * @param prefix
   * @return subset configuration which contains the Key-Value pairs whose keys start with the passed in prefix.
   */
  public static MetricsConfiguration getSubsetConfiguration(MetricsConfiguration metricsConfiguration, String prefix) {

    if (null == metricsConfiguration) {
      return null;
    }

    Properties properties = metricsConfiguration.getProperties();
    if (null == properties || StringUtils.isEmpty(prefix)) {
      return new MetricsConfiguration(properties);
    }

    Properties subsetProperties = new Properties();

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(prefix)) {
        key = key.substring(prefix.length());
        subsetProperties.put(key, val);
      }
    }

    return new MetricsConfiguration(subsetProperties);
  }
}
