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

/**
 * 
 * ProxyClusterResult
 */
public class ProxyClusterResult {

    private String clusterId;
    private boolean hasUpdated;
    private String md5;
    private ProxyClusterConfig config;

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
     * get hasUpdated
     * 
     * @return the hasUpdated
     */
    public boolean isHasUpdated() {
        return hasUpdated;
    }

    /**
     * set hasUpdated
     * 
     * @param hasUpdated the hasUpdated to set
     */
    public void setHasUpdated(boolean hasUpdated) {
        this.hasUpdated = hasUpdated;
    }

    /**
     * get md5
     * 
     * @return the md5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * set md5
     * 
     * @param md5 the md5 to set
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * get config
     * 
     * @return the config
     */
    public ProxyClusterConfig getConfig() {
        return config;
    }

    /**
     * set config
     * 
     * @param config the config to set
     */
    public void setConfig(ProxyClusterConfig config) {
        this.config = config;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static ProxyClusterResult getExample() {
        ProxyClusterResult result = new ProxyClusterResult();
        ProxyClusterConfig cluster = ProxyClusterConfig.getExample();
        result.setClusterId(cluster.getClusterId());
        result.setConfig(cluster);
        result.setHasUpdated(true);
        String md5 = ProxyClusterConfig.generateMd5(cluster);
        result.setMd5(md5);
        return result;
    }
}
