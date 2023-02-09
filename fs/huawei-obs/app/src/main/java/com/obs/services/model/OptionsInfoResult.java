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
 * Response to a bucket or object preflight request
 *
 */
public class OptionsInfoResult extends HeaderResponse {
    protected String allowOrigin;

    protected List<String> allowHeaders;

    protected int maxAge;

    protected List<String> allowMethods;

    protected List<String> exposeHeaders;

    public OptionsInfoResult(String allowOrigin, List<String> allowHeaders, int maxAge, List<String> allowMethods,
            List<String> exposeHeaders) {
        super();
        this.allowOrigin = allowOrigin;
        this.allowHeaders = allowHeaders;
        this.maxAge = maxAge;
        this.allowMethods = allowMethods;
        this.exposeHeaders = exposeHeaders;
    }

    protected OptionsInfoResult() {
    }
    
    /**
     * Obtain the origin of the allowed cross-origin request.
     * 
     * @return Origin of the allowed cross-origin request
     */
    public String getAllowOrigin() {
        return allowOrigin;
    }

    /**
     * Obtain the list of allowed request headers.
     * 
     * @return List of allowed request headers
     */
    public List<String> getAllowHeaders() {
        if (this.allowHeaders == null) {
            allowHeaders = new ArrayList<String>();
        }
        return allowHeaders;
    }

    /**
     * Obtain the cache duration (in seconds) of the request result in the
     * instance of ObsClient.
     * 
     * @return Cache duration of the request result in the instance of ObsClient
     */
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * Obtain the list of allowed cross-origin request methods.
     * 
     * @return List of allowed cross-origin request methods
     */
    public List<String> getAllowMethods() {
        if (this.allowMethods == null) {
            this.allowMethods = new ArrayList<String>();
        }
        return allowMethods;
    }

    /**
     * Obtain the list of allowed additional headers in the response.
     * 
     * @return List of allowed additional headers in the response
     */
    public List<String> getExposeHeaders() {
        if (this.exposeHeaders == null) {
            this.exposeHeaders = new ArrayList<String>();
        }
        return exposeHeaders;
    }

    @Override
    public String toString() {
        return "OptionsInfoResult [allowOrigin=" + allowOrigin + ", allowHeaders=" + allowHeaders + ", maxAge=" + maxAge
                + ", allowMethods=" + allowMethods + ", exposeHeaders=" + exposeHeaders + "]";
    }

}
