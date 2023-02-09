/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

/**
 * Grantee group information in the ACL, {@link AccessControlList}
 */
public class GroupGrantee implements GranteeInterface {

    /**
     * Anonymous user group, indicating all users
     */
    public static final GroupGrantee ALL_USERS = new GroupGrantee(GroupGranteeEnum.ALL_USERS);

    /**
     * OBS authorized user group, indicating all users who own OBS accounts
     */
    @Deprecated
    public static final GroupGrantee AUTHENTICATED_USERS = new GroupGrantee(GroupGranteeEnum.AUTHENTICATED_USERS);

    /**
     * Log delivery group, indicating common users who can configure access logs
     */
    public static final GroupGrantee LOG_DELIVERY = new GroupGrantee(GroupGranteeEnum.LOG_DELIVERY);

    private GroupGranteeEnum groupGranteeType;

    public GroupGrantee() {
    }

    /**
     * Constructor
     * 
     * @param uri
     *            URI for the grantee group
     */
    public GroupGrantee(String uri) {
        this.groupGranteeType = GroupGranteeEnum.getValueFromCode(uri);
    }

    public GroupGrantee(GroupGranteeEnum groupGranteeType) {
        this.groupGranteeType = groupGranteeType;
    }

    /**
     * Set the URI for the grantee group.
     * 
     * @param uri
     *            URI for the grantee group
     */
    @Override
    public void setIdentifier(String uri) {
        this.groupGranteeType = GroupGranteeEnum.getValueFromCode(uri);
    }

    /**
     * Obtain the URI of the grantee group.
     * 
     * @return URI of the grantee group.
     */
    @Override
    public String getIdentifier() {
        return this.groupGranteeType == null ? null : this.groupGranteeType.getCode();
    }

    /**
     * Obtain type of the grantee group.
     * 
     * @return Type of the grantee group
     */
    public GroupGranteeEnum getGroupGranteeType() {
        return this.groupGranteeType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupGranteeType == null) ? 0 : groupGranteeType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GroupGrantee other = (GroupGrantee) obj;
        return groupGranteeType == other.groupGranteeType;
    }

    /**
     * Return the object description.
     * 
     * @return Object description
     */
    @Override
    public String toString() {
        return "GroupGrantee [" + groupGranteeType + "]";
    }
}
