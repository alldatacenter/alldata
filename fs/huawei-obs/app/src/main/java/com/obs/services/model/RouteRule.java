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
 * Route rules for redirection
 */
public class RouteRule {
    private RouteRuleCondition condition;

    private Redirect redirect;

    /**
     * Obtain the redirection condition.
     * 
     * @return Redirection condition
     */
    public RouteRuleCondition getCondition() {
        return condition;
    }

    /**
     * Set the redirection condition
     * 
     * @param condition
     *            Redirection condition
     */
    public void setCondition(RouteRuleCondition condition) {
        this.condition = condition;
    }

    /**
     * Obtain the redirection configuration.
     * 
     * @return Redirection configuration
     */
    public Redirect getRedirect() {
        return redirect;
    }

    /**
     * Configure the redirection.
     * 
     * @param redirect
     *            Redirection configuration
     */
    public void setRedirect(Redirect redirect) {
        this.redirect = redirect;
    }

    @Override
    public String toString() {
        return "RouteRule [condition=" + condition + ", redirect=" + redirect + "]";
    }

}
