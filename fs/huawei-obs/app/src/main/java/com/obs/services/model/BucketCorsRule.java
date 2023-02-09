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

import java.util.ArrayList;
import java.util.List;

/**
 * Bucket CORS rules
 */
public class BucketCorsRule {
    private String id;

    private int maxAgeSecond = Integer.MIN_VALUE;

    private List<String> allowedMethod;

    private List<String> allowedOrigin;

    private List<String> allowedHeader;

    private List<String> exposeHeader;

    /**
     * Obtain the CORS rule ID.
     * 
     * @return CORS rule ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the CORS rule ID.
     * 
     * @param id
     *            CORS rule ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtain the cache duration (in seconds) of the request result in the
     * instance of ObsClient.
     * 
     * @return Cache duration
     */
    public int getMaxAgeSecond() {
        if (this.maxAgeSecond == Integer.MIN_VALUE) {
            return 0;
        }
        return maxAgeSecond;
    }

    /**
     * Set the cache duration (in seconds) of the request result in the instance
     * of ObsClient.
     * 
     * @param maxAgeSecond
     *            Cache duration
     */
    public void setMaxAgeSecond(int maxAgeSecond) {
        this.maxAgeSecond = maxAgeSecond;
    }

    /**
     * Obtain the list of methods allowed by the CORS rules.
     * 
     * @return Method list
     */
    public List<String> getAllowedMethod() {
        if (null == allowedMethod) {
            return allowedMethod = new ArrayList<String>();
        }
        return allowedMethod;
    }

    /**
     * Set the methods allowed by the CORS rules. Possible values are GET, PUT,
     * DELETE, POST, and HEAD.
     * 
     * @param allowedMethod
     *            Method list
     */
    public void setAllowedMethod(List<String> allowedMethod) {
        this.allowedMethod = allowedMethod;
    }

    /**
     * Obtain the list of origins (character strings representing domain names)
     * allowed by the CORS rules.
     * 
     * @return List of request origins
     */
    public List<String> getAllowedOrigin() {
        if (null == allowedOrigin) {
            return allowedOrigin = new ArrayList<String>();
        }
        return allowedOrigin;
    }

    /**
     * Set the list of origins (character strings representing domain names)
     * allowed by the CORS rules.
     * 
     * @param allowedOrigin
     *            List of request origins
     */
    public void setAllowedOrigin(List<String> allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    /**
     * Obtain the list of request headers allowed by the CORS rules.
     * 
     * @return List of request headers
     */
    public List<String> getAllowedHeader() {
        if (null == allowedHeader) {
            return allowedHeader = new ArrayList<String>();
        }
        return allowedHeader;
    }

    /**
     * Set the list of request headers allowed by the CORS rules.
     * 
     * @param allowedHeader
     *            List of request headers
     */
    public void setAllowedHeader(List<String> allowedHeader) {
        this.allowedHeader = allowedHeader;
    }

    /**
     * Obtain the additional response headers allowed by the CORS rules.
     * 
     * @return List of additional headers
     */
    public List<String> getExposeHeader() {
        if (null == exposeHeader) {
            return exposeHeader = new ArrayList<String>();
        }
        return exposeHeader;
    }

    /**
     * Specify the additional response headers allowed by the CORS rules.
     * 
     * @param exposeHeader
     *            List of additional headers
     */
    public void setExposeHeader(List<String> exposeHeader) {
        this.exposeHeader = exposeHeader;
    }

    @Override
    public String toString() {
        return "BucketCorsRule [id=" + id + ", maxAgeSecond=" + maxAgeSecond + ", allowedMethod=" + allowedMethod
                + ", allowedOrigin=" + allowedOrigin + ", allowedHeader=" + allowedHeader + ", exposeHeader="
                + exposeHeader + "]";
    }

}
