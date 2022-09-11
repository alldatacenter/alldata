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
 * CacheCluster
 */
public class CacheClusterObject {
    private String name;
    private String zone;
    private Map<String, String> params = new HashMap<>();

    /**
     * get name
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * set name
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get zone
     * 
     * @return the zone
     */
    public String getZone() {
        return zone;
    }

    /**
     * set zone
     * 
     * @param zone the zone to set
     */
    public void setZone(String zone) {
        this.zone = zone;
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
