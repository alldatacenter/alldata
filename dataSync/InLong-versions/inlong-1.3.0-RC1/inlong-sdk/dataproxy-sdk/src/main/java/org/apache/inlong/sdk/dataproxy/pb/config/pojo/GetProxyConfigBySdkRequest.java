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
import java.util.List;

/**
 * 
 * GetProxyConfigBySdkRequest
 */
public class GetProxyConfigBySdkRequest {

    private List<ProxyInfo> proxys;

    /**
     * get proxys
     * 
     * @return the proxys
     */
    public List<ProxyInfo> getProxys() {
        return proxys;
    }

    /**
     * set proxys
     * 
     * @param proxys the proxys to set
     */
    public void setProxys(List<ProxyInfo> proxys) {
        this.proxys = proxys;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static GetProxyConfigBySdkRequest getExample() {
        ProxyClusterConfig cluster = ProxyClusterConfig.getExample();
        String md5 = ProxyClusterConfig.generateMd5(cluster);
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setClusterId(cluster.getClusterId());
        proxyInfo.setMd5(md5);
        GetProxyConfigBySdkRequest request = new GetProxyConfigBySdkRequest();
        request.setProxys(new ArrayList<>());
        request.getProxys().add(proxyInfo);
        return request;
    }
}
