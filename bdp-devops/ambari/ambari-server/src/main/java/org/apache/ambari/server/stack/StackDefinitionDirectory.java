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
import java.io.FilenameFilter;

/**
 * Base stack definition directory.
 * Contains functionality common across directory types.
 */
public abstract class StackDefinitionDirectory {
  /**
   * xml filename filter
   */
  protected static final FilenameFilter XML_FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File folder, String fileName) {
      return fileName.toLowerCase().endsWith(".xml");
    }
  };

  public static final String CONFIG_UPGRADE_XML_FILENAME_PREFIX = "config-upgrade.xml";

  /**
   * underlying directory
   */
  protected File directory;


  /**
   * Constructor.
   *
   * @param directory  underlying directory
   */
  public StackDefinitionDirectory(String directory) {
    //todo: handle non-existent dir
    this.directory = new File(directory);
  }

  /**
   * Obtain the configuration sub-directory instance for the specified path.
   *
   * @param directoryName  name of the configuration directory
   * @return ConfigurationDirectory instance for the specified configuration directory name
   */
  public ConfigurationDirectory getConfigurationDirectory(String directoryName, String propertiesDirectoryName) {
    ConfigurationDirectory configDirectory = null;
    File configDirFile = new File(directory.getAbsolutePath() + File.separator + directoryName);
    File propertiesDirFile = new File(directory.getAbsolutePath() + File.separator + propertiesDirectoryName);
    if (configDirFile.exists() && configDirFile.isDirectory())  {
      if(propertiesDirFile.exists() && propertiesDirFile.isDirectory()) {
        configDirectory = new ConfigurationDirectory(configDirFile.getAbsolutePath(), propertiesDirFile.getAbsolutePath());
      } else {
        configDirectory = new ConfigurationDirectory(configDirFile.getAbsolutePath(), null);
      }
    }
    return configDirectory;
  }

  /**
   * Obtain the path for this directory instance.
   *
   * @return the path represented by this directory
   */
  public String getPath() {
    return directory.getPath();
  }

  /**
   * Obtain the absolute path for this directory instance.
   *
   * @return the absolute path represented by this directory
   */
  public String getAbsolutePath() {
    return directory.getAbsolutePath();
  }

  /**
   * Obtain the name of the directory.
   *
   * @return name of the directory
   */
  public String getName() {
    return directory.getName();
  }

  /**
   * Obtain the underlying directory.
   *
   * @return the underlying directory file
   */
  protected File getDirectory() {
    return directory;
  }
}
