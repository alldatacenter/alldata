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

package org.apache.inlong.sdk.dataproxy.pb.context;

import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchProfile;
import org.apache.inlong.sdk.dataproxy.pb.network.IpPort;

/**
 * SdkProfile
 */
public class SdkProfile {

    public static final String KEY_SDK_PACKID = "sdkPackId";
    
    private final DispatchProfile dispatchProfile;
    private final long sdkPackId;
    private final long sendTime;
    private IpPort ipPort;

    /**
     * Constructor
     * 
     * @param dispatchProfile
     * @param sdkPackId
     */
    public SdkProfile(DispatchProfile dispatchProfile, long sdkPackId) {
        this.dispatchProfile = dispatchProfile;
        this.sdkPackId = sdkPackId;
        this.sendTime = System.currentTimeMillis();
    }

    /**
     * get dispatchProfile
     * 
     * @return the dispatchProfile
     */
    public DispatchProfile getDispatchProfile() {
        return dispatchProfile;
    }

    /**
     * get sdkPackId
     * 
     * @return the sdkPackId
     */
    public long getSdkPackId() {
        return sdkPackId;
    }

    /**
     * get sendTime
     * 
     * @return the sendTime
     */
    public long getSendTime() {
        return sendTime;
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
     * set ipPort
     * 
     * @param ipPort the ipPort to set
     */
    public void setIpPort(IpPort ipPort) {
        this.ipPort = ipPort;
    }

}
