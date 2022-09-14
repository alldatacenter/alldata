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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * GetProxyConfigBySdkResponse
 */
public class GetProxyConfigBySdkResponse {

    private boolean result;
    private int errCode; // ERROR_CODE.value
    private Map<String, ProxyClusterResult> data;

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
     * get data
     * 
     * @return the data
     */
    public Map<String, ProxyClusterResult> getData() {
        return data;
    }

    /**
     * set data
     * 
     * @param data the data to set
     */
    public void setData(Map<String, ProxyClusterResult> data) {
        this.data = data;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static GetProxyConfigBySdkResponse getExample() {
        GetProxyConfigBySdkResponse response = new GetProxyConfigBySdkResponse();
        response.setResult(true);
        response.setErrCode(ErrorCode.SUCC.value());
        response.setData(new HashMap<>());
        ProxyClusterResult result = ProxyClusterResult.getExample();
        response.getData().put(result.getClusterId(), result);
        return response;
    }
}
