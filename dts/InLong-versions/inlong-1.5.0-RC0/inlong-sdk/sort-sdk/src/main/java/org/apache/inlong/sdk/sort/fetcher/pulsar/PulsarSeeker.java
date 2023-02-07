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

package org.apache.inlong.sdk.sort.fetcher.pulsar;

import org.apache.inlong.sdk.sort.api.Seeker;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.util.TimeUtil;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pulsar seeker.
 */
public class PulsarSeeker implements Seeker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarSeeker.class);
    private long seekTime = -1;
    private Consumer<byte[]> consumer;
    private String topic;

    public PulsarSeeker(Consumer<byte[]> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void configure(InLongTopic inLongTopic) {
        seekTime = TimeUtil.parseStartTime(inLongTopic);
        topic = inLongTopic.getTopic();
        LOGGER.info("start to config pulsar seeker, topic is {}, seek time is {}", topic, seekTime);
    }

    @Override
    public void seek() {
        if (seekTime < 0) {
            return;
        }
        LOGGER.info("start to seek pulsar topic {}, seek time is {}", topic, seekTime);
        try {
            consumer.seek(seekTime);
        } catch (PulsarClientException e) {
            LOGGER.error("fail to seek, start time is {}, ex is {}", seekTime, e.getMessage(), e);
        }
    }

    @Override
    public long getSeekTime() {
        return seekTime;
    }
}
