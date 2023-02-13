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

package org.apache.inlong.agent.plugin.sources.snapshot;

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * MongoDBSnapshotBase : mongo snapshot
 */
public class MongoDBSnapshotBase extends AbstractSnapshot {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBSnapshotBase.class);
    /**
     * agent configuration
     */
    private static final AgentConfiguration AGENT_CONFIGURATION = AgentConfiguration.getAgentConf();
    /**
     * snapshot file
     */
    private final File file;

    public MongoDBSnapshotBase(String filePath) {
        file = new File(filePath);
    }

    @Override
    public String getSnapshot() {
        return ENCODER.encodeToString(this.load(file));
    }

    @Override
    public void close() {

    }

    public static String getSnapshotFilePath() {
        String historyPath = AGENT_CONFIGURATION.get(
                AgentConstants.AGENT_HISTORY_PATH, AgentConstants.DEFAULT_AGENT_HISTORY_PATH);
        String parentPath = AGENT_CONFIGURATION.get(
                AgentConstants.AGENT_HOME, AgentConstants.DEFAULT_AGENT_HOME);
        return AgentUtils.makeDirsIfNotExist(historyPath, parentPath).getAbsolutePath();
    }
}
