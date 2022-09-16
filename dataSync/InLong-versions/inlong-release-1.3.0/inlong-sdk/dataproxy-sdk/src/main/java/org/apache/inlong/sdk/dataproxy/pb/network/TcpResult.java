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

package org.apache.inlong.sdk.dataproxy.pb.network;

/**
 * TcpResult
 */
public class TcpResult {

    public final IpPort ipPort;
    public final boolean result;
    public final String errorMsg;
    public Long channelId;

    /**
     * Constructor
     * 
     * @param ipPort
     * @param result
     * @param errorMsg
     */
    public TcpResult(IpPort ipPort, boolean result, String errorMsg) {
        this.ipPort = ipPort;
        this.result = result;
        this.errorMsg = errorMsg;
    }

    /**
     * Constructor
     * 
     * @param sendIp
     * @param sendPort
     * @param result
     * @param errorMsg
     */
    public TcpResult(String sendIp, int sendPort, boolean result, String errorMsg) {
        this.ipPort = new IpPort(sendIp, sendPort);
        this.result = result;
        this.errorMsg = errorMsg;
    }

    /**
     * get channelId
     * 
     * @return the channelId
     */
    public Long getChannelId() {
        return channelId;
    }

    /**
     * set channelId
     * 
     * @param channelId the channelId to set
     */
    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    /**
     * get ipPort
     * 
     * @return the ipPort
     */
    public IpPort getIpPort() {
        return ipPort;
    }

    /**
     * get result
     * 
     * @return the result
     */
    public boolean isResult() {
        return result;
    }

    /**
     * get errorMsg
     * 
     * @return the errorMsg
     */
    public String getErrorMsg() {
        return errorMsg;
    }

}
