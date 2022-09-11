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

import java.util.ArrayList;
import java.util.List;

/**
 * CacheClusterSet
 */
public class CacheClusterSetObject {
    private String setName;
    private String type;
    private List<CacheClusterObject> cacheClusters = new ArrayList<>();
    private List<CacheTopicObject> topics = new ArrayList<>();

    /**
     * get setName
     * 
     * @return the setName
     */
    public String getSetName() {
        return setName;
    }

    /**
     * set setName
     * 
     * @param setName the setName to set
     */
    public void setSetName(String setName) {
        this.setName = setName;
    }

    /**
     * get type
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * set type
     * 
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * get cacheClusters
     * 
     * @return the cacheClusters
     */
    public List<CacheClusterObject> getCacheClusters() {
        return cacheClusters;
    }

    /**
     * set cacheClusters
     * 
     * @param cacheClusters the cacheClusters to set
     */
    public void setCacheClusters(List<CacheClusterObject> cacheClusters) {
        this.cacheClusters = cacheClusters;
    }

    /**
     * get topics
     * 
     * @return the topics
     */
    public List<CacheTopicObject> getTopics() {
        return topics;
    }

    /**
     * set topics
     * 
     * @param topics the topics to set
     */
    public void setTopics(List<CacheTopicObject> topics) {
        this.topics = topics;
    }

}
