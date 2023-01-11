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

package org.apache.ranger.ugsyncutil.model;

import java.util.Set;

public class GroupUserInfo {
    String groupName;
    Set<String> addUsers;
    Set<String> delUsers;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Set<String> getAddUsers() {
        return addUsers;
    }

    public void setAddUsers(Set<String> addUsers) {
        this.addUsers = addUsers;
    }

    public Set<String> getDelUsers() {
        return delUsers;
    }

    public void setDelUsers(Set<String> delUsers) {
        this.delUsers = delUsers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append("GroupUserInfo [groupName= ").append(groupName);
        sb.append(", addUsers = ").append(addUsers);
        sb.append(", delUsers = ").append(delUsers);
        sb.append("]");
        return sb;
    }
}
