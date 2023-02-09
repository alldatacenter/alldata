/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

/**
 * Redirection configuration for all requests
 */
public class RedirectAllRequest {
    private ProtocolEnum protocol;

    private String hostName;

    /**
     * Obtain the protocol used for redirecting requests.
     * 
     * @return Protocol used for redirecting requests
     * @see #getRedirectProtocol()
     */
    @Deprecated
    public String getProtocol() {
        return this.protocol != null ? this.protocol.getCode() : null;
    }

    /**
     * Set the protocol used for redirecting requests.
     * 
     * @param protocol
     *            Protocol used for redirecting requests
     * @see #setRedirectProtocol(ProtocolEnum protocol)
     */
    @Deprecated
    public void setProtocol(String protocol) {
        this.protocol = ProtocolEnum.getValueFromCode(protocol);
    }

    /**
     * Obtain the protocol used for redirecting requests.
     * 
     * @return Protocol used for redirecting requests
     */
    public ProtocolEnum getRedirectProtocol() {
        return protocol;
    }

    /**
     * Set the protocol used for redirecting requests.
     * 
     * @param protocol
     *            Protocol used for redirecting requests
     */
    public void setRedirectProtocol(ProtocolEnum protocol) {
        this.protocol = protocol;
    }

    /**
     * Obtain the host name used for redirecting requests.
     * 
     * @return Host name used for redirecting requests
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the host name used for redirecting requests.
     * 
     * @param hostName
     *            Host name used for redirecting requests
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String toString() {
        return "RedirectAllRequest [protocol=" + protocol + ", hostName=" + hostName + "]";
    }

}
