/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.HashMap;
import java.util.Map;

/**
 * InLongId
 */
public class InLongIdObject {
    private String inlongId;
    private String topic;
    private Map<String, String> params = new HashMap<>();

    /**
     * get inlongId
     * 
     * @return the inlongId
     */
    public String getInlongId() {
        return inlongId;
    }

    /**
     * set inlongId
     * 
     * @param inlongId the inlongId to set
     */
    public void setInlongId(String inlongId) {
        this.inlongId = inlongId;
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
     * get params
     * 
     * @return the params
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * set params
     * 
     * @param params the params to set
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

}
