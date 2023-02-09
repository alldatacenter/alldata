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

package com.obs.services;

/**
 * HTTP proxy configuration
 */
public class HttpProxyConfiguration {

    private String proxyAddr;

    private int proxyPort;

    private String proxyUname;

    private String userPasswd;

    private String domain;

    private String workstation;

    public HttpProxyConfiguration() {
    }

    /**
     * 
     * @param proxyAddr
     *            Proxy address
     * @param proxyPort
     *            Proxy port
     * @param proxyUname
     *            Proxy username
     * @param userPasswd
     *            Proxy password
     * @param domain
     *            Proxy domain
     */
    public HttpProxyConfiguration(String proxyAddr, int proxyPort, String proxyUname, String userPasswd,
            String domain) {
        this.proxyAddr = proxyAddr;
        this.proxyPort = proxyPort;
        this.proxyUname = proxyUname;
        this.userPasswd = userPasswd;
        this.domain = domain;
        this.workstation = this.proxyAddr;
    }

    /**
     * Parameterized constructor
     * 
     * @param proxyAddr
     *            Proxy address
     * @param proxyPort
     *            Proxy port
     * @param proxyUname
     *            Proxy username
     * @param userPasswd
     *            Proxy password
     * @param domain
     *            Proxy domain
     * @param workstation
     *            Workstation where the proxy is resides
     */
    public HttpProxyConfiguration(String proxyAddr, int proxyPort, String proxyUname, String userPasswd, String domain,
            String workstation) {
        this(proxyAddr, proxyPort, proxyUname, userPasswd, domain);
        this.workstation = this.proxyAddr;
    }

    /**
     * Obtain the proxy address.
     * 
     * @return Proxy address
     */
    public String getProxyAddr() {
        return proxyAddr;
    }

    /**
     * Set the proxy address.
     * 
     * @param proxyAddr
     *            Proxy address
     */
    public void setProxyAddr(String proxyAddr) {
        this.proxyAddr = proxyAddr;
    }

    /**
     * Obtain the proxy port.
     * 
     * @return Proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Set the proxy port.
     * 
     * @param proxyPort
     *            Proxy port
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Obtain the proxy username.
     * 
     * @return Proxy username
     */
    public String getProxyUName() {
        return proxyUname;
    }

    /**
     * Set the username.
     * 
     * @param proxyUName
     *            Proxy username
     */
    public void setProxyUName(String proxyUName) {
        this.proxyUname = proxyUName;
    }

    /**
     * Obtain the proxy password.
     * 
     * @return Proxy password
     */
    public String getUserPasswd() {
        return userPasswd;
    }

    @Deprecated
    public String getUserPaaswd() {
        return getUserPasswd();
    }

    /**
     * Set the proxy password.
     * 
     * @param userPasswd
     *            Proxy password
     */
    public void setUserPasswd(String userPasswd) {
        this.userPasswd = userPasswd;
    }

    @Deprecated
    public void setUserPaaswd(String userPasswd) {
        setUserPasswd(userPasswd);
    }

    /**
     * Obtain the proxy domain.
     * 
     * @return Proxy domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Set the proxy domain.
     * 
     * @param domain
     *            Proxy domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Obtain the proxy workstation.
     * 
     * @return Proxy workstation
     */
    public String getWorkstation() {
        return workstation;
    }

    /**
     * Set the proxy workstation.
     * 
     * @param workstation
     *            Workstation where the proxy is resides
     */
    public void setWorkstation(String workstation) {
        this.workstation = workstation;
    }

}
