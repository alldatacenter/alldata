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

package org.apache.inlong.manager.pojo.dataproxy;

/**
 * InlongStreamId
 */
public class InlongStreamId {

    private String inlongGroupId;
    private String inlongStreamId;
    private String topic;
    private String extParams;

    /**
     * get inlongGroupId
     *
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * set inlongGroupId
     *
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    /**
     * get inlongStreamId
     *
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * set inlongStreamId
     *
     * @param inlongStreamId the inlongStreamId to set
     */
    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }

    /**
     * get topic
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * set topic
     *
     * @param topic the topic to set
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * get extParams
     *
     * @return the extParams
     */
    public String getExtParams() {
        return extParams;
    }

    /**
     * set extParams
     *
     * @param extParams the extParams to set
     */
    public void setExtParams(String extParams) {
        this.extParams = extParams;
    }

}
