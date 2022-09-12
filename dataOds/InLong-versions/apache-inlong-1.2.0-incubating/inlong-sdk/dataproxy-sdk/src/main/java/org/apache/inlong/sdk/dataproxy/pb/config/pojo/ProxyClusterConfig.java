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

package org.apache.inlong.sdk.dataproxy.pb.config.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.alibaba.fastjson.JSON;

/**
 * 
 * ProxyClusterConfig
 */
public class ProxyClusterConfig {

    private String clusterId;
    private String regionId;
    private List<InlongStreamConfig> inlongStreamList;
    private List<ProxyNodeInfo> nodeList;
    private Map<String, String> proxyParams;

    /**
     * get clusterId
     * 
     * @return the clusterId
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * set clusterId
     * 
     * @param clusterId the clusterId to set
     */
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * get regionId
     * 
     * @return the regionId
     */
    public String getRegionId() {
        return regionId;
    }

    /**
     * set regionId
     * 
     * @param regionId the regionId to set
     */
    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    /**
     * get inlongStreamList
     * 
     * @return the inlongStreamList
     */
    public List<InlongStreamConfig> getInlongStreamList() {
        return inlongStreamList;
    }

    /**
     * set inlongStreamList
     * 
     * @param inlongStreamList the inlongStreamList to set
     */
    public void setInlongStreamList(List<InlongStreamConfig> inlongStreamList) {
        this.inlongStreamList = inlongStreamList;
    }

    /**
     * get nodeList
     * 
     * @return the nodeList
     */
    public List<ProxyNodeInfo> getNodeList() {
        return nodeList;
    }

    /**
     * set nodeList
     * 
     * @param nodeList the nodeList to set
     */
    public void setNodeList(List<ProxyNodeInfo> nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * get proxyParams
     * 
     * @return the proxyParams
     */
    public Map<String, String> getProxyParams() {
        return proxyParams;
    }

    /**
     * set proxyParams
     * 
     * @param proxyParams the proxyParams to set
     */
    public void setProxyParams(Map<String, String> proxyParams) {
        this.proxyParams = proxyParams;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static ProxyClusterConfig getExample() {
        ProxyClusterConfig config = new ProxyClusterConfig();
        config.setClusterId("inlong6th.sz.sz1");
        config.setRegionId("sz");
        config.setProxyParams(new HashMap<>());
        config.getProxyParams().put("reloadInterval", "60000");
        config.setInlongStreamList(new ArrayList<>());
        InlongStreamConfig streamConfig = new InlongStreamConfig();
        streamConfig.setInlongGroupId("inlongGroupId");
        streamConfig.setInlongStreamId("inlongStreamId");
        streamConfig.setStatus(0);
        streamConfig.setSampling(1);
        config.getInlongStreamList().add(streamConfig);
        config.setNodeList(new ArrayList<>());
        ProxyNodeInfo nodeInfo = new ProxyNodeInfo();
        nodeInfo.setNodeIp("127.0.0.1");
        nodeInfo.setNodePort(8080);
        config.getNodeList().add(nodeInfo);
        return config;
    }

    /**
     * generateMd5
     * 
     * @param  config
     * @return
     */
    public static String generateMd5(ProxyClusterConfig config) {
        String md5 = DigestUtils.md2Hex(JSON.toJSONString(config));
        return md5;
    }
}
