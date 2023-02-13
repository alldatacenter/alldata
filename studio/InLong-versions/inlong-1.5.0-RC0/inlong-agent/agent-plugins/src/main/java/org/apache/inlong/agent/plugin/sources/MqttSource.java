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

package org.apache.inlong.agent.plugin.sources;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.MqttReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MqttSource extends AbstractSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSource.class);

    private static final String JOB_MQTTJOB_PARAM_PREFIX = "job.mqttJob.";

    private static final String JOB_MQTTJOB_SERVERURI = "";

    private static final String JOB_MQTTJOB_CLIENTID = "";

    public static final String JOB_MQTTJOB_TOPICS = "job.mqttJob.topics";

    public MqttSource() {
    }

    private List<Reader> splitSqlJob(String topics) {
        if (StringUtils.isEmpty(topics)) {
            return null;
        }
        final List<Reader> result = new ArrayList<>();
        String[] topicList = topics.split(CommonConstants.COMMA);
        if (Objects.nonNull(topicList)) {
            Arrays.stream(topicList).forEach(topic -> {
                result.add(new MqttReader(topic));
            });
        }
        return result;
    }

    @Override
    public List<Reader> split(JobProfile conf) {
        super.init(conf);
        String topics = conf.get(JOB_MQTTJOB_TOPICS, StringUtils.EMPTY);
        List<Reader> readerList = null;
        if (StringUtils.isNotEmpty(topics)) {
            readerList = splitSqlJob(topics);
        }
        if (CollectionUtils.isNotEmpty(readerList)) {
            sourceMetric.sourceSuccessCount.incrementAndGet();
        } else {
            sourceMetric.sourceFailCount.incrementAndGet();
        }
        return readerList;
    }
}
