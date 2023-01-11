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

package org.apache.ranger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class AccessAuditsService {
    protected List<SortField> sortFields = new ArrayList<SortField>();
    protected List<SearchField> searchFields;
    @Autowired
    protected
    RESTErrorUtil restErrorUtil;
    @Autowired
    protected
    RangerDaoManager daoManager;

    public AccessAuditsService() {
        searchFields = new ArrayList<SearchField>();
        searchFields.add(new SearchField("id", "id",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("accessType", "access",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("aclEnforcer", "enforcer",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("agentId", "agent",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("repoName", "repo",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("sessionId", "sess",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("requestUser", "reqUser",
                SearchField.DATA_TYPE.STR_LIST, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("excludeUser", "exlUser",
                SearchField.DATA_TYPE.STR_LIST, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("requestData", "reqData", SearchField.DATA_TYPE.STRING,
                SearchField.SEARCH_TYPE.PARTIAL));
        searchFields.add(new SearchField("resourcePath", "resource", SearchField.DATA_TYPE.STRING,
                SearchField.SEARCH_TYPE.PARTIAL));
        searchFields.add(new SearchField("clientIP", "cliIP",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

        searchFields.add(new SearchField("auditType", "logType",
                SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("accessResult", "result",
                SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        // searchFields.add(new SearchField("assetId", "obj.assetId",
        // SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("policyId", "policy",
                SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("repoType", "repoType",
                SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        /* Note; search fields starting with '-' denotes exclude conditions,
         * it should be handled manually if audit destination does not support the same.
         * solr support this way while cloudwatch does not.
         */
        searchFields.add(new SearchField("-repoType", "-repoType",
                SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("-requestUser", "-reqUser",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("resourceType", "resType",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("reason", "reason",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("action", "action",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

        searchFields.add(new SearchField("startDate", "evtTime",
                SearchField.DATA_TYPE.DATE, SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN));
        searchFields.add(new SearchField("endDate", "evtTime", SearchField.DATA_TYPE.DATE,
                SearchField.SEARCH_TYPE.LESS_EQUAL_THAN));

        searchFields.add(new SearchField("tags", "tags", SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
        searchFields.add(new SearchField("cluster", "cluster",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("zoneName", "zoneName",
                SearchField.DATA_TYPE.STR_LIST, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField("agentHost", "agentHost",
                SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));

        sortFields.add(new SortField("eventTime", "evtTime", true,
                SortField.SORT_ORDER.DESC));
        sortFields.add(new SortField("policyId", "policy", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("requestUser", "reqUser", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("resourceType", "resType", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("accessType", "access", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("action", "action", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("aclEnforcer", "enforcer", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("zoneName", "zoneName", false, SORT_ORDER.ASC));
        sortFields.add(new SortField("clientIP", "cliIP", false, SORT_ORDER.ASC));
    }

    protected void updateUserExclusion(Map<String, Object> paramList) {
        String val = (String) paramList.get("excludeServiceUser");

        if (val != null && Boolean.valueOf(val.trim())) {
            // add param to negate requestUsers which will be added as filter query
            List<String> excludeUsersList = getExcludeUsersList();
            if (CollectionUtils.isNotEmpty(excludeUsersList)) {
                Object oldUserExclusions = paramList.get("-requestUser");
                if (oldUserExclusions instanceof Collection && (!((Collection<?>)oldUserExclusions).isEmpty())) {
                    excludeUsersList.addAll((Collection<String>)oldUserExclusions);
                    paramList.put("-requestUser", excludeUsersList);
                } else {
                    paramList.put("-requestUser", excludeUsersList);
                }
            }
        }
    }

    private List<String> getExcludeUsersList() {
        //for excluding serviceUsers using existing property in ranger-admin-site
        List<String> excludeUsersList = new ArrayList<String>(getServiceUserList());

        //for excluding additional users using new property in ranger-admin-site
        String additionalExcludeUsers = PropertiesUtil.getProperty("ranger.accesslogs.exclude.users.list");
        List<String> additionalExcludeUsersList = null;
        if (StringUtils.isNotBlank(additionalExcludeUsers)) {
            additionalExcludeUsersList = new ArrayList<>(Arrays.asList(StringUtils.split(additionalExcludeUsers, ",")));
            for (String serviceUser : additionalExcludeUsersList) {
                if (StringUtils.isNotBlank(serviceUser) && !excludeUsersList.contains(serviceUser.trim())) {
                    excludeUsersList.add(serviceUser);
                }
            }
        }
        return excludeUsersList;
    }

    private List<String> getServiceUserList() {
        String components = EmbeddedServiceDefsUtil.DEFAULT_BOOTSTRAP_SERVICEDEF_LIST;
        List<String> serviceUsersList = new ArrayList<String>();
        List<String> componentNames =  Arrays.asList(StringUtils.split(components,","));
        for(String componentName : componentNames) {
            String serviceUser = PropertiesUtil.getProperty("ranger.plugins."+componentName+".serviceuser");
            if(StringUtils.isNotBlank(serviceUser)) {
                serviceUsersList.add(serviceUser);
            }
        }
        return serviceUsersList;
    }
}
