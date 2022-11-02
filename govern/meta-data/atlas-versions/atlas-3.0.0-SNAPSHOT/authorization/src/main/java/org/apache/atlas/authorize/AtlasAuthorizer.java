/**
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


import org.apache.atlas.model.instance.AtlasEntityHeader;

public interface AtlasAuthorizer {
    /**
     * initialization of authorizer implementation
     */
    void init();

    /**
     * cleanup of authorizer implementation
     */
    void cleanUp();

    /**
     * authorize admin operations
     * @param request
     * @return
     * @throws AtlasAuthorizationException
     */
    boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException;

    /**
     * authorize operations on an entity
     * @param request
     * @return
     * @throws AtlasAuthorizationException
     */
    boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException;

    /**
     * authorize operations on a type
     * @param request
     * @return
     * @throws AtlasAuthorizationException
     */
    boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException;

    /**
     * authorize relationship type
     * @param request
     * @return
     * @throws AtlasAuthorizationException
     */
    default
    boolean isAccessAllowed(AtlasRelationshipAccessRequest request) throws AtlasAuthorizationException {
        return true;
    }

    /**
     * scrub search-results to handle entities for which the user doesn't have access
     * @param request
     * @return
     * @throws AtlasAuthorizationException
     */
    default
    void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
    }

    default
    void scrubEntityHeader(AtlasEntityHeader entity) {
        entity.setGuid("-1");

        if(entity.getAttributes() != null) {
            entity.getAttributes().clear();
        }

        if(entity.getClassifications() != null) {
            entity.getClassifications().clear();
        }

        if(entity.getClassificationNames() != null) {
            entity.getClassificationNames().clear();
        }

        if(entity.getMeanings() != null) {
            entity.getMeanings().clear();
        }

        if(entity.getMeaningNames() != null) {
            entity.getMeaningNames().clear();
        }
    }

    default
    void filterTypesDef(AtlasTypesDefFilterRequest request) throws AtlasAuthorizationException {
    }
}
