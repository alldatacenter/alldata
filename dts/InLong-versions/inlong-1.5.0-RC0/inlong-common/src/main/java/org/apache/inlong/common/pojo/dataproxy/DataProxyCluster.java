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

/**
 * DataProxyCluster
 */
public class DataProxyCluster {

    private ProxyClusterObject proxyCluster = new ProxyClusterObject();
    private CacheClusterSetObject cacheClusterSet = new CacheClusterSetObject();

    /**
     * get proxyCluster
     *
     * @return the proxyCluster
     */
    public ProxyClusterObject getProxyCluster() {
        return proxyCluster;
    }

    /**
     * set proxyCluster
     *
     * @param proxyCluster the proxyCluster to set
     */
    public void setProxyCluster(ProxyClusterObject proxyCluster) {
        this.proxyCluster = proxyCluster;
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

}
