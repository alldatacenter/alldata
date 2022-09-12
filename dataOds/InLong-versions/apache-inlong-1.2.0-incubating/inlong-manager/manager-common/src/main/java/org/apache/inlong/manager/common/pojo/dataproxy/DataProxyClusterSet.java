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

package org.apache.inlong.manager.common.pojo.dataproxy;

import org.apache.inlong.common.pojo.dataproxy.CacheClusterSetObject;
import org.apache.inlong.common.pojo.dataproxy.InLongIdObject;
import org.apache.inlong.common.pojo.dataproxy.ProxyChannel;
import org.apache.inlong.common.pojo.dataproxy.ProxyClusterObject;
import org.apache.inlong.common.pojo.dataproxy.ProxySink;
import org.apache.inlong.common.pojo.dataproxy.ProxySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DataProxyClusterSet
 */
public class DataProxyClusterSet {

    private String setName;
    private CacheClusterSetObject cacheClusterSet = new CacheClusterSetObject();
    private List<ProxyClusterObject> proxyClusterList = new ArrayList<>();
    private Map<String, ProxyChannel> proxyChannelMap = new HashMap<>();
    private Map<String, ProxySource> proxySourceMap = new HashMap<>();
    private Map<String, ProxySink> proxySinkMap = new HashMap<>();
    private List<InLongIdObject> inlongIds = new ArrayList<>();
    private Map<String, Set<String>> proxy2Cache = new HashMap<>();
    //
    private String defaultConfigJson;
    // key: proxyClusterName, value: jsonString
    private Map<String, String> proxyConfigJson = new HashMap<>();
    // key: proxyClusterName, value: md5
    private Map<String, String> md5Map = new HashMap<>();

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
     * get cacheClusterSet
     *
     * @return the cacheClusterSet
     */
    public CacheClusterSetObject getCacheClusterSet() {
        return cacheClusterSet;
    }

    /**
     * set cacheClusterSet
     *
     * @param cacheClusterSet the cacheClusterSet to set
     */
    public void setCacheClusterSet(CacheClusterSetObject cacheClusterSet) {
        this.cacheClusterSet = cacheClusterSet;
    }

    /**
     * get proxyClusterList
     *
     * @return the proxyClusterList
     */
    public List<ProxyClusterObject> getProxyClusterList() {
        return proxyClusterList;
    }

    /**
     * set proxyClusterList
     *
     * @param proxyClusterList the proxyClusterList to set
     */
    public void setProxyClusterList(List<ProxyClusterObject> proxyClusterList) {
        this.proxyClusterList = proxyClusterList;
    }

    /**
     * get proxyChannelMap
     *
     * @return the proxyChannelMap
     */
    public Map<String, ProxyChannel> getProxyChannelMap() {
        return proxyChannelMap;
    }

    /**
     * set proxyChannelMap
     *
     * @param proxyChannelMap the proxyChannelMap to set
     */
    public void setProxyChannelMap(Map<String, ProxyChannel> proxyChannelMap) {
        this.proxyChannelMap = proxyChannelMap;
    }

    /**
     * get defaultConfigJson
     *
     * @return the defaultConfigJson
     */
    public String getDefaultConfigJson() {
        return defaultConfigJson;
    }

    /**
     * set defaultConfigJson
     *
     * @param defaultConfigJson the defaultConfigJson to set
     */
    public void setDefaultConfigJson(String defaultConfigJson) {
        this.defaultConfigJson = defaultConfigJson;
    }

    /**
     * get proxySourceMap
     *
     * @return the proxySourceMap
     */
    public Map<String, ProxySource> getProxySourceMap() {
        return proxySourceMap;
    }

    /**
     * set proxySourceMap
     *
     * @param proxySourceMap the proxySourceMap to set
     */
    public void setProxySourceMap(Map<String, ProxySource> proxySourceMap) {
        this.proxySourceMap = proxySourceMap;
    }

    /**
     * get proxySinkMap
     *
     * @return the proxySinkMap
     */
    public Map<String, ProxySink> getProxySinkMap() {
        return proxySinkMap;
    }

    /**
     * set proxySinkMap
     *
     * @param proxySinkMap the proxySinkMap to set
     */
    public void setProxySinkMap(Map<String, ProxySink> proxySinkMap) {
        this.proxySinkMap = proxySinkMap;
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
     * get proxy2Cache
     *
     * @return the proxy2Cache
     */
    public Map<String, Set<String>> getProxy2Cache() {
        return proxy2Cache;
    }

    /**
     * set proxy2Cache
     *
     * @param proxy2Cache the proxy2Cache to set
     */
    public void setProxy2Cache(Map<String, Set<String>> proxy2Cache) {
        this.proxy2Cache = proxy2Cache;
    }

    /**
     * addProxy2Cache
     */
    public void addProxy2Cache(String proxyClusterName, String cacheClusterName) {
        Set<String> cacheNameSet = this.proxy2Cache.computeIfAbsent(proxyClusterName, k -> new HashSet<>());
        cacheNameSet.add(cacheClusterName);
    }

    /**
     * get proxyConfigJson
     *
     * @return the proxyConfigJson
     */
    public Map<String, String> getProxyConfigJson() {
        return proxyConfigJson;
    }

    /**
     * set proxyConfigJson
     *
     * @param proxyConfigJson the proxyConfigJson to set
     */
    public void setProxyConfigJson(Map<String, String> proxyConfigJson) {
        this.proxyConfigJson = proxyConfigJson;
    }

    /**
     * get md5Map
     *
     * @return the md5Map
     */
    public Map<String, String> getMd5Map() {
        return md5Map;
    }

    /**
     * set md5Map
     *
     * @param md5Map the md5Map to set
     */
    public void setMd5Map(Map<String, String> md5Map) {
        this.md5Map = md5Map;
    }

}
