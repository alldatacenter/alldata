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
 * DataProxyCluster
 */
public class ProxyClusterObject {
    private String name;
    private String setName;
    private String zone;
    private List<ProxyChannel> channels = new ArrayList<>();
    private List<InLongIdObject> inlongIds = new ArrayList<>();
    private List<ProxySource> sources = new ArrayList<>();
    private List<ProxySink> sinks = new ArrayList<>();

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
     * get channels
     * 
     * @return the channels
     */
    public List<ProxyChannel> getChannels() {
        return channels;
    }

    /**
     * set channels
     * 
     * @param channels the channels to set
     */
    public void setChannels(List<ProxyChannel> channels) {
        this.channels = channels;
    }

    /**
     * get inlongIds
     * 
     * @return the inlongIds
     */
    public List<InLongIdObject> getInlongIds() {
        return inlongIds;
    }

    /**
     * set inlongIds
     * 
     * @param inlongIds the inlongIds to set
     */
    public void setInlongIds(List<InLongIdObject> inlongIds) {
        this.inlongIds = inlongIds;
    }

    /**
     * get sources
     * 
     * @return the sources
     */
    public List<ProxySource> getSources() {
        return sources;
    }

    /**
     * set sources
     * 
     * @param sources the sources to set
     */
    public void setSources(List<ProxySource> sources) {
        this.sources = sources;
    }

    /**
     * get sinks
     * 
     * @return the sinks
     */
    public List<ProxySink> getSinks() {
        return sinks;
    }

    /**
     * set sinks
     * 
     * @param sinks the sinks to set
     */
    public void setSinks(List<ProxySink> sinks) {
        this.sinks = sinks;
    }

}
