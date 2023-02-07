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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;

import java.util.List;

import static org.apache.inlong.agent.constant.JobConstants.JOB_MQ_ClUSTERS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MQ_TOPIC;

/**
 * job profile which contains details describing properties of one job.
 */
public class JobProfile extends AbstractConfiguration {

    private static final Gson GSON = new Gson();

    /**
     * parse json string to configuration instance.
     *
     * @return job configuration
     */
    public static JobProfile parseJsonStr(String jsonStr) {
        JobProfile conf = new JobProfile();
        conf.loadJsonStrResource(jsonStr);
        return conf;
    }

    /**
     * parse properties file
     *
     * @param fileName file name.
     * @return jobConfiguration.
     */
    public static JobProfile parsePropertiesFile(String fileName) {
        JobProfile conf = new JobProfile();
        conf.loadPropertiesResource(fileName);
        return conf;
    }

    /**
     * parse json file.
     *
     * @param fileName json file name.
     * @return jobConfiguration.
     */
    public static JobProfile parseJsonFile(String fileName) {
        JobProfile conf = new JobProfile();
        conf.loadJsonResource(fileName);
        return conf;
    }

    /**
     * check whether required keys exists.
     *
     * @return return true if all required keys exists else false.
     */
    @Override
    public boolean allRequiredKeyExist() {
        return hasKey(JobConstants.JOB_ID) && hasKey(JobConstants.JOB_SOURCE_CLASS)
                && hasKey(JobConstants.JOB_SINK) && hasKey(JobConstants.JOB_CHANNEL)
                && hasKey(JobConstants.JOB_GROUP_ID) && hasKey(JobConstants.JOB_STREAM_ID);
    }

    public String toJsonStr() {
        return GSON.toJson(getConfigStorage());
    }

    public String getInstanceId() {
        return get(JobConstants.JOB_INSTANCE_ID);
    }

    /**
     * get MQClusterInfo list from config
     */
    public List<MQClusterInfo> getMqClusters() {
        List<MQClusterInfo> result = null;
        String mqClusterStr = get(JOB_MQ_ClUSTERS);
        if (StringUtils.isNotBlank(mqClusterStr)) {
            result = GSON.fromJson(mqClusterStr, new TypeToken<List<MQClusterInfo>>() {
            }.getType());
        }
        return result;
    }

    /**
     * get mqTopic from config
     */
    public DataProxyTopicInfo getMqTopic() {
        DataProxyTopicInfo result = null;
        String topicStr = get(JOB_MQ_TOPIC);
        if (StringUtils.isNotBlank(topicStr)) {
            result = GSON.fromJson(topicStr, DataProxyTopicInfo.class);
        }
        return result;
    }
}
