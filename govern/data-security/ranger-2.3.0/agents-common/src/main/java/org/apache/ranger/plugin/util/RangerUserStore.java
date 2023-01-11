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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.GroupInfo;
import org.apache.ranger.plugin.model.UserInfo;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerUserStore implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String CLOUD_IDENTITY_NAME = "cloud_id";

    private Long                             userStoreVersion;
    private Date                             userStoreUpdateTime;
    private Map<String, Map<String, String>> userAttrMapping;
    private Map<String, Map<String, String>> groupAttrMapping ;
    private Map<String, Set<String>>         userGroupMapping;
    private Map<String, String>              userCloudIdMapping;
    private Map<String, String>              groupCloudIdMapping;

    public RangerUserStore() {this(-1L, null, null, null);}

    public RangerUserStore(Long userStoreVersion, Set<UserInfo> users, Set<GroupInfo> groups, Map<String, Set<String>> userGroups) {
        setUserStoreVersion(userStoreVersion);
        setUserStoreUpdateTime(new Date());
        setUserGroupMapping(userGroups);
        buildMap(users, groups);
    }
    public Long getUserStoreVersion() {
        return userStoreVersion;
    }

    public void setUserStoreVersion(Long userStoreVersion) {
        this.userStoreVersion = userStoreVersion;
    }

    public Date getUserStoreUpdateTime() {
        return userStoreUpdateTime;
    }

    public void setUserStoreUpdateTime(Date userStoreUpdateTime) {
        this.userStoreUpdateTime = userStoreUpdateTime;
    }

    public Map<String, Map<String, String>> getUserAttrMapping() {
        return userAttrMapping;
    }

    public void setUserAttrMapping(Map<String, Map<String, String>> userAttrMapping) {
        this.userAttrMapping = userAttrMapping;
    }

    public Map<String, Map<String, String>> getGroupAttrMapping() {
        return groupAttrMapping;
    }

    public void setGroupAttrMapping(Map<String, Map<String, String>> groupAttrMapping) {
        this.groupAttrMapping = groupAttrMapping;
    }

    public Map<String, Set<String>> getUserGroupMapping() {
        return userGroupMapping;
    }

    public void setUserGroupMapping(Map<String, Set<String>> userGroupMapping) {
        this.userGroupMapping = userGroupMapping;
    }

    public Map<String, String> getUserCloudIdMapping() {
        return userCloudIdMapping;
    }

    public void setUserCloudIdMapping(Map<String, String> userCloudIdMapping) {
        this.userCloudIdMapping = userCloudIdMapping;
    }

    public Map<String, String> getGroupCloudIdMapping() {
        return groupCloudIdMapping;
    }

    public void setGroupCloudIdMapping(Map<String, String> groupCloudIdMapping) {
        this.groupCloudIdMapping = groupCloudIdMapping;
    }

    @Override
    public String toString( ) {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append("RangerUserStore={")
                .append("userStoreVersion=").append(userStoreVersion).append(", ")
                .append("userStoreUpdateTime=").append(userStoreUpdateTime).append(", ");
        sb.append("users={");
        if(MapUtils.isNotEmpty(userAttrMapping)) {
            for(String user : userAttrMapping.keySet()) {
                sb.append(user);
            }
        }
        sb.append("}, ");
        sb.append("groups={");
        if(MapUtils.isNotEmpty(groupAttrMapping)) {
            for(String group : groupAttrMapping.keySet()) {
                sb.append(group);
            }
        }
        sb.append("}");
        sb.append("}");

        return sb;
    }

    private void buildMap(Set<UserInfo> users, Set<GroupInfo> groups) {
        if (CollectionUtils.isNotEmpty(users)) {
            userAttrMapping = new HashMap<>();
            userCloudIdMapping = new HashMap<>();
            for (UserInfo user : users) {
                String username = user.getName();
                Map<String, String> userAttrs = user.getOtherAttributes();
                if (MapUtils.isNotEmpty(userAttrs)) {
                    userAttrMapping.put(username, userAttrs);
                    String cloudId = userAttrs.get(CLOUD_IDENTITY_NAME);
                    if (StringUtils.isNotEmpty(cloudId)) {
                        userCloudIdMapping.put(cloudId, username);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(groups)) {
            groupAttrMapping = new HashMap<>();
            groupCloudIdMapping = new HashMap<>();
            for (GroupInfo group : groups) {
                String groupname = group.getName();
                Map<String, String> groupAttrs = group.getOtherAttributes();
                if (MapUtils.isNotEmpty(groupAttrs)) {
                    groupAttrMapping.put(groupname, groupAttrs);
                    String cloudId = groupAttrs.get(CLOUD_IDENTITY_NAME);
                    if (StringUtils.isNotEmpty(cloudId)) {
                        groupCloudIdMapping.put(cloudId, groupname);
                    }
                }
            }
        }
    }
}
