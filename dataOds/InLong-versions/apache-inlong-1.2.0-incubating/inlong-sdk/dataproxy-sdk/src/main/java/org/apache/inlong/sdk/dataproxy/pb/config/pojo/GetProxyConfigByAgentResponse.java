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

import org.apache.commons.codec.digest.DigestUtils;

import com.alibaba.fastjson.JSON;

/**
 * 
 * GetProxyConfigByAgentResponse
 */
public class GetProxyConfigByAgentResponse {

    private boolean result;
    private int errCode; // ERROR_CODE.value
    private String md5;
    private List<ProxyClusterConfig> data;

    /**
     * get result
     * 
     * @return the result
     */
    public boolean isResult() {
        return result;
    }

    /**
     * set result
     * 
     * @param result the result to set
     */
    public void setResult(boolean result) {
        this.result = result;
    }

    /**
     * get errCode
     * 
     * @return the errCode
     */
    public int getErrCode() {
        return errCode;
    }

    /**
     * set errCode
     * 
     * @param errCode the errCode to set
     */
    public void setErrCode(int errCode) {
        this.errCode = errCode;
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
     * get data
     * 
     * @return the data
     */
    public List<ProxyClusterConfig> getData() {
        return data;
    }

    /**
     * set data
     * 
     * @param data the data to set
     */
    public void setData(List<ProxyClusterConfig> data) {
        this.data = data;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static GetProxyConfigByAgentResponse getExample() {
        GetProxyConfigByAgentResponse response = new GetProxyConfigByAgentResponse();
        response.setResult(true);
        response.setErrCode(ErrorCode.SUCC.value());
        response.setData(new ArrayList<>());
        ProxyClusterConfig cluster = ProxyClusterConfig.getExample();
        response.getData().add(cluster);
        String md5 = GetProxyConfigByAgentResponse.generateMd5(response.getData());
        response.setMd5(md5);
        return response;
    }

    /**
     * generateMd5
     * 
     * @param  configList
     * @return
     */
    public static String generateMd5(List<ProxyClusterConfig> configList) {
        String md5 = DigestUtils.md2Hex(JSON.toJSONString(configList));
        return md5;
    }

    public static void main(String[] args) {
        GetProxyConfigByAgentResponse data = GetProxyConfigByAgentResponse.getExample();
        System.out.println(JSON.toJSONString(data));
    }
}
