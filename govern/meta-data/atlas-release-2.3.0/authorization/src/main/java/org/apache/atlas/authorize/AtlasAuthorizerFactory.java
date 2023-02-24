/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.authorize;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.authorize.simple.AtlasSimpleAuthorizer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AtlasAuthorizerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasAuthorizerFactory.class);

    private static final String NONE_AUTHORIZER   = AtlasNoneAuthorizer.class.getName();
    private static final String SIMPLE_AUTHORIZER = AtlasSimpleAuthorizer.class.getName();
    private static final String RANGER_AUTHORIZER = "org.apache.ranger.authorization.atlas.authorizer.RangerAtlasAuthorizer";

    private static volatile AtlasAuthorizer INSTANCE = null;

    public static AtlasAuthorizer getAtlasAuthorizer() throws AtlasAuthorizationException {
        AtlasAuthorizer ret = INSTANCE;

        if (ret == null) {
            synchronized (AtlasAuthorizerFactory.class) {
                if (INSTANCE == null) {
                    Configuration configuration = null;

                    try {
                        configuration = ApplicationProperties.get();
                    } catch (AtlasException e) {
                        LOG.error("Exception while fetching configuration", e);
                    }

                    String authorizerClass = configuration != null ? configuration.getString("atlas.authorizer.impl") : "SIMPLE";

                    if (StringUtils.isNotEmpty(authorizerClass)) {
                        if (StringUtils.equalsIgnoreCase(authorizerClass, "SIMPLE")) {
                            authorizerClass = SIMPLE_AUTHORIZER;
                        } else if (StringUtils.equalsIgnoreCase(authorizerClass, "RANGER")) {
                            authorizerClass = RANGER_AUTHORIZER;
                        } else if (StringUtils.equalsIgnoreCase(authorizerClass, "NONE")) {
                            authorizerClass = NONE_AUTHORIZER;
                        }
                    } else {
                        authorizerClass = SIMPLE_AUTHORIZER;
                    }

                    LOG.info("Initializing Authorizer {}", authorizerClass);

                    try {
                        Class authorizerMetaObject = Class.forName(authorizerClass);

                        if (authorizerMetaObject != null) {
                            INSTANCE = (AtlasAuthorizer) authorizerMetaObject.newInstance();

                            INSTANCE.init();
                        }
                    } catch (Exception e) {
                        LOG.error("Error while creating authorizer of type {}", authorizerClass, e);

                        throw new AtlasAuthorizationException("Error while creating authorizer of type '" + authorizerClass + "'", e);
                    }
                }

                ret = INSTANCE;
            }
        }

        return ret;
    }
}
