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

package org.apache.inlong.agent.plugin.sources.reader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Validator;
import org.apache.inlong.agent.plugin.validator.PatternValidator;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_BYTE_SPEED_LIMIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_OFFSET;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_PARTITION_OFFSET_DELIMITER;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_RECORD_SPEED_LIMIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_KAFKA_TOPIC;

/**
 * read kafka data
 */
public class KafkaReader<K, V> extends AbstractReader {

    public static final int NEVER_STOP_SIGN = -1;
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReader.class);
    /* metric */
    private static final String KAFKA_READER_TAG_NAME = "AgentKafkaMetric";
    private static final String KAFKA_SOURCE_READ_RECORD_SPEED = "job.kafkaJob.record.speed.limit";
    private static final String KAFKA_SOURCE_READ_BYTE_SPEED = "job.kafkaJob.byte.speed.limit";
    private static final String KAFKA_SOURCE_READ_MIN_INTERVAL = "kafka.min.interval.limit";
    private static final String JOB_KAFKAJOB_READ_TIMEOUT = "job.kafkaJob.read.timeout";
    /* total readRecords */
    private static AtomicLong currentTotalReadRecords = new AtomicLong(0);
    private static AtomicLong lastTotalReadRecords = new AtomicLong(0);
    /* total readBytes */
    private static AtomicLong currentTotalReadBytes = new AtomicLong(0);
    private static AtomicLong lastTotalReadBytes = new AtomicLong(0);
    KafkaConsumer<K, V> consumer;
    long lastTimestamp;
    /* bps: records/s */
    long recordSpeed;
    /* tps: bytes/s */
    long byteSpeed;
    /* sleepTime */
    long flowControlInterval;
    private Iterator<ConsumerRecord<K, V>> iterator;
    private List<Validator> validators = new ArrayList<>();
    private long timeout;
    private long waitTimeout = 1000;
    private long lastTime = 0;
    private String inlongGroupId;
    private String inlongStreamId;
    private String snapshot;
    private boolean isFinished = false;
    private boolean destroyed = false;
    private String topic;

    /**
     * init attribute
     */
    public KafkaReader(KafkaConsumer<K, V> consumer, Map<String, String> paraMap) {
        this.consumer = consumer;
        this.recordSpeed = Long.parseLong(paraMap.getOrDefault(JOB_KAFKA_RECORD_SPEED_LIMIT, "10000"));
        this.byteSpeed = Long.parseLong(paraMap.getOrDefault(JOB_KAFKA_BYTE_SPEED_LIMIT, String.valueOf(1024 * 1024)));
        this.flowControlInterval = Long.parseLong(paraMap.getOrDefault(KAFKA_SOURCE_READ_MIN_INTERVAL, "1000"));
        this.lastTimestamp = System.currentTimeMillis();
        this.topic = paraMap.get(JOB_KAFKA_TOPIC);

        LOGGER.info("KAFKA_SOURCE_READ_RECORD_SPEED = {}", this.recordSpeed);
        LOGGER.info("KAFKA_SOURCE_READ_BYTE_SPEED = {}", this.byteSpeed);
    }

    @Override
    public Message read() {

        if (iterator != null && iterator.hasNext()) {
            ConsumerRecord<K, V> record = iterator.next();
            // body
            byte[] recordValue = (byte[]) record.value();
            if (validateMessage(recordValue)) {
                AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS,
                        inlongGroupId, inlongStreamId, System.currentTimeMillis(), 1, recordValue.length);
                // header
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("record.offset", String.valueOf(record.offset()));
                headerMap.put("record.key", String.valueOf(record.key()));
                LOGGER.debug(
                        "partition:" + record.partition()
                                + ", value:" + new String(recordValue) + ", offset:" + record.offset());
                // control speed
                readerMetric.pluginReadSuccessCount.incrementAndGet();
                readerMetric.pluginReadCount.incrementAndGet();
                // commit succeed,then record current offset
                snapshot = record.partition() + JOB_KAFKA_PARTITION_OFFSET_DELIMITER + record.offset();
                DefaultMessage message = new DefaultMessage(recordValue, headerMap);
                recordReadLimit(1L, message.getBody().length);
                return message;
            }
        } else {
            // commit offset
            if (isSourceExist()) {
                consumer.commitAsync();
            }
            fetchData(5000);
        }
        AgentUtils.silenceSleepInMs(waitTimeout);

        return null;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public String getReadSource() {
        Set<TopicPartition> assignment = consumer.assignment();
        // cousumer->topic->one partition
        Iterator<TopicPartition> iterator = assignment.iterator();
        while (iterator.hasNext()) {
            TopicPartition topicPartition = iterator.next();
            return topicPartition.topic() + "_" + topicPartition.partition();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public void setReadTimeout(long millis) {
        timeout = millis;
    }

    @Override
    public void setWaitMillisecond(long millis) {
        waitTimeout = millis;
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        // get offset from jobConf
        snapshot = jobConf.get(JOB_KAFKA_OFFSET, null);
        initReadTimeout(jobConf);
        // fetch data
        fetchData(5000);
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                consumer.close();
                destroyed = true;
            }
        }
    }

    private void initReadTimeout(JobProfile jobConf) {
        int waitTime = jobConf.getInt(JOB_KAFKAJOB_READ_TIMEOUT, NEVER_STOP_SIGN);
        if (waitTime == NEVER_STOP_SIGN) {
            timeout = NEVER_STOP_SIGN;
        } else {
            timeout = TimeUnit.MINUTES.toMillis(waitTime);
        }
    }

    private boolean validateMessage(byte[] message) {
        if (validators.isEmpty()) {
            return true;
        }
        return validators.stream().allMatch(v -> v.validate(new String(message)));
    }

    /**
     * add specified pattern to validators
     *
     * @param pattern specified pattern
     */
    public void addPatternValidator(String pattern) {
        if (pattern.isEmpty()) {
            return;
        }
        validators.add(new PatternValidator(pattern));
    }

    @Override
    public String getSnapshot() {
        return snapshot;
    }

    @Override
    public void finishRead() {
        isFinished = true;
    }

    @Override
    public boolean isSourceExist() {
        return !CollectionUtils.isEmpty(consumer.partitionsFor(topic));
    }

    private boolean fetchData(long fetchDataTimeout) {
        // cosume data
        ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(fetchDataTimeout));
        iterator = records.iterator();
        return iterator != null ? true : false;
    }

    private void recordReadLimit(long recordSize, long byteSize) {
        boolean isChannelByteSpeedLimit = (this.byteSpeed > 0);
        boolean isChannelRecordSpeedLimit = (this.recordSpeed > 0);
        if (!isChannelByteSpeedLimit && !isChannelRecordSpeedLimit) {
            return;
        }
        currentTotalReadRecords.accumulateAndGet(recordSize, (x, y) -> x + y);
        currentTotalReadBytes.accumulateAndGet(byteSize, (x, y) -> x + y);

        long nowTimestamp = System.currentTimeMillis();
        long interval = nowTimestamp - lastTimestamp;

        if (interval - this.flowControlInterval >= 0) {
            long byteLimitSleepTime = 0;
            long recordLimitSleepTime = 0;
            if (isChannelByteSpeedLimit) {
                long currentByteSpeed = (currentTotalReadBytes.get() - lastTotalReadBytes.get()) * 1000 / interval;
                LOGGER.info("current produce byte speed bytes/s:{}", currentByteSpeed);
                if (currentByteSpeed > this.byteSpeed) {
                    // calculate byteLimitSleepTime
                    byteLimitSleepTime = currentByteSpeed * interval / this.byteSpeed - interval;
                }
            }

            if (isChannelRecordSpeedLimit) {
                long currentRecordSpeed =
                        (currentTotalReadRecords.get() - lastTotalReadRecords.get()) * 1000 / interval;
                LOGGER.info("current read speed records/s:{}", currentRecordSpeed);
                if (currentRecordSpeed > this.recordSpeed) {
                    // calculate recordSleepTime reduce to recordLimit
                    recordLimitSleepTime = currentRecordSpeed * interval / this.recordSpeed - interval;
                }
            }
            // calculate sleep time
            long sleepTime = byteLimitSleepTime < recordLimitSleepTime
                    ? recordLimitSleepTime
                    : byteLimitSleepTime;
            if (sleepTime > 0) {
                LOGGER.info("sleep seconds:{}", sleepTime / 1000);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        lastTimestamp = nowTimestamp;
        lastTotalReadRecords = currentTotalReadRecords;
    }
}
