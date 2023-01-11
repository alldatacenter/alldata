/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.security.web.filter;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

public class SSOAuthenticationProperties {

    private String authenticationProviderUrl;
    private RSAPublicKey publicKey;
    private String cookieName = "hadoop-jwt";
    private String originalUrlQueryParam;
    private String[] userAgentList;
    private List<String> audiences = Collections.emptyList();
    private String expectedSigAlg;

    public String getAuthenticationProviderUrl() {
        return authenticationProviderUrl;
    }

    public void setAuthenticationProviderUrl(String authenticationProviderUrl) {
        this.authenticationProviderUrl = authenticationProviderUrl;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getOriginalUrlQueryParam() {
        return originalUrlQueryParam;
    }

    public void setOriginalUrlQueryParam(String originalUrlQueryParam) {
        this.originalUrlQueryParam = originalUrlQueryParam;
    }

    /**
     * @return the userAgentList
     */
    public String[] getUserAgentList() {
        return userAgentList;
    }

    /**
     * @param userAgentList the userAgentList to set
     */
    public void setUserAgentList(String[] userAgentList) {
        this.userAgentList = userAgentList;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    public String getExpectedSigAlg() {
        return expectedSigAlg;
    }

    public void setExpectedSigAlg(String expectedSigAlg) {
        this.expectedSigAlg = expectedSigAlg;
    }
}

