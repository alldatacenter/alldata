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

import java.util.ArrayList;
import java.util.List;

/**
 * Website hosting configuration of a bucket
 */
public class WebsiteConfiguration extends HeaderResponse {
    private String suffix;

    private String key;

    private RedirectAllRequest redirectAllRequestsTo;

    private List<RouteRule> routeRules;

    /**
     * Obtain the hosting homepage.
     * 
     * @return Hosting homepage
     * 
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Set the hosting homepage.
     * 
     * @param suffix
     *            Hosting homepage
     * 
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Obtain the error page of the hosting.
     ** 
     * @return key Error page of the hosting.
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the error page of the hosting.
     * 
     * @param key
     *            Error page of the hosting
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Set the redirection route rules.
     * 
     * @return routeRules Redirection route rule list
     */
    public List<RouteRule> getRouteRules() {
        if (this.routeRules == null) {
            this.routeRules = new ArrayList<RouteRule>();
        }
        return routeRules;
    }

    /**
     * Set the redirection route rule list.
     * 
     * @param routeRules
     *            Redirection route rule list
     */
    public void setRouteRules(List<RouteRule> routeRules) {
        this.routeRules = routeRules;
    }

    /**
     * Obtain the redirection rules of all requests.
     * 
     * @return Redirection rules of all requests
     */
    public RedirectAllRequest getRedirectAllRequestsTo() {
        return redirectAllRequestsTo;
    }

    /**
     * Set redirection rules of all requests.
     * 
     * @param redirectAllRequestsTo
     *            Redirection rules of all requests
     */
    public void setRedirectAllRequestsTo(RedirectAllRequest redirectAllRequestsTo) {
        this.redirectAllRequestsTo = redirectAllRequestsTo;
    }

    @Override
    public String toString() {
        return "WebsiteConfigration [suffix=" + suffix + ", key=" + key + ", redirectAllRequestsTo="
                + redirectAllRequestsTo + ", routeRules=" + routeRules + "]";
    }
}
