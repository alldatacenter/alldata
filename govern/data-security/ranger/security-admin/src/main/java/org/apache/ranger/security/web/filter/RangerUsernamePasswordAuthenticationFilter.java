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

 /**
 *
 */
package org.apache.ranger.security.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class RangerUsernamePasswordAuthenticationFilter extends
	UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory
	    .getLogger(RangerUsernamePasswordAuthenticationFilter.class);

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.security.web.authentication.
     * AbstractAuthenticationProcessingFilter
     * #setRememberMeServices(org.springframework
     * .security.web.authentication.RememberMeServices)
     */
    @Override
    public void setRememberMeServices(RememberMeServices rememberMeServices) {
	if (logger.isDebugEnabled()) {
	    logger.debug("setRememberMeServices() enter: rememberMeServices="
		    + rememberMeServices.toString());
	}
	super.setRememberMeServices(rememberMeServices);
    }

}
