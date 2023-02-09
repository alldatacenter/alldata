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
 * 
 * Response to a request for browser-based authorized access
 *
 */
public class V4PostSignatureResponse extends PostSignatureResponse {
    private String algorithm;

    private String credential;

    private String date;

    public V4PostSignatureResponse(String policy, String originPolicy, String algorithm, String credential, String date,
            String signature, String expiration) {
        this.policy = policy;
        this.originPolicy = originPolicy;
        this.algorithm = algorithm;
        this.credential = credential;
        this.date = date;
        this.signature = signature;
        this.expiration = expiration;
    }

    /**
     * Obtain the signature algorithm.
     * 
     * @return Signature algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Obtain the credential information
     * 
     * @return Credential information
     */
    public String getCredential() {
        return credential;
    }

    /**
     * Obtain the date in the ISO 8601 format.
     * 
     * @return Date in the ISO 8601 format
     */
    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "V4PostSignatureResponse [algorithm=" + algorithm + ", credential=" + credential + ", date=" + date
                + ", expiration=" + expiration + ", policy=" + policy + ", originPolicy=" + originPolicy
                + ", signature=" + signature + "]";
    }

}
