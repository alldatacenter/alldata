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

import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.Set;

public class AtlasSearchResultScrubRequest extends AtlasAccessRequest {
    private final AtlasTypeRegistry typeRegistry;
    private final AtlasSearchResult searchResult;


    public AtlasSearchResultScrubRequest(AtlasTypeRegistry typeRegistry, AtlasSearchResult searchResult) {
        this(typeRegistry, searchResult, null, null);
    }

    public AtlasSearchResultScrubRequest(AtlasTypeRegistry typeRegistry, AtlasSearchResult searchResult, String userName, Set<String> userGroups) {
        super(AtlasPrivilege.ENTITY_READ, userName, userGroups);

        this.typeRegistry = typeRegistry;
        this.searchResult = searchResult;
    }

    public AtlasTypeRegistry getTypeRegistry() { return typeRegistry; }

    public AtlasSearchResult getSearchResult() {
        return searchResult;
    }

    @Override
    public String toString() {
        return "AtlasSearchResultScrubRequest[searchResult=" + searchResult + ", action=" + getAction() + ", accessTime=" + getAccessTime() + ", user=" + getUser() +
                ", userGroups=" + getUserGroups() + ", clientIPAddress=" + getClientIPAddress() +
                ", forwardedAddresses=" + getForwardedAddresses() + ", remoteIPAddress=" + getRemoteIPAddress() + "]";
    }
}


