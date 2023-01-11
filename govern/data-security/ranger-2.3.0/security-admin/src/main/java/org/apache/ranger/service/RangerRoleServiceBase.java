/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.service;

import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.entity.XXRole;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.util.SearchFilter;

import java.util.HashMap;
import java.util.Map;

public abstract class RangerRoleServiceBase<T extends XXRole, V extends RangerRole> extends RangerBaseModelService<T, V> {

    public RangerRoleServiceBase() {
        super();

        searchFields.add(new SearchField(SearchFilter.ROLE_ID, "obj.id", SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField(SearchFilter.ROLE_NAME, "obj.name", SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.GROUP_NAME, "xXRoleRefGroup.groupName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL, "XXRoleRefGroup xXRoleRefGroup",
				"xXRoleRefGroup.roleId = obj.id"));
		searchFields.add(new SearchField(SearchFilter.USER_NAME, "xXRoleRefUser.userName", SearchField.DATA_TYPE.STRING,
				SearchField.SEARCH_TYPE.FULL, "XXRoleRefUser xXRoleRefUser", "xXRoleRefUser.roleId = obj.id"));
		searchFields.add(new SearchField(SearchFilter.ROLE_NAME_PARTIAL, "obj.name", SearchField.DATA_TYPE.STRING,
				SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField(SearchFilter.GROUP_NAME_PARTIAL, "xXRoleRefGroup.groupName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL, "XXRoleRefGroup xXRoleRefGroup",
				"xXRoleRefGroup.roleId = obj.id"));
		searchFields.add(new SearchField(SearchFilter.USER_NAME_PARTIAL, "xXRoleRefUser.userName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL, "XXRoleRefUser xXRoleRefUser",
				"xXRoleRefUser.roleId = obj.id"));

        sortFields.add(new SortField(SearchFilter.CREATE_TIME, "obj.createTime"));
        sortFields.add(new SortField(SearchFilter.UPDATE_TIME, "obj.updateTime"));
        sortFields.add(new SortField(SearchFilter.ROLE_ID, "obj.id", true, SortField.SORT_ORDER.ASC));
        sortFields.add(new SortField(SearchFilter.ROLE_NAME, "obj.name"));

    }

    @Override
    protected T mapViewToEntityBean(V vObj, T xObj, int OPERATION_CONTEXT) {
        xObj.setName(vObj.getName());
        xObj.setDescription(vObj.getDescription());

        Map<String, Object> options = vObj.getOptions();
        if (options == null) {
            options = new HashMap<>();
        }
        xObj.setOptions(JsonUtils.mapToJson(options));

        return xObj;
    }

    @Override
    protected V mapEntityToViewBean(V vObj, T xObj) {
        vObj.setName(xObj.getName());
        vObj.setDescription(xObj.getDescription());

        return vObj;
    }
}

