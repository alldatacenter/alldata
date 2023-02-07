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

package org.apache.inlong.agent.conf;

import org.apache.commons.io.FileUtils;
import org.apache.inlong.agent.constant.AgentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * agent configuration. Only one instance in the process.
 * Basically it use properties file to store configurations.
 */
public class AgentConfiguration extends AbstractConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfiguration.class);

    public static final String DEFAULT_CONFIG_FILE = "agent.properties";
    public static final String TMP_CONFIG_FILE = ".tmp.agent.properties";

    private static final ArrayList<String> LOCAL_RESOURCES = new ArrayList<>();

    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static volatile AgentConfiguration agentConf = null;

    static {
        LOCAL_RESOURCES.add(DEFAULT_CONFIG_FILE);
    }

    /**
     * load config from agent file.
     */
    private AgentConfiguration() {
        for (String fileName : LOCAL_RESOURCES) {
            super.loadPropertiesResource(fileName);
        }
    }

    /**
     * singleton for agent configuration.
     *
     * @return static instance of AgentConfiguration
     */
    public static AgentConfiguration getAgentConf() {
        if (agentConf == null) {
            synchronized (AgentConfiguration.class) {
                if (agentConf == null) {
                    agentConf = new AgentConfiguration();
                }
            }
        }
        return agentConf;
    }

    private String getNextBackupFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = format.format(new Date(System.currentTimeMillis()));
        return DEFAULT_CONFIG_FILE + "." + dateStr;
    }

    /**
     * flush config to local files.
     */
    public void flushToLocalPropertiesFile() {
        LOCK.writeLock().lock();
        // TODO: flush to local file as properties file.
        try {
            String agentConfParent = get(
                    AgentConstants.AGENT_CONF_PARENT, AgentConstants.DEFAULT_AGENT_CONF_PARENT);
            File sourceFile = new File(agentConfParent, DEFAULT_CONFIG_FILE);
            File targetFile = new File(agentConfParent, getNextBackupFileName());
            File tmpFile = new File(agentConfParent, TMP_CONFIG_FILE);
            if (sourceFile.exists()) {
                FileUtils.copyFile(sourceFile, targetFile);
            }
            List<String> tmpCache = getStorageList();
            FileUtils.writeLines(tmpFile, tmpCache);

            FileUtils.copyFile(tmpFile, sourceFile);
            boolean result = tmpFile.delete();
            if (!result) {
                LOGGER.warn("cannot delete file {}", tmpFile);
            }
        } catch (Exception ex) {
            LOGGER.error("error while flush agent conf to local", ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * refresh config from local files.
     * Note: there is a concurrency issue when hot-updating and fetching configuration
     */
    public void reloadFromLocalPropertiesFile() {
        for (String fileName : LOCAL_RESOURCES) {
            super.loadPropertiesResource(fileName);
        }
    }

    @Override
    public boolean allRequiredKeyExist() {
        return true;
    }
}
