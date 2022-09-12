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
 * GetProxyConfigByStreamResponse
 */
public class GetProxyConfigByStreamResponse {

    private boolean result;
    private int errCode;// ERROR_CODE.value
    private String md5;
    private ProxyClusterConfig data;

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
    public ProxyClusterConfig getData() {
        return data;
    }

    /**
     * set data
     * 
     * @param data the data to set
     */
    public void setData(ProxyClusterConfig data) {
        this.data = data;
    }

    /**
     * getExample
     * 
     * @return
     */
    public static GetProxyConfigByStreamResponse getExample() {
        GetProxyConfigByStreamResponse response = new GetProxyConfigByStreamResponse();
        response.setResult(true);
        response.setErrCode(ErrorCode.SUCC.value());
        ProxyClusterConfig cluster = ProxyClusterConfig.getExample();
        response.setData(cluster);
        String md5 = ProxyClusterConfig.generateMd5(cluster);
        response.setMd5(md5);
        return response;
    }
}
