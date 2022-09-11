/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flume.sink.tubemq;

import static org.apache.flume.sink.tubemq.ConfigOptions.TOPIC;

import java.util.Map;
import org.apache.flume.Event;

/**
 * Event with retry time
 */
public class EventStat {
    private final Event event;
    private int retryCnt;
    private String topic;

    public EventStat(Event event) {
        this.event = event;
        this.retryCnt = 0;
        Map<String, String> headers = event.getHeaders();
        if (headers != null && headers.containsKey(TOPIC)) {
            this.topic = headers.get(TOPIC);
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String defaultTopic) {
        this.topic = defaultTopic;
    }

    public Event getEvent() {
        return event;
    }

    public int getRetryCnt() {
        return retryCnt;
    }

    public void incRetryCnt() {
        this.retryCnt++;
    }
}
