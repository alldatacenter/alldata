/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.util;

import org.apache.inlong.manager.plugin.flink.dto.FlinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.apache.inlong.manager.plugin.flink.enums.Constants.ADDRESS;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.JOB_MANAGER_PORT;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.METRICS_AUDIT_PROXY_HOSTS;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.DRAIN;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.PARALLELISM;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.PORT;
import static org.apache.inlong.manager.plugin.flink.enums.Constants.SAVEPOINT_DIRECTORY;

/**
 * Configuration file for Flink, only one instance in the process.
 * Basically it used properties file to store.
 */
public class FlinkConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlinkConfiguration.class);

    private static final String DEFAULT_CONFIG_FILE = "flink-sort-plugin.properties";
    private static final String INLONG_MANAGER = "inlong-manager";

    private final FlinkConfig flinkConfig;

    /**
     * load config from flink file.
     */
    public FlinkConfiguration() throws Exception {
        String path = formatPath();
        flinkConfig = getFlinkConfigFromFile(path);
    }

    /**
     * fetch DEFAULT_CONFIG_FILE full path
     */
    private String formatPath() throws Exception {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        LOGGER.info("format first path {}", path);

        int index = path.indexOf(INLONG_MANAGER);
        if (index == -1) {
            throw new Exception(INLONG_MANAGER + " path not found in " + path);
        }

        path = path.substring(0, index);
        String confPath = path + INLONG_MANAGER + File.separator + "plugins" + File.separator + DEFAULT_CONFIG_FILE;
        File file = new File(confPath);
        if (!file.exists()) {
            String message = String.format("not found %s in path %s", DEFAULT_CONFIG_FILE, confPath);
            LOGGER.error(message);
            throw new Exception(message);
        }

        LOGGER.info("after format, {} located in {}", DEFAULT_CONFIG_FILE, confPath);
        return confPath;
    }

    /**
     * get flink config
     */
    public FlinkConfig getFlinkConfig() {
        return flinkConfig;
    }

    /**
     * parse properties
     */
    private FlinkConfig getFlinkConfigFromFile(String fileName) throws IOException {
        Properties properties = new Properties();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        properties.load(bufferedReader);
        FlinkConfig flinkConfig = new FlinkConfig();
        flinkConfig.setPort(Integer.valueOf(properties.getProperty(PORT)));
        flinkConfig.setAddress(properties.getProperty(ADDRESS));
        flinkConfig.setParallelism(Integer.valueOf(properties.getProperty(PARALLELISM)));
        flinkConfig.setSavepointDirectory(properties.getProperty(SAVEPOINT_DIRECTORY));
        flinkConfig.setJobManagerPort(Integer.valueOf(properties.getProperty(JOB_MANAGER_PORT)));
        flinkConfig.setDrain(Boolean.parseBoolean(properties.getProperty(DRAIN)));
        flinkConfig.setAuditProxyHosts(properties.getProperty(METRICS_AUDIT_PROXY_HOSTS));
        return flinkConfig;
    }

}
