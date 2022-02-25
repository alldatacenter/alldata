/**
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

package org.apache.ambari.view.pig.persistence;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

import java.io.File;

/**
 * Configuration enables all necessary options for PropertiesConfiguration:
 * auto-save, auto-reloading, no delimiter parsing and other
 */
@Deprecated
public class PersistentConfiguration extends PropertiesConfiguration {
  /**
   * Constructor
   * @param fileName path to data file
   * @throws ConfigurationException
   */
  public PersistentConfiguration(String fileName) throws ConfigurationException {
    super();

    File config = new File(fileName);
    setFile(config);
    this.setAutoSave(true);
    this.setReloadingStrategy(new FileChangedReloadingStrategy());
    this.setDelimiterParsingDisabled(true);
    this.setListDelimiter((char) 0);

    if (config.exists()) {
      this.load();
    }
  }
}
