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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.util.SearchFilter;

public abstract class RangerSecurityZoneServiceBase<T extends XXSecurityZone, V extends RangerSecurityZone> extends RangerBaseModelService<T, V> {

	public RangerSecurityZoneServiceBase() {
		super();
        searchFields.add(new SearchField(SearchFilter.ZONE_ID, "obj.id", SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
        searchFields.add(new SearchField(SearchFilter.ZONE_NAME, "obj.name", SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

        sortFields.add(new SortField(SearchFilter.CREATE_TIME, "obj.createTime"));
        sortFields.add(new SortField(SearchFilter.UPDATE_TIME, "obj.updateTime"));
        sortFields.add(new SortField(SearchFilter.ZONE_ID, "obj.id", true, SortField.SORT_ORDER.ASC));
        sortFields.add(new SortField(SearchFilter.ZONE_NAME, "obj.name"));
	}

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int OPERATION_CONTEXT) {
		xObj.setName(vObj.getName());
		xObj.setDescription(vObj.getDescription());
		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		if (xObj.getId().equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
			vObj.setName(StringUtils.EMPTY);
		} else {
			vObj.setName(xObj.getName());
		}
		vObj.setDescription(xObj.getDescription());
		return vObj;
	}
}
