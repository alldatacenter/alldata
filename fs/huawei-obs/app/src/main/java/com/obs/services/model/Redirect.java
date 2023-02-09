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
 * Request redirection configuration
 */
public class Redirect {
    private ProtocolEnum protocol;

    private String hostName;

    private String replaceKeyPrefixWith;

    private String replaceKeyWith;

    private String httpRedirectCode;

    /**
     * Obtain the protocol used for redirecting the request.
     * 
     * @return Protocol used for redirecting the request
     * @see #getRedirectProtocol()
     */
    @Deprecated
    public String getProtocol() {
        return this.protocol != null ? this.protocol.getCode() : null;
    }

    /**
     * Set the protocol used for redirecting the request.
     * 
     * @param protocol
     *            Protocol used for redirecting the request
     * @see #setRedirectProtocol(ProtocolEnum protocol)
     */
    @Deprecated
    public void setProtocol(String protocol) {
        this.protocol = ProtocolEnum.getValueFromCode(protocol);
    }

    /**
     * Obtain the protocol used for redirecting the request.
     * 
     * @return Protocol used for redirecting the request
     */
    public ProtocolEnum getRedirectProtocol() {
        return protocol;
    }

    /**
     * Set the protocol used for redirecting the request.
     * 
     * @param protocol
     *            Protocol used for redirecting the request
     */
    public void setRedirectProtocol(ProtocolEnum protocol) {
        this.protocol = protocol;
    }

    /**
     * Obtain the host name used for redirecting the request.
     * 
     * @return Host name used for redirecting the request
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the host name used for redirecting the request.
     * 
     * @param hostName
     *            Host name used for redirecting the request
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Obtain the object name prefix used for redirecting the request.
     * 
     * @return Object name prefix used for redirecting the request
     */
    public String getReplaceKeyPrefixWith() {
        return replaceKeyPrefixWith;
    }

    /**
     * Set the object name prefix used for redirecting the request.
     * 
     * @param replaceKeyPrefixWith
     *            Object name prefix used for redirecting the request
     */
    public void setReplaceKeyPrefixWith(String replaceKeyPrefixWith) {
        this.replaceKeyPrefixWith = replaceKeyPrefixWith;
    }

    /**
     * Obtain the object name used for redirecting the request.
     * 
     * @return Object name used for redirecting the request
     */
    public String getReplaceKeyWith() {
        return replaceKeyWith;
    }

    /**
     * Set the object name used for redirecting the request.
     * 
     * @param replaceKeyWith
     *            Object name used for redirecting the request
     */
    public void setReplaceKeyWith(String replaceKeyWith) {
        this.replaceKeyWith = replaceKeyWith;
    }

    /**
     * Obtain the configuration of the HTTP status code in the response.
     * 
     * @return Configuration of the HTTP status code
     */
    public String getHttpRedirectCode() {
        return httpRedirectCode;
    }

    /**
     * Configure the HTTP status code in the response.
     * 
     * @param httpRedirectCode
     *            Configuration of the HTTP status code
     */
    public void setHttpRedirectCode(String httpRedirectCode) {
        this.httpRedirectCode = httpRedirectCode;
    }

    @Override
    public String toString() {
        return "RedirectRule [protocol=" + protocol + ", hostName=" + hostName + ", replaceKeyPrefixWith="
                + replaceKeyPrefixWith + ", replaceKeyWith=" + replaceKeyWith + ", httpRedirectCode=" + httpRedirectCode
                + "]";
    }
}
