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

package org.apache.inlong.sdk.sort.interceptor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.sdk.sort.api.Interceptor;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The sdk interceptor that use to filter messages do not in the time interval.
 */
public class MsgTimeInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(MsgTimeInterceptor.class);
    private long startTime;
    private long stopTime;

    public MsgTimeInterceptor() {
    }

    @Override
    public List<InLongMessage> intercept(List<InLongMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return new ArrayList<>(0);
        }
        return messages.stream()
                .filter(msg -> isValidMsgTime(msg.getMsgTime()))
                .collect(Collectors.toList());
    }

    @Override
    public void configure(InLongTopic inLongTopic) {
        startTime = TimeUtil.parseStartTime(inLongTopic);
        stopTime = TimeUtil.parseStopTime(inLongTopic);
        logger.info("start to config MsgTimeInterceptor, start time is {}, stop time is {}", startTime, stopTime);
    }

    private boolean isValidMsgTime(long msgTime) {
        return msgTime >= startTime && msgTime <= stopTime;
    }

}
