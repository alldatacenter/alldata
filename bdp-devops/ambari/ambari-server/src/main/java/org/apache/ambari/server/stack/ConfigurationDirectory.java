/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.stack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.namespace.QName;

import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.utils.JsonUtils;
import org.apache.ambari.server.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

/**
 * Encapsulates IO operations on a stack definition configuration directory.
 */
public class ConfigurationDirectory extends StackDefinitionDirectory {
  /**
   * Used to unmarshal a stack definition configuration to an object representation
   */
  private static ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * Map of configuration type to configuration module.
   * One entry for each configuration file in this configuration directory.
   */
  private Map<String, ConfigurationModule> configurationModules = new HashMap<>();

  /**
   * Logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ConfigurationDirectory.class);

  /**
   * Properties directory
   */
  protected File propertiesDirFile;

  /**
   * Constructor.
   *
   * @param directoryName  configuration directory name
   */
  public ConfigurationDirectory(String directoryName, String propertiesDirectoryName) {
    super(directoryName);
    if(!StringUtils.isBlank(propertiesDirectoryName)) {
      propertiesDirFile = new File(propertiesDirectoryName);
    }
    parsePath();
  }

  /**
   * Obtain a collection of of configuration modules representing each configuration
   * file contained in this configuration directory.
   *
   * @return collection of configuration modules
   */
  public Collection<ConfigurationModule> getConfigurationModules() {
    return configurationModules.values();
  }

  /**
   * Parse the configuration directory.
   */
  private void parsePath() {
    File[] configFiles = directory.listFiles(StackDirectory.FILENAME_FILTER);
    if (configFiles != null) {
      for (File configFile : configFiles) {
        if (configFile.getName().endsWith(StackDirectory.SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
          String configType = ConfigHelper.fileNameToConfigType(configFile.getName());
          ConfigurationXml config = null;
          try {
            config = unmarshaller.unmarshal(ConfigurationXml.class, configFile);
            ConfigurationInfo configInfo = new ConfigurationInfo(parseProperties(config,
                configFile.getName()), parseAttributes(config));
            ConfigurationModule module = new ConfigurationModule(configType, configInfo);
            configurationModules.put(configType, module);
          } catch (Exception e) {
            String error = null;
            if (e instanceof JAXBException || e instanceof UnmarshalException || e instanceof SAXParseException) {
              error = "Could not parse XML " + configFile + ": " + e;
            } else {
              error = "Could not load configuration for " + configFile;
            }
            config = new ConfigurationXml();
            config.setValid(false);
            config.addError(error);
            ConfigurationInfo configInfo = new ConfigurationInfo(parseProperties(config,
                configFile.getName()), parseAttributes(config));
            configInfo.setValid(false);
            configInfo.addError(error);
            ConfigurationModule module = new ConfigurationModule(configType, configInfo);
            configurationModules.put(configType, module);
          }
        }
      }
    }
  }

  /**
   * Parse a configurations properties.
   *
   * @param configuration  object representation of a configuration file
   * @param fileName       configuration file name
   *
   * @return  collection of properties
   */
  private Collection<PropertyInfo> parseProperties(ConfigurationXml configuration, String fileName) {
  List<PropertyInfo> props = new ArrayList<>();
  for (PropertyInfo pi : configuration.getProperties()) {
    pi.setFilename(fileName);
    if(pi.getPropertyTypes().contains(PropertyInfo.PropertyType.VALUE_FROM_PROPERTY_FILE)) {
      if(propertiesDirFile != null && propertiesDirFile.exists() && propertiesDirFile.isDirectory() ) {
        String propertyFileName = pi.getPropertyValueAttributes().getPropertyFileName();
        String propertyFileType = pi.getPropertyValueAttributes().getPropertyFileType();
        String propertyFilePath = propertiesDirFile.getAbsolutePath() + File.separator + propertyFileName;
        File propertyFile = new File(propertyFilePath);
        if (propertyFile.exists() && propertyFile.isFile()) {
          try {
            String propertyValue =
                    FileUtils.readFileToString(propertyFile, Charset.defaultCharset());
            boolean valid = true;
            switch (propertyFileType.toLowerCase()) {
              case "xml" :
                if (!XmlUtils.isValidXml(propertyValue)) {
                  valid = false;
                  LOG.error("Failed to load value from property file. Property file {} is not a valid XML file", propertyFilePath);
                }
                break;
              case "json":
                if (!JsonUtils.isValidJson(propertyValue)) {
                  valid = false;
                  LOG.error("Failed to load value from property file. Property file {} is not a valid JSON file", propertyFilePath);
                }
                break;
              case "text":
                // fallthrough
              default:
                // no validity check
                break;
            }
            if (valid) {
              pi.setValue(propertyValue);
            }
          } catch (IOException e) {
            LOG.error("Failed to load value from property file {}. Error Message {}", propertyFilePath, e.getMessage());
          }
        } else {
          LOG.error("Failed to load value from property file. Properties file {} does not exist", propertyFilePath);
        }
      } else {
        if (propertiesDirFile == null) {
          LOG.error("Failed to load value from property file. Properties directory is null");
        } else {
          LOG.error("Failed to load value from property file. Properties directory {} does not exist", propertiesDirFile.getAbsolutePath());
        }
      }
    }
    props.add(pi);
  }
  return props; }

  /**
   * Parse a configurations type attributes.
   *
   * @param configuration  object representation of a configuration file
   *
   * @return  collection of attributes for the configuration type
   */
  private Map<String, String> parseAttributes(ConfigurationXml configuration) {
    Map<String, String> attributes = new HashMap<>();
    for (Map.Entry<QName, String> attribute : configuration.getAttributes().entrySet()) {
      attributes.put(attribute.getKey().getLocalPart(), attribute.getValue());
    }
    return attributes;
  }
}
