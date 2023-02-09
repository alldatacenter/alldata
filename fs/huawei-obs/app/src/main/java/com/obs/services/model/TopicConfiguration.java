/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.List;

/**
 * Event notification configuration
 *
 */
public class TopicConfiguration extends AbstractNotification {

    public TopicConfiguration() {

    }

    /**
     * Constructor
     * 
     * @param id
     *            Event notification configuration ID
     * @param filter
     *            Filtering rule group
     * @param topic
     *            URN of the event notification topic
     * @param events
     *            List of the event types that require the release of
     *            notification messages
     */
    public TopicConfiguration(String id, Filter filter, String topic, List<EventTypeEnum> events) {
        super(id, filter, events);
        this.topic = topic;

    }

    private String topic;

    /**
     * Obtain the URN of the event notification topic.
     * 
     * @return URN of the event notification topic
     * 
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Set the URN of the event notification topic.
     * 
     * @param topic
     *            URN of the event notification topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "TopicConfiguration [id=" + id + ", topic=" + topic + ", events=" + events + ", filter=" + filter + "]";
    }

}
