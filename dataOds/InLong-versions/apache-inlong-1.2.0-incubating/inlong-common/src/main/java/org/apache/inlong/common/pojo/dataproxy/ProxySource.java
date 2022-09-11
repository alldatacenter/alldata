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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataProxySource
 */
public class ProxySource {
    private String name;
    private String type;
    private String selectorType;
    private List<String> channels = new ArrayList<>();
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
     * get selectorType
     * 
     * @return the selectorType
     */
    public String getSelectorType() {
        return selectorType;
    }

    /**
     * set selectorType
     * 
     * @param selectorType the selectorType to set
     */
    public void setSelectorType(String selectorType) {
        this.selectorType = selectorType;
    }

    /**
     * get channels
     * 
     * @return the channels
     */
    public List<String> getChannels() {
        return channels;
    }

    /**
     * set channels
     * 
     * @param channels the channels to set
     */
    public void setChannels(List<String> channels) {
        this.channels = channels;
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
