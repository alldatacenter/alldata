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

import org.apache.atlas.model.typedef.AtlasTypesDef;

import java.util.Set;

public class AtlasTypesDefFilterRequest extends AtlasAccessRequest {
    private final AtlasTypesDef typesDef;


    public AtlasTypesDefFilterRequest(AtlasTypesDef typesDef) {
        super(AtlasPrivilege.TYPE_READ);

        this.typesDef = typesDef;
    }

    public AtlasTypesDefFilterRequest(AtlasTypesDef typesDef, String userName, Set<String> usergroups) {
        super(AtlasPrivilege.TYPE_READ, userName, usergroups);

        this.typesDef = typesDef;
    }

    public AtlasTypesDef getTypesDef() {
        return typesDef;
    }

    @Override
    public String toString() {
        return "AtlasTypesDefFilterRequest[typesDef=" + typesDef + ", action=" + getAction() + ", accessTime=" + getAccessTime() +
                ", user=" + getUser() + ", userGroups=" + getUserGroups() + ", clientIPAddress=" + getClientIPAddress() +
                ", forwardedAddresses=" + getForwardedAddresses() + ", remoteIPAddress=" + getRemoteIPAddress() + "]";
    }
}
