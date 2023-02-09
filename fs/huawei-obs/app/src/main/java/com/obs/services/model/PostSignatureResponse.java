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
**/

package com.obs.services.model;

/**
 * 
 * Response to a request for browser-based authorized access
 *
 */
public class PostSignatureResponse {
    protected String policy;

    protected String originPolicy;

    protected String signature;

    protected String expiration;

    protected String token;

    public PostSignatureResponse() {

    }

    public PostSignatureResponse(String policy, String originPolicy, String signature, String expiration,
            String accessKey) {
        this.policy = policy;
        this.originPolicy = originPolicy;
        this.signature = signature;
        this.expiration = expiration;
        this.token = accessKey + ":" + signature + ":" + policy;
    }

    /**
     * Obtain the security policy of the request in the Base64 format.
     * 
     * @return Security policy in the Base64 format
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * Obtain the security policy of the request in the original format.
     * 
     * @return Security policy in the original format
     */
    public String getOriginPolicy() {
        return originPolicy;
    }

    /**
     * Obtain the signature string.
     * 
     * @return Signature string
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Obtain the expiration date of the request.
     * 
     * @return Expiration date
     */
    public String getExpiration() {
        return expiration;
    }

    /**
     * Obtain the token
     * 
     * @return token
     */
    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "PostSignatureResponse [policy=" + policy + ", originPolicy=" + originPolicy + ", signature=" + signature
                + ", expiration=" + expiration + ", token=" + token + "]";
    }

}
