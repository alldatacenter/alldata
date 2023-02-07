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

import com.google.gson.Gson;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.KafkaReader;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_LINE_FILTER;
import static org.apache.inlong.agent.constant.JobConstants.JOB_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_AUTO_COMMIT_OFFSET_RESET;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_BOOTSTRAP_SERVERS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_GROUP_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_OFFSET;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_PARTITION_OFFSET_DELIMITER;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_TOPIC;
import static org.apache.inlong.agent.constant.JobConstants.JOB_LINE_FILTER_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_OFFSET_DELIMITER;

/**
 * kafka source, split kafka source job into multi readers
 */
public class KafkaSource extends AbstractSource {

    public static final String JOB_KAFKA_AUTO_RESETE = "auto.offset.reset";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSource.class);
    private static final String JOB_KAFKAJOB_PARAM_PREFIX = "job.kafkaJob.";
    private static final String JOB_KAFKAJOB_WAIT_TIMEOUT = "job.kafkajob.wait.timeout";
    private static final String KAFKA_COMMIT_AUTO = "enable.auto.commit";
    private static final String KAFKA_DESERIALIZER_METHOD =
            "org.apache.kafka.common.serialization.ByteArrayDeserializer";
    private static final String KAFKA_KEY_DESERIALIZER = "key.deserializer";
    private static final String KAFKA_VALUE_DESERIALIZER = "value.deserializer";
    private static final String KAFKA_SESSION_TIMEOUT = "session.timeout.ms";
    private static final Gson gson = new Gson();
    private static AtomicLong metricsIndex = new AtomicLong(0);

    public KafkaSource() {
    }

    @Override
    public List<Reader> split(JobProfile conf) {
        super.init(conf);
        List<Reader> result = new ArrayList<>();
        String filterPattern = conf.get(JOB_LINE_FILTER_PATTERN, DEFAULT_JOB_LINE_FILTER);

        Properties props = new Properties();
        Map<String, String> map = gson.fromJson(conf.toJsonStr(), Map.class);
        props.put(JOB_KAFKA_BOOTSTRAP_SERVERS.replace(JOB_KAFKAJOB_PARAM_PREFIX, StringUtils.EMPTY),
                map.get(JOB_KAFKA_BOOTSTRAP_SERVERS));

        props.put(KAFKA_KEY_DESERIALIZER, KAFKA_DESERIALIZER_METHOD);
        props.put(KAFKA_VALUE_DESERIALIZER, KAFKA_DESERIALIZER_METHOD);
        // set offset
        props.put(KAFKA_COMMIT_AUTO, false);
        if (ObjectUtils.isNotEmpty(map.get(JOB_KAFKA_AUTO_COMMIT_OFFSET_RESET))) {
            props.put(JOB_KAFKA_AUTO_RESETE, map.get(JOB_KAFKA_AUTO_COMMIT_OFFSET_RESET));
        }
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        List<PartitionInfo> partitionInfoList = consumer.partitionsFor(conf.get(JOB_KAFKA_TOPIC));
        String allPartitionOffsets = map.get(JOB_KAFKA_OFFSET);
        Long offset = null;
        String[] partitionOffsets = null;
        if (StringUtils.isNotBlank(allPartitionOffsets)) {
            // example:0#110_1#666_2#222
            partitionOffsets = allPartitionOffsets.split(JOB_OFFSET_DELIMITER);
        }
        // set consumer session timeout
        props.put(KAFKA_SESSION_TIMEOUT, 30000);
        // spilt reader reduce to partition
        if (null != partitionInfoList) {
            for (PartitionInfo partitionInfo : partitionInfoList) {
                props.put(JOB_KAFKA_GROUP_ID.replace(JOB_KAFKAJOB_PARAM_PREFIX, StringUtils.EMPTY),
                        map.getOrDefault(JOB_KAFKA_GROUP_ID,
                                map.get(JOB_ID) + JOB_OFFSET_DELIMITER
                                        + "group" + partitionInfo.partition()));
                KafkaConsumer<String, byte[]> partitonConsumer = new KafkaConsumer<>(props);
                partitonConsumer.assign(Collections.singletonList(
                        new TopicPartition(partitionInfo.topic(), partitionInfo.partition())));
                // if get offset,consume from offset; if not,consume from 0
                if (partitionOffsets != null && partitionOffsets.length > 0) {
                    for (String partitionOffset : partitionOffsets) {
                        if (partitionOffset.contains(JOB_KAFKA_PARTITION_OFFSET_DELIMITER)
                                && partitionOffset.split(JOB_KAFKA_PARTITION_OFFSET_DELIMITER)[0]
                                        .equals(String.valueOf(partitionInfo.partition()))) {
                            offset = Long.valueOf(partitionOffset.split(JOB_KAFKA_PARTITION_OFFSET_DELIMITER)[1]);
                        }
                    }
                }
                LOGGER.info("kafka topic partition offset:{}", offset);
                if (offset != null) {
                    // if offset not null,then consume from the offset
                    partitonConsumer.seek(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()), offset);
                }
                KafkaReader<String, byte[]> kafkaReader = new KafkaReader<>(partitonConsumer, map);
                addValidator(filterPattern, kafkaReader);
                result.add(kafkaReader);
            }
            sourceMetric.sourceSuccessCount.incrementAndGet();
        } else {
            sourceMetric.sourceFailCount.incrementAndGet();
        }
        return result;
    }

    private void addValidator(String filterPattern, KafkaReader kafkaReader) {
        kafkaReader.addPatternValidator(filterPattern);
    }
}
