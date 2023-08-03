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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AtlasNoneAuthorizer implements AtlasAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasNoneAuthorizer.class);

    public void init() {
        LOG.info("AtlasNoneAuthorizer.init()");
    }

    public void cleanUp() {
        LOG.info("AtlasNoneAuthorizer.cleanUp()");
    }

    public boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException {
        return true;
    }

    public boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException {
        return true;
    }

    public boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException {
        return true;
    }

    @Override
    public boolean isAccessAllowed(AtlasRelationshipAccessRequest request) throws AtlasAuthorizationException {
        return true;
    }

    public void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {

    }
}
