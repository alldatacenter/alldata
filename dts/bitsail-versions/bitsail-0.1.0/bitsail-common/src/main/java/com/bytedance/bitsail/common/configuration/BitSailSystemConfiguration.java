/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.configuration;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * Created 2022/7/6
 */
public class BitSailSystemConfiguration implements Serializable {
  public static final String BITSAIL_ENV_CONF_NAME = "BITSAIL_CONF_NAME";
  public static final String DEFAULT_SYSTEM_OPTIONAL_KEY = "sys";
  static final String BITSAIL_CONF_NAME = "bitsail.conf";
  private static final Logger LOG = LoggerFactory.getLogger(BitSailSystemConfiguration.class);
  private static final BitSailConfiguration DEFAULT_SYSTEM_CONFIGURATION;
  private static final String BITSAIL_ENV_CONF_DIR = "BITSAIL_CONF_DIR";
  private static final String CURRENT_DIR = "./conf";
  private static final String PARENT_DIR = "../conf";
  private static final String RUNTIME_FILE_DIR = "../runtime_files/conf";

  static {
    BitSailConfiguration defaultSystemConfiguration = BitSailConfiguration
        .newDefault();
    defaultSystemConfiguration.set(DEFAULT_SYSTEM_OPTIONAL_KEY, Maps.newHashMap());
    DEFAULT_SYSTEM_CONFIGURATION = defaultSystemConfiguration;
  }

  public static BitSailConfiguration loadSysConfiguration() {
    return loadSysConfiguration(getConfigDirectory());
  }

  public static BitSailConfiguration loadSysConfiguration(String configDir) {
    BitSailConfiguration sysConfiguration = BitSailConfiguration.newDefault();

    String confName = System.getenv(BITSAIL_ENV_CONF_NAME);
    final String finalConfName = StringUtils.isNotEmpty(confName) ? confName
        : BITSAIL_CONF_NAME;

    final File absoluteConfFile = new File(configDir, finalConfName);
    if (!absoluteConfFile.exists()) {
      LOG.warn("System configuration file: {} is not exists.", absoluteConfFile);
      return DEFAULT_SYSTEM_CONFIGURATION;
    }

    Config config = ConfigFactory
        .parseFile(absoluteConfFile)
        .resolve(ConfigResolveOptions.defaults())
        .getConfig("BITSAIL");

    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      sysConfiguration.set(entry.getKey(), entry.getValue().unwrapped());
    }

    return sysConfiguration;
  }

  private static String getConfigDirectory() {
    String confDir = System.getenv(BITSAIL_ENV_CONF_DIR);

    if (confDir != null) {
      if (new File(confDir).exists()) {
        return confDir;
      } else {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
            String.format("System configuration not exists, the environment: %s.", confDir));
      }
    } else if (new File(CURRENT_DIR).exists()) {
      confDir = CURRENT_DIR;
    } else if (new File(PARENT_DIR).exists()) {
      confDir = PARENT_DIR;
    } else if (new File(RUNTIME_FILE_DIR).exists()) {
      confDir = RUNTIME_FILE_DIR;
    }
//    else {
//      throw BITSAILException.asBITSAILException(CommonErrorCode.CONFIG_ERROR, "No system configuration found.");
//
//    }
    return confDir;
  }

}
