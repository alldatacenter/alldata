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

import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AtlasAccessRequest {
    private static Logger LOG = LoggerFactory.getLogger(AtlasAccessRequest.class);

    private static final String DEFAULT_ENTITY_ID_ATTRIBUTE = "qualifiedName";

    private final AtlasPrivilege action;
    private final Date           accessTime;
    private       String         user            = null;
    private       Set<String>    userGroups      = null;
    private       String         clientIPAddress = null;
    private       List<String>   forwardedAddresses;
    private       String         remoteIPAddress;


    protected AtlasAccessRequest(AtlasPrivilege action) {
        this(action, null, null, new Date(), null);
    }

    protected AtlasAccessRequest(AtlasPrivilege action, String user, Set<String> userGroups) {
        this(action, user, userGroups, new Date(), null, null, null);
    }

    protected AtlasAccessRequest(AtlasPrivilege action, String user, Set<String> userGroups, Date accessTime,
                                 String clientIPAddress, List<String> forwardedAddresses, String remoteIPAddress) {
        this(action, user, userGroups, accessTime, clientIPAddress);
        this.forwardedAddresses  = forwardedAddresses;
        this.remoteIPAddress     = remoteIPAddress;
    }

    protected AtlasAccessRequest(AtlasPrivilege action, String user, Set<String> userGroups, Date accessTime, String clientIPAddress) {
        this.action          = action;
        this.user            = user;
        this.userGroups      = userGroups;
        this.accessTime      = accessTime;
        this.clientIPAddress = clientIPAddress;
    }

    public AtlasPrivilege getAction() {
        return action;
    }

    public Date getAccessTime() {
        return accessTime;
    }

    public String getUser() {
        return user;
    }

    public Set<String> getUserGroups() {
        return userGroups;
    }

    public void setUser(String user, Set<String> userGroups) {
        this.user       = user;
        this.userGroups = userGroups;
    }

    public List<String> getForwardedAddresses() {
        return forwardedAddresses;
    }

    public String getRemoteIPAddress() {
        return remoteIPAddress;
    }

    public String getClientIPAddress() {
        return clientIPAddress;
    }

    public void setForwardedAddresses(List<String> forwardedAddresses) {
        this.forwardedAddresses = forwardedAddresses;
    }

    public void setRemoteIPAddress(String remoteIPAddress) {
        this.remoteIPAddress = remoteIPAddress;
    }

    public void setClientIPAddress(String clientIPAddress) {
        this.clientIPAddress = clientIPAddress;
    }

    public Set<String> getEntityTypeAndAllSuperTypes(String entityType, AtlasTypeRegistry typeRegistry) {
        final Set<String> ret;

        if (entityType == null) {
            ret = Collections.emptySet();
        } else if (typeRegistry == null) {
            ret = Collections.singleton(entityType);
        } else {
            AtlasEntityType entType = typeRegistry.getEntityTypeByName(entityType);

            ret = entType != null ? entType.getTypeAndAllSuperTypes() : Collections.singleton(entityType);
        }

        return ret;
    }

    public Set<String> getClassificationTypeAndAllSuperTypes(String classificationName, AtlasTypeRegistry typeRegistry) {
        final Set<String> ret;

        if (classificationName == null) {
            ret = Collections.emptySet();
        } else if (typeRegistry == null) {
            ret = Collections.singleton(classificationName);
        } else {
            AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(classificationName);

            return classificationType != null ? classificationType.getTypeAndAllSuperTypes() : Collections.singleton(classificationName);
        }

        return ret;
    }

    public String getEntityId(AtlasEntityHeader entity) {
        return getEntityId(entity, null);
    }

    public String getEntityId(AtlasEntityHeader entity, AtlasTypeRegistry typeRegistry) {
        Object ret = null;

        if (entity != null) {
            AtlasEntityType             entityType     = typeRegistry == null ? null : typeRegistry.getEntityTypeByName(entity.getTypeName());
            Map<String, AtlasAttribute> uniqAttributes = entityType == null ? null : entityType.getUniqAttributes();

            if (MapUtils.isEmpty(uniqAttributes)) {
                ret = entity.getAttribute(DEFAULT_ENTITY_ID_ATTRIBUTE);
            } else {
                for (AtlasAttribute uniqAttribute : uniqAttributes.values()) {
                    ret = entity.getAttribute(uniqAttribute.getName());

                    if (ret != null) {
                        break;
                    }
                }

            }
        }

        return ret == null ? "" : ret.toString();
    }

    public Set<String> getClassificationNames(AtlasEntityHeader entity) {
        final Set<String> ret;

        if (entity == null || entity.getClassifications() == null) {
            ret = Collections.emptySet();
        } else {
            ret = new HashSet<>();

            for (AtlasClassification classify : entity.getClassifications()) {
                ret.add(classify.getTypeName());
            }
        }

        return ret;
    }

    @Override
    public String toString() {
        return "AtlasAccessRequest[" + "action=" + action + ", accessTime=" + accessTime +", user='" + user + '\'' +
                ", userGroups=" + userGroups + ", clientIPAddress='" + clientIPAddress + '\'' +
                ", forwardedAddresses=" + forwardedAddresses + ", remoteIPAddress='" + remoteIPAddress + '\'' +
                ']';

    }
}
