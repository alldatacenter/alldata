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

package org.apache.ranger.plugin.policyengine;

import java.util.HashSet;
import java.util.Set;

public class RangerResourceAccessInfo {
    final private RangerAccessRequest request;
    final private Set<String>         allowedUsers;
    final private Set<String>         allowedGroups;
    final private Set<String>         deniedUsers;
    final private Set<String>         deniedGroups;


    public RangerResourceAccessInfo(RangerAccessRequest request) {
        this.request       = request;
        this.allowedUsers  = new HashSet<>();
        this.allowedGroups = new HashSet<>();
        this.deniedUsers   = new HashSet<>();
        this.deniedGroups  = new HashSet<>();
    }

    public RangerResourceAccessInfo(RangerResourceAccessInfo other) {
        this.request       = other.request;
        this.allowedUsers  = other.allowedUsers == null ? new HashSet<String>() : new HashSet<String>(other.allowedUsers);
        this.allowedGroups = other.allowedGroups == null ? new HashSet<String>() : new HashSet<String>(other.allowedGroups);
        this.deniedUsers   = other.deniedUsers == null ? new HashSet<String>() : new HashSet<String>(other.deniedUsers);
        this.deniedGroups  = other.deniedGroups == null ? new HashSet<String>() : new HashSet<String>(other.deniedGroups);
    }

    public RangerAccessRequest getRequest() {
        return request;
    }

    public Set<String> getAllowedUsers() {
        return allowedUsers;
    }

    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }

    public Set<String> getDeniedUsers() {
        return deniedUsers;
    }

    public Set<String> getDeniedGroups() {
        return deniedGroups;
    }

    @Override
    public String toString( ) {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append("RangerResourceAccessInfo={");

        sb.append("request={");
        if(request != null) {
            sb.append(request);
        }
        sb.append("} ");

        sb.append("allowedUsers={");
        for(String user : allowedUsers) {
            sb.append(user).append(" ");
        }
        sb.append("} ");

        sb.append("allowedGroups={");
        for(String group : allowedGroups) {
            sb.append(group).append(" ");
        }
        sb.append("} ");

        sb.append("deniedUsers={");
        for(String user : deniedUsers) {
            sb.append(user).append(" ");
        }
        sb.append("} ");

        sb.append("deniedGroups={");
        for(String group : deniedGroups) {
            sb.append(group).append(" ");
        }
        sb.append("} ");

        sb.append("}");

        return sb;
    }

}
