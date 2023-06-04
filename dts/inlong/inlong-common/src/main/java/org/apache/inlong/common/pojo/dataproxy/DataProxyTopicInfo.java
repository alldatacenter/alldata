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

package org.apache.inlong.common.pojo.dataproxy;

import org.apache.commons.lang3.StringUtils;

/**
 * Topic info for DataProxy, includes the topic name and the inlongGroupId to which it belongs.
 */
public class DataProxyTopicInfo {

    /**
     * The topic name that needs to send data
     */
    private String topic;

    /**
     * The inlongGroupId to which the topic belongs
     */
    private String inlongGroupId;

    /**
     * The data format, will deprecate in the future
     */
    @Deprecated
    private String m;

    public DataProxyTopicInfo() {
    }

    public DataProxyTopicInfo(String topic, String inlongGroupId) {
        this(topic, inlongGroupId, null);
    }

    public DataProxyTopicInfo(String topic, String inlongGroupId, String m) {
        this.topic = topic;
        this.inlongGroupId = inlongGroupId;
        this.m = m;
    }

    @Override
    public String toString() {
        return "DataProxyTopicInfo{topic='" + topic + '\''
                + ", inlongGroupId='" + inlongGroupId + '\''
                + ", m='" + m + '\''
                + '}';
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getInlongGroupId() {
        return inlongGroupId;
    }

    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(inlongGroupId) && StringUtils.isNotBlank(topic);
    }

}
